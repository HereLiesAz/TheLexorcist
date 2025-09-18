package com.hereliesaz.lexorcist.data

import android.util.Log // Added for logging
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
    private val googleApiService: GoogleApiService,
    private val evidenceRepository: EvidenceRepository
) : CaseRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Define before use
    private val tag = "CaseRepositoryImpl" // Added for logging

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
                // Optionally, emit a loading state for the UI if it handles it for the cases list
            }
            is Result.Success -> {
                _cases.value = result.data.sortedByDescending { it.lastModifiedTime ?: 0L }
            }
            is Result.Error -> errorReporter.reportError(result.exception)
            is Result.UserRecoverableError -> errorReporter.reportError(result.exception) // Consider specific user UI for this
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
            _selectedCaseEvidence.value = Result.Loading // Indicate evidence is loading

            loadAllegationsJob = repositoryScope.launch {
                internalRefreshAllegations(case.spreadsheetId)
            }
            loadEvidenceJob = repositoryScope.launch {
                // Assuming storageService.getEvidenceForCase also returns Result
                // and might have a Loading state. The UI would observe _selectedCaseEvidence.
                val result = storageService.getEvidenceForCase(case.spreadsheetId)
                if (isActive) { // Check if the job is still active before emitting
                    _selectedCaseEvidence.value = result
                }
            }
        }
    }

    private suspend fun internalRefreshAllegations(spreadsheetId: String) {
        when (val result = storageService.getAllegationsForCase(spreadsheetId)) {
            is Result.Loading -> {
                Log.d(tag, "Refreshing allegations for $spreadsheetId: Loading...")
                // _allegations.value is likely already emptyList() or a placeholder
            }
            is Result.Success -> if (repositoryScope.isActive) _allegations.value = result.data
            is Result.Error -> {
                if (repositoryScope.isActive) _allegations.value = emptyList()
                errorReporter.reportError(result.exception)
            }
            is Result.UserRecoverableError -> {
                if (repositoryScope.isActive) _allegations.value = emptyList()
                errorReporter.reportError(result.exception) // Consider specific user UI
            }
        }
    }

    override suspend fun refreshSelectedCaseDetails() {
        val currentSelectedCase = _selectedCase.value
        currentSelectedCase?.spreadsheetId?.let { spreadsheetId ->
            loadAllegationsJob?.cancel()
            loadEvidenceJob?.cancel()

            _allegations.value = emptyList() // Reset or show loading state
            _selectedCaseEvidence.value = Result.Loading // Indicate evidence is loading

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
            name = caseName, spreadsheetId = "", // Placeholder, will be set by storageService
            plaintiffs = plaintiffs,
            defendants = defendants, court = court,
            // Initialize other Case properties as needed, ensure they have defaults or are set
            id = 0, // Assuming ID is generated later or 0 is a valid temp ID
            folderId = "", // Placeholder
            isArchived = false,
            lastModifiedTime = System.currentTimeMillis() // Set initial time
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

    override suspend fun archiveCase(caseToArchive: Case): Result<Case> { // Renamed parameter
        val archivedDetails = caseToArchive.copy(isArchived = true, lastModifiedTime = System.currentTimeMillis())

        return when (val updateStorageResult = storageService.updateCase(archivedDetails)) {
            is Result.Loading -> Result.Loading
            is Result.Success -> {
                val updatedCase = updateStorageResult.data // Assuming updateCase returns the updated Case
                _cases.update { currentCaseList ->
                    currentCaseList.map { caseInList ->
                        if (caseInList.spreadsheetId == updatedCase.spreadsheetId) {
                            updatedCase
                        } else {
                            caseInList
                        }
                    }.sortedByDescending { it.lastModifiedTime ?: 0L }
                }

                if (_selectedCase.value?.spreadsheetId == updatedCase.spreadsheetId) {
                    selectCase(updatedCase) // Refresh selected case details
                }
                Result.Success(updatedCase)
            }
            is Result.Error -> Result.Error(updateStorageResult.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(updateStorageResult.exception)
        }
    }

    override suspend fun deleteCase(caseToDelete: Case): Result<Unit> { // Renamed parameter
        return when (val deleteResult = storageService.deleteCase(caseToDelete)) {
            is Result.Loading -> Result.Loading
            is Result.Success -> {
                _cases.update {
                    it.filterNot { c -> c.spreadsheetId == caseToDelete.spreadsheetId }
                        .sortedByDescending { c -> c.lastModifiedTime ?: 0L }
                }
                if (_selectedCase.value?.spreadsheetId == caseToDelete.spreadsheetId) {
                    selectCase(null)
                }
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(deleteResult.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(deleteResult.exception)
        }
    }

    override suspend fun importSpreadsheet(spreadsheetId: String): Case? {
        val sheetData = googleApiService.readSpreadsheet(spreadsheetId)
        if (sheetData.isNullOrEmpty()) {
            Log.w(tag, "Import spreadsheet $spreadsheetId: No sheet data found.")
            return null
        }
        val parsedData = caseSheetParser.parseCaseFromData(spreadsheetId, sheetData)
        if (parsedData != null) {
            val (newCase, evidenceList) = parsedData
            when (val createResult = storageService.createCase(newCase)) {
                is Result.Loading -> {
                    Log.d(tag, "Import spreadsheet $spreadsheetId: Case creation is loading.")
                    // Decide if we should return null or wait/retry. For now, return null.
                    return null
                }
                is Result.Success -> {
                    val createdCase = createResult.data
                    evidenceList.forEach { evidence ->
                        // Assuming addEvidence can handle potential failures gracefully or returns a Result
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
                }
                is Result.UserRecoverableError -> {
                    errorReporter.reportError(createResult.exception) // Consider specific UI
                    Log.w(tag, "Import spreadsheet $spreadsheetId: User recoverable error creating case.", createResult.exception)
                }
            }
        } else {
            Log.w(tag, "Import spreadsheet $spreadsheetId: Failed to parse case data.")
        }
        return null
    }

    override suspend fun synchronize() {
        storageService.synchronize() // Assuming this is a suspend function
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
            val allegationDetails = Allegation(spreadsheetId = spreadsheetId, text = allegationText, id = 0, timestamp = System.currentTimeMillis()) // Ensure ID and timestamp are handled
            when (val result = storageService.addAllegation(spreadsheetId, allegationDetails)) {
                is Result.Loading -> {
                    Log.d(tag, "Adding allegation to $spreadsheetId: Loading...")
                }
                is Result.Success -> {
                    _allegations.update { (it + result.data).distinctBy { al -> al.id } } // Ensure uniqueness if ID is key
                }
                is Result.Error -> {
                    errorReporter.reportError(result.exception)
                }
                is Result.UserRecoverableError -> {
                    errorReporter.reportError(result.exception) // Consider specific UI
                }
            }
        } else {
            val exception = Exception("Cannot add an allegation to a non-selected case or case ID mismatch.")
            errorReporter.reportError(exception)
            Log.e(tag, "Failed to add allegation: Attempted to add to $spreadsheetId but $currentSelectedCaseId is selected.", exception)
        }
    }
}
