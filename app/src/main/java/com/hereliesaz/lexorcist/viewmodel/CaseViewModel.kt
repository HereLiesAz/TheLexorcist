package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log // Added import for Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.LocalFileStorageService
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.data.SortOrder
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map // Ensured import
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CaseViewModel
@Inject
constructor(
    @param:ApplicationContext private val applicationContext: Context,
    private val caseRepository: CaseRepository,
    private val evidenceRepository: EvidenceRepository,
    private val settingsManager: SettingsManager,
    private val localFileStorageService: LocalFileStorageService,
    private val scriptRunner: com.hereliesaz.lexorcist.service.ScriptRunner,
    private val ocrProcessingService: com.hereliesaz.lexorcist.service.OcrProcessingService,
    private val transcriptionService: com.hereliesaz.lexorcist.service.TranscriptionService,
    private val workManager: androidx.work.WorkManager,
    private val logService: com.hereliesaz.lexorcist.service.LogService,
    private val storageService: com.hereliesaz.lexorcist.data.StorageService,
    private val globalLoadingState: com.hereliesaz.lexorcist.service.GlobalLoadingState,
) : ViewModel() {
    private val sharedPref =
        applicationContext.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)


    private val _processingStatus = MutableStateFlow<String?>(null) // Consider removing if _processingState covers this
    val processingStatus: StateFlow<String?> = _processingStatus.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val cases: StateFlow<List<Case>> =
        caseRepository.cases
            .combine(sortOrder) { cases, currentSortOrder ->
                when (currentSortOrder) {
                    SortOrder.NAME_ASC -> cases.sortedBy { it.name }
                    SortOrder.NAME_DESC -> cases.sortedByDescending { it.name }
                    SortOrder.DATE_ASC -> cases.sortedBy { it.id }
                    SortOrder.DATE_DESC -> cases.sortedByDescending { it.id }
                }
            }
            .combine(searchQuery) { cases, query ->
                if (query.isBlank()) {
                    cases
                } else {
                    cases.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _vmSelectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _vmSelectedCase.asStateFlow()

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val sheetFilters: StateFlow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    val allegations: StateFlow<List<Allegation>> =
        caseRepository.selectedCaseAllegations
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    val htmlTemplates: StateFlow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    private val _plaintiffs = MutableStateFlow(sharedPref.getString("plaintiffs", "") ?: "")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()

    private val _defendants = MutableStateFlow(sharedPref.getString("defendants", "") ?: "")
    val defendants: StateFlow<String> = _defendants.asStateFlow()

    private val _court = MutableStateFlow(sharedPref.getString("court", "") ?: "")
    val court: StateFlow<String> = _court.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _userRecoverableAuthIntent = MutableStateFlow<Intent?>(null)
    val userRecoverableAuthIntent: StateFlow<Intent?> =
        _userRecoverableAuthIntent.asStateFlow()

    private val _videoProcessingProgress = MutableStateFlow<String?>(null)
    val videoProcessingProgress: StateFlow<String?> = _videoProcessingProgress.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState?>(null) // Used for general media processing UI
    val processingState: StateFlow<ProcessingState?> = _processingState.asStateFlow()

    private val _logMessages = MutableStateFlow<List<com.hereliesaz.lexorcist.model.LogEntry>>(emptyList())
    val logMessages: StateFlow<List<com.hereliesaz.lexorcist.model.LogEntry>> = _logMessages.asStateFlow()

    private val _navigateToTranscriptionScreen = MutableSharedFlow<Int>()
    val navigateToTranscriptionScreen = _navigateToTranscriptionScreen.asSharedFlow()

    private val _timelineSortType = MutableStateFlow(TimelineSortType.DATE_OF_OCCURRENCE)
    val timelineSortType: StateFlow<TimelineSortType> = _timelineSortType.asStateFlow()

    private val _selectedCaseEvidenceListInternal =
        MutableStateFlow<List<com.hereliesaz.lexorcist.data.Evidence>>(emptyList())

    val selectedCaseEvidenceList: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        _selectedCaseEvidenceListInternal
            .combine(timelineSortType) { evidence, sortType ->
                when (sortType) {
                    TimelineSortType.DATE_OF_OCCURRENCE -> evidence.sortedByDescending { it.documentDate }
                    TimelineSortType.DATE_EVIDENCE_ADDED -> evidence.sortedByDescending { it.timestamp }
                    TimelineSortType.BY_ALLEGATION -> evidence.sortedBy { it.allegationId }
                    TimelineSortType.BY_FILE_TYPE -> evidence.sortedBy { it.type }
                    TimelineSortType.CUSTOM -> evidence
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedEvidence: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        selectedCaseEvidenceList
            .map { list -> list.filter { it.isSelected } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onTimelineSortOrderChanged(sortType: TimelineSortType) {
        _timelineSortType.value = sortType
    }

    private val _themeMode =
        MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> =
        _themeMode.asStateFlow()

    private val _storageLocation = MutableStateFlow<String?>(null)
    val storageLocation: StateFlow<String?> = _storageLocation.asStateFlow()

    init {
        Log.d("CaseViewModel", "--- CaseViewModel INIT --- instancia: $this")
        loadThemeModePreference()
        _storageLocation.value = settingsManager.getStorageLocation()

        viewModelScope.launch {
            logService.logEventFlow.collect { newLog ->
                _logMessages.value = listOf(newLog) + _logMessages.value.take(199) // Keep last 200 logs
            }
        }

        viewModelScope.launch {
            Log.d("CaseViewModel", "INIT: Starting to collect caseRepository.selectedCase. Current repo instance: ${caseRepository}")
            caseRepository.selectedCase.collect { caseFromRepo ->
                Log.d("CaseViewModel", "INIT COLLECTOR: caseRepository.selectedCase emitted: ${caseFromRepo?.name ?: "null"}. Updating _vmSelectedCase.")
                _vmSelectedCase.value = caseFromRepo
            }
        }

        viewModelScope.launch {
            caseRepository.selectedCaseEvidence.collect { result ->
                // This collects general evidence list for the selected case, not specific to an ongoing processing operation.
                // _isLoading might be too broad here if only evidence list is updating.
                // Consider a specific loading state for the evidence list if needed.
                when (result) {
                    is Result.Success -> {
                        _selectedCaseEvidenceListInternal.value = result.data
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.exception.message ?: "Error loading evidence"
                        _selectedCaseEvidenceListInternal.value = emptyList()
                    }
                    is Result.UserRecoverableError -> {
                        _userRecoverableAuthIntent.value = result.exception.intent
                        _selectedCaseEvidenceListInternal.value = emptyList()
                    }
                    is Result.Loading -> { // Handle loading state from repository if provided
                        // You might want a specific isLoadingEvidenceList StateFlow
                    }
                }
            }
        }
    }

    fun setStorageLocation(uri: android.net.Uri) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val oldLocation = settingsManager.getStorageLocation()
                settingsManager.saveStorageLocation(uri.toString())
                _storageLocation.value = uri.toString()
                if (oldLocation != null) {
                    viewModelScope.launch { _userMessage.emit("Moving files to new location...") }
                    localFileStorageService.moveFilesToNewLocation(oldLocation, uri.toString())
                    viewModelScope.launch { _userMessage.emit("Files moved successfully.") }
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    private fun clearCaseData() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.selectCase(null) 
                _sheetFilters.value = emptyList()
                _htmlTemplates.value = emptyList()
                _plaintiffs.value = ""
                _defendants.value = ""
                _court.value = ""
                saveCaseInfoToSharedPrefs()
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearUserRecoverableAuthIntent() {
        _userRecoverableAuthIntent.value = null
    }

    fun setThemeMode(themeMode: ThemeMode) {
        _themeMode.value = themeMode
        sharedPref.edit().putString("theme_mode", themeMode.name).apply()
    }

    private fun loadThemeModePreference() {
        val themeName =
            sharedPref.getString(
                "theme_mode",
                ThemeMode.SYSTEM.name
            )
        _themeMode.value =
            ThemeMode.valueOf(
                themeName ?: ThemeMode.SYSTEM.name
            )
    }

    fun onSortOrderChange(newSortOrder: SortOrder) {
        _sortOrder.value = newSortOrder
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun loadCasesFromRepository() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.refreshCases()
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun loadHtmlTemplatesFromRepository() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.refreshHtmlTemplates()
                caseRepository.getHtmlTemplates().collect {
                    _htmlTemplates.value = it
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun importSpreadsheetWithRepository(spreadsheetId: String) {
        viewModelScope.launch { 
            globalLoadingState.pushLoading()
            try {
                caseRepository.importSpreadsheet(spreadsheetId) 
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String,
    ) {
        Log.d("CaseViewModel", "createCase called with name: $caseName")
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val result =
                    caseRepository.createCase(
                        caseName,
                        exhibitSheetName,
                        caseNumber,
                        caseSection,
                        caseJudge,
                        plaintiffs.value,
                        defendants.value,
                        court.value,
                    )
                when (result) {
                    is Result.Success -> {
                        Log.d("CaseViewModel", "Case creation successful")
                    }
                    is Result.Error -> {
                        _errorMessage.value =
                            result.exception.message ?: "Unknown error during case creation"
                    }
                    is Result.UserRecoverableError -> {
                        _userRecoverableAuthIntent.value = result.exception.intent
                    }
                    is Result.Loading -> { /* Handle Loading if necessary */ }
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun selectCase(case: Case?) {
        Log.d("CaseViewModel", "--- CaseViewModel.selectCase ENTERED with case: ${case?.name ?: "null"} --- instance: $this, repo instance: $caseRepository")
        viewModelScope.launch {
            Log.d("CaseViewModel", "viewModelScope.launch in selectCase for case: ${case?.name ?: "null"}")
            globalLoadingState.pushLoading()
            try {
                // Reset processing state when a new case is selected or deselected
                _processingState.value = ProcessingState.Idle 
                _logMessages.value = emptyList()

                caseRepository.selectCase(case)
                Log.d("CaseViewModel", "IMMEDIATELY AFTER caseRepository.selectCase, ViewModel's _vmSelectedCase.value is: ${_vmSelectedCase.value?.name ?: "null"}")

                if (case != null) {
                    Log.d("CaseViewModel", "Case is not null, proceeding to load filters/templates for ${case.name}")
                    loadSheetFiltersFromRepository(case.spreadsheetId)
                    loadHtmlTemplatesFromRepository()
                } else {
                    Log.d("CaseViewModel", "Case is null, clearing filters/templates.")
                    _sheetFilters.value = emptyList()
                    _htmlTemplates.value = emptyList()
                }
            } finally {
                globalLoadingState.popLoading()
                Log.d("CaseViewModel", "isLoading SET TO false in selectCase finally block for case: ${case?.name ?: "null"}")
            }
        }
    }

    private fun loadSheetFiltersFromRepository(spreadsheetId: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.refreshSheetFilters(spreadsheetId)
                caseRepository.getSheetFilters(spreadsheetId).collect {
                    _sheetFilters.value = it
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun addSheetFilterWithRepository(
        name: String,
        value: String,
    ) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: return@launch
                caseRepository.addSheetFilter(spreadsheetId, name, value)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun addAllegationWithRepository(allegationText: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val case = selectedCase.value ?: return@launch
                caseRepository.addAllegation(case.spreadsheetId, allegationText)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun toggleEvidenceSelection(evidenceId: Int) {
        val updatedList =
            _selectedCaseEvidenceListInternal.value.map {
                if (it.id == evidenceId) {
                    it.copy(isSelected = !it.isSelected)
                } else {
                    it
                }
            }
        _selectedCaseEvidenceListInternal.value = updatedList
    }

    fun clearEvidenceSelection() {
        val list =
            _selectedCaseEvidenceListInternal.value.map {
                it.copy(isSelected = false)
            }
        _selectedCaseEvidenceListInternal.value = list
    }

    fun assignAllegationToEvidence(
        evidenceId: Int,
        allegationId: Int,
    ) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                val evidence = _selectedCaseEvidenceListInternal.value.find { it.id == evidenceId }
                if (evidence != null) {
                    val updatedEvidence = evidence.copy(allegationId = allegationId)
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun onPlaintiffsChanged(name: String) {
        _plaintiffs.value = name
        saveCaseInfoToSharedPrefs()
    }

    fun onDefendantsChanged(name: String) {
        _defendants.value = name
        saveCaseInfoToSharedPrefs()
    }

    fun onCourtChanged(name: String) {
        _court.value = name
        saveCaseInfoToSharedPrefs()
    }

    private fun saveCaseInfoToSharedPrefs() {
        sharedPref
            .edit()
            .putString("plaintiffs", _plaintiffs.value)
            .putString("defendants", _defendants.value)
            .putString("court", _court.value)
            .apply()
    }

    fun archiveCaseWithRepository(case: Case) {
        viewModelScope.launch { 
            globalLoadingState.pushLoading()
            try {
                caseRepository.archiveCase(case) 
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun deleteCaseWithRepository(case: Case) {
        viewModelScope.launch { 
            globalLoadingState.pushLoading()
            try {
                caseRepository.deleteCase(case) 
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                caseRepository.clearCache()
                clearCaseData()
                sharedPref.edit().clear().apply()
                loadThemeModePreference()
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun updateEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                evidenceRepository.updateEvidence(evidence)
                val script = settingsManager.getScript()
                val result = scriptRunner.runScript(script, evidence)
                if (result is Result.Success) {
                    val currentTagsInEvidence: List<String> = evidence.tags
                    val newTagsFromScript: List<String> = result.data.tags // Corrected: result.data.tags
                    val combinedTags: List<String> = currentTagsInEvidence + newTagsFromScript
                    val updatedEvidence = evidence.copy(tags = combinedTags) // Corrected: combinedTags
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun deleteEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                evidenceRepository.deleteEvidence(evidence)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun assignAllegationToSelectedEvidence(allegationId: Int) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                selectedEvidence.value.forEach { evidence ->
                    val updatedEvidence = evidence.copy(allegationId = allegationId)
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
                clearEvidenceSelection()
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun clearLogs() {
        _logMessages.value = emptyList()
        _processingState.value = ProcessingState.Idle // Reset processing state when logs are cleared
    }

    fun addTextEvidence(text: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                 val currentCaseFromState = _vmSelectedCase.value
                Log.d("CaseViewModel", "addTextEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
                val caseToUse = currentCaseFromState ?: run {
                    Log.w("CaseViewModel", "addTextEvidence: No case selected from StateFlow, aborting.")
                    viewModelScope.launch { _userMessage.emit("Please select a case first to add text evidence.") }
                    globalLoadingState.popLoading()
                    return@launch
                }
                val entities = com.hereliesaz.lexorcist.DataParser.tagData(text)
                val newEvidence =
                    com.hereliesaz.lexorcist.data.Evidence(
                        caseId = caseToUse.id.toLong(),
                        spreadsheetId = caseToUse.spreadsheetId,
                        type = "text",
                        content = text,
                        formattedContent = null,
                        mediaUri = null,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Manual text entry",
                        documentDate = System.currentTimeMillis(),
                        allegationId = null,
                        category = "",
                        tags = emptyList(),
                        commentary = null,
                        entities = entities,
                    )
                evidenceRepository.addEvidence(newEvidence)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun updateCommentary(evidenceId: Int, commentary: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                evidenceRepository.updateCommentary(evidenceId, commentary)
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun processImageEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processImageEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            globalLoadingState.pushLoading()
            clearLogs() // Clears logs and sets processingState to Idle
            val caseToUse = currentCaseFromState ?: run {
                Log.w("CaseViewModel", "processImageEvidence: No case selected from StateFlow, aborting.")
                _userMessage.value = "Please select a case first to add image evidence."
                globalLoadingState.popLoading()
                _processingState.value = ProcessingState.Failure("No case selected")
                return@launch
            }
            try {
                Log.d("CaseViewModel", "Processing image for case: ${caseToUse.name}")
                // OcrProcessingService.processImage now has a callback for ProcessingState updates
                // So, _processingState will be updated directly from there.
                val (newEvidence, message) = ocrProcessingService.processImage(
                    uri = uri,
                    context = applicationContext,
                    caseId = caseToUse.id.toLong(),
                    spreadsheetId = caseToUse.spreadsheetId,
                ) { state -> _processingState.value = state } // Pass the lambda to update ViewModel's state
                
                message?.let { 
                    Log.i("CaseViewModel", "Message from ocrProcessingService: $it")
                    _userMessage.value = it
                }
                if (newEvidence != null && newEvidence.content.isEmpty()) {
                    Log.i("CaseViewModel", "No text found in image for URI: $uri")
                    _userMessage.value = "No text found in the image."
                }
                Log.d("CaseViewModel", "Image processing finished for URI: $uri")
            } catch (e: Exception) {
                val errorMsg = "Error processing image URI: $uri: ${e.message}"
                Log.e("CaseViewModel", errorMsg, e)
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure(errorMsg)
            } finally {
                // _processingState should now reflect the final state from the service
                globalLoadingState.popLoading()
                Log.d("CaseViewModel", "processImageEvidence finally block. isLoading set to false. Final ProcessingState: ${_processingState.value}")
            }
        }
    }

    fun processAudioEvidence(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.Main) { // Ensure UI updates are on Main
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processAudioEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            globalLoadingState.pushLoading()
            clearLogs() // Clears logs and sets processingState to Idle, then we set InProgress
            _processingState.value = ProcessingState.InProgress(0.0f) // Initial progress

            val caseToUse = currentCaseFromState ?: run {
                val errorMsg = "Please select a case first to add audio evidence."
                Log.w("CaseViewModel", "processAudioEvidence: No case selected. Message: $errorMsg")
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure("No case selected")
                globalLoadingState.popLoading()
                return@launch
            }

            try {
                Log.i("CaseViewModel", "Processing audio for case: ${caseToUse.name}, URI: $uri")
                _processingState.value = ProcessingState.InProgress(0.1f) // After initial checks, before upload

                // 1. Upload file
                val uploadResult = withContext(Dispatchers.IO) {
                    evidenceRepository.uploadFile(uri, caseToUse.name, caseToUse.spreadsheetId)
                }

                when (uploadResult) {
                    is Result.Success -> {
                        val uploadedFileUriString = uploadResult.data
                        Log.i("CaseViewModel", "Audio file uploaded: $uploadedFileUriString")
                        _userMessage.value = "Raw audio file saved. Starting transcription."
                        _processingState.value = ProcessingState.InProgress(0.25f) // After upload, before transcription

                        // 2. Transcribe audio
                        // The URI passed to transcribeAudio should be the original content URI for Vosk to access
                        val transcriptionResult = transcriptionService.transcribeAudio(uri) 

                        when (transcriptionResult) {
                            is Result.Success -> {
                                val transcribedText = transcriptionResult.data
                                Log.i("CaseViewModel", "Audio transcribed: $transcribedText")
                                _processingState.value = ProcessingState.InProgress(0.75f) // After transcription, before saving evidence

                                val newEvidence =
                                    com.hereliesaz.lexorcist.data.Evidence(
                                        caseId = caseToUse.id.toLong(),
                                        spreadsheetId = caseToUse.spreadsheetId,
                                        type = "audio",
                                        content = transcribedText,
                                        formattedContent = "```\n$transcribedText\n```",
                                        mediaUri = uploadedFileUriString, // Use the uploaded file URI/path
                                        timestamp = System.currentTimeMillis(),
                                        sourceDocument = uploadedFileUriString, // Or original file name if preferred
                                        documentDate = System.currentTimeMillis(), // Consider Exif or other means for original date
                                        allegationId = null,
                                        category = "Audio Transcription",
                                        tags = listOf("audio", "transcription"),
                                        commentary = null,
                                        parentVideoId = null,
                                        entities = com.hereliesaz.lexorcist.DataParser.tagData(transcribedText),
                                    )
                                val savedEvidence = withContext(Dispatchers.IO) {
                                    evidenceRepository.addEvidence(newEvidence)
                                }

                                if (savedEvidence != null) {
                                    _userMessage.value = "Audio evidence processed and saved."
                                    _processingState.value = ProcessingState.Completed("Audio processing complete.")
                                    if (savedEvidence.content.isNotEmpty()) {
                                        _navigateToTranscriptionScreen.emit(savedEvidence.id)
                                    }
                                } else {
                                    val errorMsg = "Failed to save transcribed audio evidence."
                                    Log.e("CaseViewModel", errorMsg)
                                    _userMessage.value = errorMsg
                                    _processingState.value = ProcessingState.Failure(errorMsg)
                                }
                            }
                            is Result.Error -> {
                                val errorMsg = "Transcription failed: ${transcriptionResult.exception.message}"
                                Log.e("CaseViewModel", errorMsg, transcriptionResult.exception)
                                _userMessage.value = errorMsg
                                _processingState.value = ProcessingState.Failure(errorMsg)
                            }
                            is Result.UserRecoverableError -> {
                                val errorMsg = "User recoverable transcription error: ${transcriptionResult.exception.message}"
                                Log.w("CaseViewModel", errorMsg, transcriptionResult.exception)
                                _userMessage.value = errorMsg
                                _userRecoverableAuthIntent.value = transcriptionResult.exception.intent
                                _processingState.value = ProcessingState.Failure(errorMsg)
                            }
                             is Result.Loading -> {
                                // This case should ideally not be returned directly from transcribeAudio if it's a one-shot call.
                                // If it can, we need to decide how to handle it, perhaps by observing TranscriptionService's own state.
                                // For now, treat as unexpected if it gets here from a direct suspend call.
                                Log.w("CaseViewModel", "Transcription returned Loading state, which is unexpected for a direct call.")
                                _processingState.value = ProcessingState.InProgress(0.5f) // Or some other intermediate state
                            }
                        }
                    }
                    is Result.Error -> {
                        val errorMsg = "Error uploading audio file: ${uploadResult.exception.message}"
                        Log.e("CaseViewModel", errorMsg, uploadResult.exception)
                        _userMessage.value = errorMsg
                        _processingState.value = ProcessingState.Failure(errorMsg)
                    }
                    is Result.UserRecoverableError -> {
                        val errorMsg = "User recoverable error uploading audio: ${uploadResult.exception.message}"
                        Log.w("CaseViewModel", errorMsg, uploadResult.exception)
                        _userMessage.value = errorMsg
                        _userRecoverableAuthIntent.value = uploadResult.exception.intent
                        _processingState.value = ProcessingState.Failure(errorMsg)
                    }
                    is Result.Loading -> {
                        // This might occur if evidenceRepository.uploadFile itself can emit Loading.
                        // If so, the UI should reflect this. For now, we are awaiting its completion.
                        Log.i("CaseViewModel", "Audio upload is loading...")
                        _processingState.value = ProcessingState.InProgress(0.15f) // Indicate upload in progress
                        // This path will exit and the try-catch won't see a final Result.Success/Error from upload
                        // for this specific call. Consider how to handle if upload truly is async and emits loading.
                        // For a suspend function, we typically expect it to suspend until a final result.
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error processing audio URI: $uri: ${e.message}"
                Log.e("CaseViewModel", errorMsg, e)
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure(errorMsg)
            } finally {
                globalLoadingState.popLoading()
                // _processingState is set to its final value (Completed or Failure) within the try block.
                // We don't reset it to null here, so the UI can show the final status.
                Log.d("CaseViewModel", "processAudioEvidence finally block. isLoading set to false. Final ProcessingState: ${_processingState.value}")
            }
        }
    }

    fun updateTranscript(evidence: com.hereliesaz.lexorcist.data.Evidence, newTranscript: String, reason: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            try {
                logService.addLog("Updating transcript for evidence ${evidence.id}")
                val result = evidenceRepository.updateTranscript(evidence, newTranscript, reason)
                if (result is Result.Error) {
                    logService.addLog("Error updating transcript: ${result.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                } else {
                    logService.addLog("Transcript updated successfully")
                }
            } finally {
                globalLoadingState.popLoading()
            }
        }
    }

    fun processVideoEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processVideoEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            globalLoadingState.pushLoading()
            clearLogs()
            _processingState.value = ProcessingState.InProgress(0.0f) // Initial progress for video processing

            val caseToUse = currentCaseFromState ?: run {
                val errorMsg = "Please select a case first to add video evidence."
                Log.w("CaseViewModel", "processVideoEvidence: No case selected. Message: $errorMsg")
                _userMessage.value = errorMsg
                _processingState.value = ProcessingState.Failure("No case selected")
                globalLoadingState.popLoading()
                return@launch
            }
            Log.d("CaseViewModel", "Processing video for case: ${caseToUse.name}")
            val workRequest =
                androidx.work.OneTimeWorkRequestBuilder<com.hereliesaz.lexorcist.service.VideoProcessingWorker>()
                    .setInputData(
                        androidx.work.Data
                            .Builder()
                            .putString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_VIDEO_URI, uri.toString())
                            .putInt(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_CASE_ID, caseToUse.id)
                            .putString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_CASE_NAME, caseToUse.name)
                            .putString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_SPREADSHEET_ID, caseToUse.spreadsheetId)
                            .build(),
                    ).build()
            workManager.enqueue(workRequest)
            // isLoading will be set to false once the work request's LiveData indicates completion or failure.
            // For now, _isLoading remains true to indicate background work has started.
            // _processingState will be updated by observing the WorkInfo.
            Log.i("CaseViewModel", "Video processing work enqueued for URI: $uri. ID: ${workRequest.id}")

            workManager.getWorkInfoByIdLiveData(workRequest.id).asFlow().collectLatest { workInfo: androidx.work.WorkInfo? ->
                if (workInfo != null) {
                    val progressPercent = workInfo.progress.getFloat(com.hereliesaz.lexorcist.service.VideoProcessingWorker.PROGRESS_PERCENT, 0f)
                    val progressMessage = workInfo.progress.getString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.PROGRESS_MESSAGE) ?: "Processing video..."
                    
                    _processingState.value = ProcessingState.InProgress(progressPercent) // Update with percentage
                    _videoProcessingProgress.value = "$progressMessage (${(progressPercent * 100).toInt()}%)" // For the separate String progress if still used
                    
                    Log.d("CaseViewModel", "Video processing progress for $uri: $progressMessage ($progressPercent), State: ${workInfo.state}")
                    
                    if (workInfo.state.isFinished) {
                        globalLoadingState.popLoading() // Work is finished, set loading to false
                        _videoProcessingProgress.value = null // Clear the specific string progress
                        when (workInfo.state) {
                            androidx.work.WorkInfo.State.SUCCEEDED -> {
                                val successMessage = workInfo.outputData.getString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.RESULT_SUCCESS) ?: "Video processed successfully."
                                _userMessage.value = successMessage
                                _processingState.value = ProcessingState.Completed(successMessage)
                                Log.i("CaseViewModel", "Video processing SUCCEEDED for URI: $uri. Message: $successMessage")
                            }
                            androidx.work.WorkInfo.State.FAILED -> {
                                val failureMessage = workInfo.outputData.getString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.RESULT_FAILURE) ?: "Video processing failed."
                                _userMessage.value = failureMessage
                                _processingState.value = ProcessingState.Failure(failureMessage)
                                Log.e("CaseViewModel", "Video processing FAILED for URI: $uri. Message: $failureMessage")
                            }
                            androidx.work.WorkInfo.State.CANCELLED -> {
                                val cancelMessage = "Video processing was cancelled."
                                _userMessage.value = cancelMessage
                                _processingState.value = ProcessingState.Failure(cancelMessage)
                                Log.w("CaseViewModel", "Video processing CANCELLED for URI: $uri.")
                            }
                            else -> { /* Other states like ENQUEUED, RUNNING, BLOCKED are handled by InProgress */ }
                        }
                    }
                }
            }
        }
    }

    fun addPhotoGroupEvidence(photoUris: List<android.net.Uri>, description: String) {
        viewModelScope.launch {
            globalLoadingState.pushLoading()
            clearLogs()
            _processingState.value = ProcessingState.InProgress(0.0f)
            var currentProgress = 0.0f
            val progressStep = if (photoUris.isNotEmpty()) 1.0f / photoUris.size else 0f

            try {
                val currentCaseFromState = _vmSelectedCase.value
                Log.d("CaseViewModel", "addPhotoGroupEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
                val caseToUse = currentCaseFromState ?: run {
                    val errorMsg = "Please select a case first to add photos."
                    Log.w("CaseViewModel", "addPhotoGroupEvidence: No case selected. Message: $errorMsg")
                     _userMessage.value = errorMsg
                     _processingState.value = ProcessingState.Failure("No case selected for photo group")
                     globalLoadingState.popLoading()
                    return@launch
                }
                val savedPhotoPaths = mutableListOf<String>()

                photoUris.forEachIndexed { index, uri ->
                    _processingState.value = ProcessingState.InProgress(currentProgress)
                    _userMessage.value = "Uploading photo ${index + 1} of ${photoUris.size}..."
                    val mimeType = applicationContext.contentResolver.getType(uri) ?: "image/jpeg"
                    when (val result = storageService.uploadFile(caseToUse.spreadsheetId, uri, mimeType)) {
                        is Result.Success -> {
                            savedPhotoPaths.add(result.data)
                            currentProgress += progressStep
                            _processingState.value = ProcessingState.InProgress(currentProgress)
                        }
                        is Result.Error -> {
                            val errorMsg = "Failed to save photo ${index + 1}: ${result.exception.message}"
                            _userMessage.value = errorMsg
                            _processingState.value = ProcessingState.Failure(errorMsg)
                            globalLoadingState.popLoading()
                            return@launch // Stop processing further photos on error
                        }
                        is Result.UserRecoverableError -> {
                            val errorMsg = "User error saving photo ${index + 1}: ${result.exception.message}"
                             _userMessage.value = errorMsg
                            _userRecoverableAuthIntent.value = result.exception.intent
                            _processingState.value = ProcessingState.Failure(errorMsg)
                            globalLoadingState.popLoading()
                            return@launch // Stop processing further photos on error
                        }
                        is Result.Loading -> {
                            // If uploadFile can emit Loading, this needs more robust handling, 
                            // for now, we assume it completes or errors.
                             _userMessage.value = "Photo ${index + 1} is uploading..."
                        }
                    }
                }

                if (savedPhotoPaths.isNotEmpty() && savedPhotoPaths.size == photoUris.size) {
                    val mediaUriJson = com.google.gson.Gson().toJson(savedPhotoPaths)
                    val newEvidence =
                        com.hereliesaz.lexorcist.data.Evidence(
                            caseId = caseToUse.id.toLong(),
                            spreadsheetId = caseToUse.spreadsheetId,
                            type = "photo_group",
                            content = description,
                            formattedContent = null,
                            mediaUri = mediaUriJson,
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = "Photo Group",
                            documentDate = System.currentTimeMillis(),
                            allegationId = null,
                            category = "Photo",
                            tags = listOf("photo", "group"),
                            commentary = null,
                            entities = emptyMap(),
                        )
                    evidenceRepository.addEvidence(newEvidence)
                    _userMessage.value = "Photo group evidence saved successfully."
                    _processingState.value = ProcessingState.Completed("Photo group saved.")
                } else if (savedPhotoPaths.isEmpty() && photoUris.isNotEmpty()) {
                    // This case might be hit if all uploads failed and returned before this check
                    if (_processingState.value !is ProcessingState.Failure) {
                        _userMessage.value = "No photos were saved."
                        _processingState.value = ProcessingState.Failure("No photos saved.")
                    }
                }

            } finally {
                globalLoadingState.popLoading()
                if (_processingState.value is ProcessingState.InProgress) {
                     // If loop finished due to error, state would be Failure. If success, Completed.
                     // This is a fallback if somehow it's still InProgress.
                    _processingState.value = ProcessingState.Idle 
                }
            }
        }
    }
}
