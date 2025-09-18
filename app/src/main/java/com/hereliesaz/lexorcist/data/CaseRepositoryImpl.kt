import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.Schema
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

\
)

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    override val selectedCaseAllegations: Flow<List<Allegation>> = _allegations.asStateFlow()

    private val _selectedCaseEvidence = MutableStateFlow<Result<List<Evidence>>>(Result.Success(emptyList()))
    override val selectedCaseEvidence: Flow<Result<List<Schema.Evidence>>> = _selectedCaseEvidence.asStateFlow()

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
                // If storageService.updateCase was successful (even if it returns Result<Unit>),
                // we assume 'archivedDetails' is the state we want to reflect.
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
                    selectCase(archivedDetails) // Refresh selected case details with the locally known 'archivedDetails'
                }
                Result.Success(archivedDetails) // Return 'archivedDetails' as the outcome of the operation
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
            val allegationDetails = Allegation(spreadsheetId = spreadsheetId, text = allegationText, id = 0) // Ensure ID is handled
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
            val exception = Exception("C
```