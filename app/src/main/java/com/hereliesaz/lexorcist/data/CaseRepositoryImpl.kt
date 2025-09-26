package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.data.CaseSheetParser
import com.hereliesaz.lexorcist.utils.ErrorReporter
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.service.GoogleApiService // Restored
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import com.hereliesaz.lexorcist.utils.Result // Ensure this is the correct Result type

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val storageService: StorageService,
    private val errorReporter: ErrorReporter,
    private val googleApiService: GoogleApiService, // Restored
    private val caseSheetParser: CaseSheetParser, 
    private val evidenceRepository: EvidenceRepository 
) : CaseRepository {

    private val tag = "CaseRepositoryImpl"
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    override val cases: Flow<List<Case>> = _cases.asStateFlow()

    private val _selectedCase = MutableStateFlow<Case?>(null)
    override val selectedCase: Flow<Case?> = _selectedCase.asStateFlow()

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    override val selectedCaseAllegations: Flow<List<Allegation>> = _allegations.asStateFlow()

    private val _selectedCaseEvidence = MutableStateFlow<Result<List<Evidence>>>(Result.Success(emptyList()))
    override val selectedCaseEvidence: Flow<Result<List<Evidence>>> = _selectedCaseEvidence.asStateFlow()

    private var loadAllegationsJob: Job? = null
    private var loadEvidenceJob: Job? = null

    override suspend fun refreshCases() {
        when (val result = storageService.getAllCases()) {
            is Result.Loading -> {
                Log.d(tag, "Refreshing cases: Loading...")
            }
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
            _selectedCaseEvidence.value = Result.Loading // Indicate evidence is loading

            loadAllegationsJob = repositoryScope.launch {
                internalRefreshAllegations(case.spreadsheetId)
            }
            loadEvidenceJob = repositoryScope.launch {
                val result = storageService.getEvidenceForCase(case.spreadsheetId)
                if (isActive) {
                    _selectedCaseEvidence.value = result
                }
            }
        }
    }

    private suspend fun internalRefreshAllegations(spreadsheetId: String) {
        when (val result = storageService.getAllegationsForCase(spreadsheetId)) {
            is Result.Loading -> {
                Log.d(tag, "Refreshing allegations for $spreadsheetId: Loading...")
            }
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

            _allegations.value = emptyList()
            _selectedCaseEvidence.value = Result.Loading

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
            name = caseName, spreadsheetId = "", // Placeholder
            plaintiffs = plaintiffs,
            defendants = defendants, court = court,
            id = 0, 
            folderId = "", 
            isArchived = false,
            lastModifiedTime = System.currentTimeMillis()
        )
        return when (val creationResult = storageService.createCase(caseDetails)) {
            is Result.Loading -> Result.Loading
            is Result.Success -> {
                val createdCase = creationResult.data
                _cases.update {
                    (it + createdCase).sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                Result.Success(createdCase)
            }
            is Result.Error -> Result.Error(creationResult.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(creationResult.exception)
        }
    }

    override suspend fun archiveCase(case: Case): Result<Case> { // Changed caseToArchive to case
        val archivedDetails = case.copy(isArchived = true, lastModifiedTime = System.currentTimeMillis())
        return when (val updateStorageResult = storageService.updateCase(archivedDetails)) {
            is Result.Loading -> Result.Loading
            is Result.Success -> {
                 // Assuming updateStorageResult is Result<Unit> or Result<Case>
                 // If Result<Unit>, it implies success, and archivedDetails is the correct state.
                 // If Result<Case>, it would be updateStorageResult.data, but we use archivedDetails to be safe.
                _cases.update { currentCaseList ->
                    currentCaseList.map { caseInList ->
                        if (caseInList.spreadsheetId == archivedDetails.spreadsheetId) {
                            archivedDetails
                        } else {
                            caseInList
                        }
                    }.sortedByDescending { it.lastModifiedTime ?: 0L }
                }
                if (_selectedCase.value?.spreadsheetId == archivedDetails.spreadsheetId) {
                    selectCase(archivedDetails) 
                }
                Result.Success(archivedDetails)
            }
            is Result.Error -> Result.Error(updateStorageResult.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(updateStorageResult.exception)
        }
    }

    override suspend fun deleteCase(case: Case): Result<Unit> { // Changed caseToDelete to case
        return when (val deleteResult = storageService.deleteCase(case)) {
            is Result.Loading -> Result.Loading
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
            is Result.UserRecoverableError -> Result.UserRecoverableError(deleteResult.exception)
        }
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        val sheetData = googleApiService.readSpreadsheet(spreadsheetId, isPublic = false)
        if (sheetData.isNullOrEmpty()) {
            Log.w(tag, "Import spreadsheet $spreadsheetId: No sheet data found.")
            return null
        }

        val parsedData = caseSheetParser.parseCaseFromData(spreadsheetId, sheetData) ?: return null
        val (newCase, evidenceList) = parsedData

        when (val createResult = storageService.createCase(newCase)) {
            is Result.Loading -> {
                Log.d(tag, "Import spreadsheet $spreadsheetId: Case creation is loading.")
                return null
            }
            is Result.Success -> {
                val createdCase = createResult.data
                for (evidence in evidenceList) {
                    evidenceRepository.addEvidence(evidence.copy(spreadsheetId = createdCase.spreadsheetId))
                }
                _cases.update {
                    (it + createdCase).sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                return createdCase
            }
            is Result.Error -> {
                errorReporter.reportError(createResult.exception)
                Log.e(tag, "Import spreadsheet $spreadsheetId: Error creating case.", createResult.exception)
                return null
            }
            is Result.UserRecoverableError -> {
                errorReporter.reportError(createResult.exception)
                Log.w(tag, "Import spreadsheet $spreadsheetId: User recoverable error creating case.", createResult.exception)
                return null
            }
        }
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
    override suspend fun refreshSheetFilters(spreadsheetId: String) { /* TODO */ }
    override suspend fun addSheetFilter(spreadsheetId: String, name: String, value: String) { /* TODO */ }

    override fun getHtmlTemplates(): Flow<List<DriveFile>> = emptyFlow()
    override suspend fun refreshHtmlTemplates() { /* TODO */ }

    override suspend fun addAllegation(spreadsheetId: String, allegationText: String) {
        val currentSelectedCaseId = _selectedCase.value?.spreadsheetId
        if (spreadsheetId == currentSelectedCaseId) {
            // Assuming Allegation constructor takes (id, spreadsheetId, text)
            // and id is auto-generated or handled by storageService.
            // Passing 0 or a placeholder that storageService can interpret.
            val allegationDetails = Allegation(id = 0, spreadsheetId = spreadsheetId, text = allegationText, allegationElementName = "")
            when (val result = storageService.addAllegation(spreadsheetId, allegationDetails)) {
                is Result.Loading -> {
                    Log.d(tag, "Adding allegation to $spreadsheetId: Loading...")
                }
                is Result.Success -> {
                    // Assuming result.data is the added Allegation or a list containing it
                    // If addAllegation in storageService returns the created allegation:
                     val newAllegation = result.data
                    _allegations.update { currentAllegations ->
                        (currentAllegations + newAllegation).distinctBy { it.id }
                    }
                    // If you need to refresh all allegations for the case:
                    // internalRefreshAllegations(spreadsheetId)
                }
                is Result.Error -> {
                    errorReporter.reportError(result.exception)
                }
                is Result.UserRecoverableError -> {
                    errorReporter.reportError(result.exception)
                }
            }
        } else {
            val exception = Exception("Attempted to add allegation to a non-selected case. Target: $spreadsheetId, Selected: $currentSelectedCaseId")
            errorReporter.reportError(exception)
            Log.w(tag, exception.message, exception)
        }
    }
}
