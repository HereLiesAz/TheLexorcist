package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.data.Case 
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.lexorcist.utils.ErrorReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val storageService: StorageService,
    private val settingsManager: SettingsManager, // Kept for now, ensure it's used or remove
    private val errorReporter: ErrorReporter,
    private val caseSheetParser: CaseSheetParser,
    private val googleApiService: GoogleApiService
) : CaseRepository {

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    override val cases: Flow<List<Case>> = _cases.asStateFlow()

    private val _selectedCase = MutableStateFlow<Case?>(null)
    override val selectedCase: Flow<Case?> = _selectedCase.asStateFlow()

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    override val selectedCaseAllegations: Flow<List<Allegation>> = _allegations.asStateFlow()

    private val _selectedCaseEvidence = MutableStateFlow<Result<List<Evidence>>>(Result.Success(emptyList()))
    override val selectedCaseEvidence: Flow<Result<List<Evidence>>> = _selectedCaseEvidence.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var loadAllegationsJob: Job? = null
    private var loadEvidenceJob: Job? = null

    override suspend fun refreshCases() {
        when (val result = storageService.getAllCases()) {
            is Result.Success -> {
                _cases.value = result.data.sortedByDescending { it.lastModifiedTime ?: 0L }
            }
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception)
        }
    }

    override suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case? {
        return _cases.value.find { it.spreadsheetId == spreadsheetId }
    }

    override suspend fun selectCase(case: Case?) {
        if (_selectedCase.value?.spreadsheetId == case?.spreadsheetId && case != null) {
            // Case already selected, optionally trigger a refresh of its details if desired
            return
        }

        loadAllegationsJob?.cancel()
        loadEvidenceJob?.cancel()

        _selectedCase.value = case

        if (case == null) {
            _allegations.value = emptyList()
            _selectedCaseEvidence.value = Result.Success(emptyList())
        } else {
            _allegations.value = emptyList() // Reset or show loading state
            // TODO: Define a proper Result.Loading state in your com.hereliesaz.lexorcist.utils.Result class
            _selectedCaseEvidence.value = Result.Success(emptyList()) 

            loadAllegationsJob = repositoryScope.launch {
                internalRefreshAllegations(case.spreadsheetId)
            }
            loadEvidenceJob = repositoryScope.launch {
                val result = storageService.getEvidenceForCase(case.spreadsheetId)
                if (isActive) { // Check if the job is still active before emitting
                    _selectedCaseEvidence.value = result
                }
            }
        }
    }

    private suspend fun internalRefreshAllegations(spreadsheetId: String) {
        when (val result = storageService.getAllegationsForCase(spreadsheetId)) {
            is Result.Success -> if (repositoryScope.isActive) _allegations.value = result.data
            is Result.Error -> {
                if (repositoryScope.isActive) _allegations.value = emptyList()
                errorReporter.reportError(result.exception)
            }
            is Result.UserRecoverableError -> {
                if (repositoryScope.isActive) _allegations.value = emptyList()
                errorReporter.reportError(result.exception)
            }
        }
    }

    override suspend fun refreshSelectedCaseDetails() {
        val currentSelectedCase = _selectedCase.value
        currentSelectedCase?.spreadsheetId?.let { spreadsheetId ->
            loadAllegationsJob?.cancel()
            loadEvidenceJob?.cancel()

            _allegations.value = emptyList() // Reset or show loading state
            // TODO: Define a proper Result.Loading state in your com.hereliesaz.lexorcist.utils.Result class
            _selectedCaseEvidence.value = Result.Success(emptyList())

            loadAllegationsJob = repositoryScope.launch {
                internalRefreshAllegations(spreadsheetId)
            }
            loadEvidenceJob = repositoryScope.launch {
                val result = storageService.getEvidenceForCase(spreadsheetId)
                if (isActive) {
                    _selectedCaseEvidence.value = result
                }
            }
        }
    }

    override suspend fun createCase(
        caseName: String, exhibitSheetName: String, caseNumber: String, caseSection: String,
        caseJudge: String, plaintiffs: String, defendants: String, court: String
    ): Result<Case> {
        val caseDetails = Case(
            name = caseName, spreadsheetId = "", plaintiffs = plaintiffs,
            defendants = defendants, court = court
            // Ensure all necessary fields of Case are initialized
        )
        // Assuming storageService.createCase returns Result<Case> with the fully created case
        return when (val creationResult = storageService.createCase(caseDetails)) {
            is Result.Success -> {
                val createdCase = creationResult.data // This is type Case
                _cases.update {
                    (it + createdCase).sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                Result.Success(createdCase)
            }
            is Result.Error -> Result.Error(creationResult.exception)
            is Result.UserRecoverableError -> Result.Error(creationResult.exception) 
        }
    }

    override suspend fun archiveCase(case: Case): Result<Case> { // Renamed parameter
        val archivedDetails: Case = case.copy(isArchived = true)
        
        return when (val updateStorageResult = storageService.updateCase(archivedDetails)) {
            is Result.Success -> {
                _cases.update { currentCaseList: List<Case> ->
                    val updatedList = currentCaseList.map { caseInList: Case ->
                        if (caseInList.spreadsheetId == archivedDetails.spreadsheetId) {
                            archivedDetails 
                        } else {
                            caseInList 
                        }
                    } 
                    updatedList.sortedByDescending { it.lastModifiedTime ?: 0L } 
                }
                
                if (_selectedCase.value?.spreadsheetId == archivedDetails.spreadsheetId) {
                    selectCase(archivedDetails) 
                }
                Result.Success(archivedDetails) 
            }
            is Result.Error -> Result.Error(updateStorageResult.exception)
            is Result.UserRecoverableError -> Result.Error(updateStorageResult.exception)
        }
    }

    override suspend fun deleteCase(case: Case): Result<Unit> { // Renamed parameter
        return when (val deleteResult = storageService.deleteCase(case)) {
            is Result.Success -> {
                _cases.update {
                    it.filterNot { c -> c.spreadsheetId == case.spreadsheetId }
                        .sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                if (_selectedCase.value?.spreadsheetId == case.spreadsheetId) {
                    selectCase(null) 
                }
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(deleteResult.exception)
            is Result.UserRecoverableError -> Result.Error(deleteResult.exception)
        }
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        val sheetData = googleApiService.readSpreadsheet(spreadsheetId)
        if (sheetData.isNullOrEmpty()) return null
        val parsedData = caseSheetParser.parseCaseFromData(spreadsheetId, sheetData)
        if (parsedData != null) {
            val (newCase, evidenceList) = parsedData
            val createResult = storageService.createCase(newCase) 
            if (createResult is Result.Success) {
                val createdCase = createResult.data
                // TODO: Handle evidenceList - this might involve another storageService call and cache update
                _cases.update {
                    (it + createdCase).sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                return createdCase
            } else if (createResult is Result.Error) {
                errorReporter.reportError(createResult.exception)
            } else if (createResult is Result.UserRecoverableError) {
                errorReporter.reportError(createResult.exception)
            }
        }
        return null
    }

    override suspend fun synchronize() {
        storageService.synchronize()
        refreshCases() 
        if (_selectedCase.value != null) {
            refreshSelectedCaseDetails()
        }
    }

    override suspend fun clearCache() {
        _cases.value = emptyList()
        selectCase(null) 
    }

    override fun getSheetFilters(spreadsheetId: String): Flow<List<SheetFilter>> = emptyFlow() 
    override suspend fun refreshSheetFilters(spreadsheetId: String) {} 
    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) {} 

    override fun getHtmlTemplates(): Flow<List<DriveFile>> = emptyFlow() 
    override suspend fun refreshHtmlTemplates() {} 

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) {
        val currentSelectedCaseId = _selectedCase.value?.spreadsheetId
        if (spreadsheetId == currentSelectedCaseId) {
            val allegationDetails = Allegation(spreadsheetId = spreadsheetId, text = allegationText)
            // TODO: Call storageService.addAllegation. It should ideally return the created Allegation.
            // storageService.addAllegation(spreadsheetId, allegationDetails)
            // Then update _allegations cache directly if successful, similar to how cases are handled.
            // For now, falling back to refresh all allegations for the selected case:
            internalRefreshAllegations(spreadsheetId)
        } else {
            // TODO: Decide how to handle adding an allegation to a non-selected case.
        }
    }
}
