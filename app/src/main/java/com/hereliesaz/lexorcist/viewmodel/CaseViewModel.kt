package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
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
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) : ViewModel() {
    private val sharedPref =
        applicationContext.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

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

    val selectedCase: StateFlow<Case?> =
        caseRepository.selectedCase.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val sheetFilters: StateFlow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations.asStateFlow()

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

    private val _userRecoverableAuthIntent = MutableStateFlow<Intent?>(null)
    val userRecoverableAuthIntent: StateFlow<Intent?> =
        _userRecoverableAuthIntent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    private val _videoProcessingProgress = MutableStateFlow<String?>(null)
    val videoProcessingProgress: StateFlow<String?> = _videoProcessingProgress.asStateFlow()

    private val _processingStatus = MutableStateFlow<String?>(null)
    val processingStatus: StateFlow<String?> = _processingStatus.asStateFlow()

    val logMessages: StateFlow<List<com.hereliesaz.lexorcist.model.LogEntry>> = logService.logMessages

    private val _navigateToTranscriptionScreen = MutableSharedFlow<Int>()
    val navigateToTranscriptionScreen = _navigateToTranscriptionScreen.asSharedFlow()

    private val _timelineSortType = MutableStateFlow(TimelineSortType.DATE_OF_OCCURRENCE)
    val timelineSortType: StateFlow<TimelineSortType> = _timelineSortType.asStateFlow()

    private val _selectedCaseEvidenceList =
        MutableStateFlow<List<com.hereliesaz.lexorcist.data.Evidence>>(emptyList())

    val selectedCaseEvidenceList: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        _selectedCaseEvidenceList
            .combine(timelineSortType) { evidence, sortType ->
                when (sortType) {
                    TimelineSortType.DATE_OF_OCCURRENCE -> evidence.sortedByDescending { it.documentDate }
                    TimelineSortType.DATE_EVIDENCE_ADDED -> evidence.sortedByDescending { it.timestamp }
                    TimelineSortType.BY_ALLEGATION -> evidence.sortedBy { it.allegationId }
                    TimelineSortType.BY_FILE_TYPE -> evidence.sortedBy { it.type }
                    TimelineSortType.CUSTOM -> evidence // Placeholder for custom sort
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedEvidence: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        selectedCaseEvidenceList.combine(
            selectedCaseEvidenceList
        ) { list, _ ->
            list.filter { it.isSelected }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onTimelineSortOrderChanged(sortType: TimelineSortType) {
        _timelineSortType.value = sortType
    }

    private val _themeMode =
        MutableStateFlow(com.hereliesaz.lexorcist.ui.theme.ThemeMode.SYSTEM)
    val themeMode: StateFlow<com.hereliesaz.lexorcist.ui.theme.ThemeMode> =
        _themeMode.asStateFlow()

    private val _storageLocation = MutableStateFlow<String?>(null)
    val storageLocation: StateFlow<String?> = _storageLocation.asStateFlow()

    init {
        loadThemeModePreference()
        _storageLocation.value = settingsManager.getStorageLocation()
    }

    fun setStorageLocation(uri: android.net.Uri) {
        viewModelScope.launch {
            val oldLocation = settingsManager.getStorageLocation()
            settingsManager.saveStorageLocation(uri.toString())
            _storageLocation.value = uri.toString()
            if (oldLocation != null) {
                viewModelScope.launch { _userMessage.emit("Moving files to new location...") }
                localFileStorageService.moveFilesToNewLocation(oldLocation, uri.toString())
                viewModelScope.launch { _userMessage.emit("Files moved successfully.") }
            }
        }
    }

    private fun clearCaseData() {
        viewModelScope.launch { caseRepository.selectCase(null) }
        _sheetFilters.value = emptyList()
        _allegations.value = emptyList()
        _htmlTemplates.value = emptyList()
        _plaintiffs.value = ""
        _defendants.value = ""
        _court.value = ""
        _selectedCaseEvidenceList.value = emptyList()
        saveCaseInfoToSharedPrefs()
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearUserRecoverableAuthIntent() {
        _userRecoverableAuthIntent.value = null
    }

    fun setThemeMode(themeMode: com.hereliesaz.lexorcist.ui.theme.ThemeMode) {
        _themeMode.value = themeMode
        sharedPref.edit().putString("theme_mode", themeMode.name).apply()
    }

    private fun loadThemeModePreference() {
        val themeName =
            sharedPref.getString(
                "theme_mode",
                com.hereliesaz.lexorcist.ui.theme.ThemeMode.SYSTEM.name
            )
        _themeMode.value =
            com.hereliesaz.lexorcist.ui.theme.ThemeMode.valueOf(
                themeName ?: com.hereliesaz.lexorcist.ui.theme.ThemeMode.SYSTEM.name
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
            _isLoading.value = true
            try {
                caseRepository.refreshCases()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHtmlTemplatesFromRepository() {
        viewModelScope.launch {
            caseRepository.refreshHtmlTemplates()
            caseRepository.getHtmlTemplates().collect {
                _htmlTemplates.value = it
            }
        }
    }

    fun importSpreadsheetWithRepository(spreadsheetId: String) {
        viewModelScope.launch { caseRepository.importSpreadsheet(spreadsheetId) }
    }

    fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String,
    ) {
        android.util.Log.d("CaseViewModel", "createCase called with name: $caseName")
        viewModelScope.launch {
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
                    android.util.Log.d("CaseViewModel", "Case creation successful")
                }
                is Result.Error -> {
                    _errorMessage.value =
                        result.exception.message ?: "Unknown error during case creation"
                }
                is Result.UserRecoverableError -> {
                    _userRecoverableAuthIntent.value = result.exception.intent
                }
            }
        }
    }

    fun selectCase(case: Case?) {
        viewModelScope.launch {
            caseRepository.selectCase(case)
            if (case != null) {
                loadSheetFiltersFromRepository(case.spreadsheetId)
                loadAllegationsFromRepository(case.id, case.spreadsheetId)
                loadHtmlTemplatesFromRepository()
                loadEvidenceForSelectedCase()
            } else {
                _sheetFilters.value = emptyList()
                _allegations.value = emptyList()
                _htmlTemplates.value = emptyList()
                _selectedCaseEvidenceList.value = emptyList()
            }
        }
    }

    private fun loadSheetFiltersFromRepository(spreadsheetId: String) {
        viewModelScope.launch {
            caseRepository.refreshSheetFilters(spreadsheetId)
            caseRepository.getSheetFilters(spreadsheetId).collect {
                _sheetFilters.value = it
            }
        }
    }

    fun addSheetFilterWithRepository(
        name: String,
        value: String,
    ) {
        viewModelScope.launch {
            val spreadsheetId = selectedCase.value?.spreadsheetId ?: return@launch
            caseRepository.addSheetFilter(spreadsheetId, name, value)
        }
    }

    private fun loadAllegationsFromRepository(
        caseId: Int,
        spreadsheetId: String,
    ) {
        viewModelScope.launch {
            caseRepository.refreshAllegations(caseId, spreadsheetId)
            caseRepository.getAllegations(caseId, spreadsheetId).collect {
                _allegations.value = it
            }
        }
    }

    fun addAllegationWithRepository(allegationText: String) {
        viewModelScope.launch {
            val case = selectedCase.value ?: return@launch
            caseRepository.addAllegation(case.spreadsheetId, allegationText)
            loadAllegationsFromRepository(case.id, case.spreadsheetId)
        }
    }

    fun toggleEvidenceSelection(evidenceId: Int) {
        val updatedList =
            _selectedCaseEvidenceList.value.map {
                if (it.id == evidenceId) {
                    it.copy(isSelected = !it.isSelected)
                } else {
                    it
                }
            }
        _selectedCaseEvidenceList.value = updatedList
    }

    fun clearEvidenceSelection() {
        val list =
            _selectedCaseEvidenceList.value.map {
                it.copy(isSelected = false)
            }
        _selectedCaseEvidenceList.value = list
    }

    fun assignAllegationToEvidence(
        evidenceId: Int,
        allegationId: Int,
    ) {
        viewModelScope.launch {
            val evidence = _selectedCaseEvidenceList.value.find { it.id == evidenceId }
            if (evidence != null) {
                val updatedEvidence = evidence.copy(allegationId = allegationId)
                evidenceRepository.updateEvidence(updatedEvidence)
                loadEvidenceForSelectedCase()
            }
        }
    }

    internal fun loadEvidenceForSelectedCase() {
        viewModelScope.launch {
            selectedCase.value?.let { case ->
                when (val result = caseRepository.getEvidenceForCase(case.spreadsheetId)) {
                    is Result.Success -> {
                        _selectedCaseEvidenceList.value = result.data
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.exception.message ?: "Unknown error"
                    }
                    is Result.UserRecoverableError -> {
                        _userRecoverableAuthIntent.value = result.exception.intent
                    }
                }
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
        viewModelScope.launch { caseRepository.archiveCase(case) }
    }

    fun deleteCaseWithRepository(case: Case) {
        viewModelScope.launch { caseRepository.deleteCase(case) }
    }

    fun clearCache() {
        viewModelScope.launch {
            caseRepository.clearCache()
            clearCaseData()
            // Clear shared preferences
            sharedPref.edit().clear().apply()
            // After clearing, reload the theme preference as it's also stored in sharedPref
            loadThemeModePreference()
        }
    }

    fun updateEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            evidenceRepository.updateEvidence(evidence)
            val script = settingsManager.getScript()
            val result = scriptRunner.runScript(script, evidence)
            if (result is Result.Success) {
                val updatedEvidence = evidence.copy(
                    tags = evidence.tags + result.data
                )
                evidenceRepository.updateEvidence(updatedEvidence)
            }
            loadEvidenceForSelectedCase()
        }
    }

    fun deleteEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            evidenceRepository.deleteEvidence(evidence)
            loadEvidenceForSelectedCase()
        }
    }

    fun assignAllegationToSelectedEvidence(allegationId: Int) {
        viewModelScope.launch {
            selectedEvidence.value.forEach { evidence ->
                val updatedEvidence = evidence.copy(allegationId = allegationId)
                evidenceRepository.updateEvidence(updatedEvidence)
            }
            clearEvidenceSelection()
            loadEvidenceForSelectedCase()
        }
    }

    fun clearLogs() {
        logService.clearLogs()
    }

    fun addTextEvidence(text: String) {
        viewModelScope.launch {
            val case = selectedCase.value ?: return@launch
            val entities = com.hereliesaz.lexorcist.DataParser.tagData(text)
            val newEvidence =
                com.hereliesaz.lexorcist.data.Evidence(
                    caseId = case.id.toLong(),
                    spreadsheetId = case.spreadsheetId,
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
            loadEvidenceForSelectedCase()
        }
    }

    fun updateCommentary(evidenceId: Int, commentary: String) {
        viewModelScope.launch {
            evidenceRepository.updateCommentary(evidenceId, commentary)
            loadEvidenceForSelectedCase()
        }
    }

    fun processImageEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            clearLogs()
            val case = selectedCase.value ?: return@launch
            _isLoading.value = true
            try {
                _processingStatus.value = "Uploading image..."
                val (newEvidence, message) = ocrProcessingService.processImage(
                    uri = uri,
                    context = applicationContext,
                    caseId = case.id.toLong(),
                    spreadsheetId = case.spreadsheetId,
                )
                _processingStatus.value = "Image processing complete."
                message?.let { viewModelScope.launch { _userMessage.emit(it) } }
                if (newEvidence != null && newEvidence.content.isEmpty()) {
                    viewModelScope.launch { _userMessage.emit("No text found in the image.") }
                }
            } finally {
                _isLoading.value = false
                _processingStatus.value = null
                loadEvidenceForSelectedCase()
            }
        }
    }

    fun processAudioEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            clearLogs()
            val case = selectedCase.value ?: return@launch
            _isLoading.value = true
            try {
                _processingStatus.value = "Uploading audio..."
                val uploadResult = evidenceRepository.uploadFile(uri, case.name, case.spreadsheetId)
                if (uploadResult is Result.Success) {
                    viewModelScope.launch { _userMessage.emit("Raw evidence file saved.") }
                    _processingStatus.value = "Transcribing audio..."
                    val (transcribedText, message) = transcriptionService.transcribeAudio(uri)
                    message?.let { viewModelScope.launch { _userMessage.emit(it) } }
                    _processingStatus.value = "Audio processing complete."

                    val newEvidence =
                        com.hereliesaz.lexorcist.data.Evidence(
                            caseId = case.id.toLong(),
                            spreadsheetId = case.spreadsheetId,
                            type = "audio",
                            content = transcribedText,
                            formattedContent = "```\n$transcribedText\n```",
                            mediaUri = uri.toString(),
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = uploadResult.data ?: uri.toString(),
                            documentDate = System.currentTimeMillis(),
                            allegationId = null,
                            category = "Audio Transcription",
                            tags = listOf("audio", "transcription"),
                            commentary = null,
                            parentVideoId = null,
                            entities = emptyMap(),
                        )
                    val newEvidenceWithId = evidenceRepository.addEvidence(newEvidence)
                    if (newEvidenceWithId != null && newEvidenceWithId.content.isNotEmpty()) {
                        _navigateToTranscriptionScreen.emit(newEvidenceWithId.id)
                    }
                }
            } finally {
                _isLoading.value = false
                _processingStatus.value = null
                loadEvidenceForSelectedCase()
            }
        }
    }

    fun updateTranscript(evidence: com.hereliesaz.lexorcist.data.Evidence, newTranscript: String, reason: String) {
        viewModelScope.launch {
            logService.addLog("Updating transcript for evidence ${evidence.id}")
            val result = evidenceRepository.updateTranscript(evidence, newTranscript, reason)
            if (result is Result.Error) {
                logService.addLog("Error updating transcript: ${result.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            } else {
                logService.addLog("Transcript updated successfully")
                loadEvidenceForSelectedCase()
            }
        }
    }

    fun processVideoEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            clearLogs()
            val case = selectedCase.value ?: return@launch
            val workRequest =
                androidx.work.OneTimeWorkRequestBuilder<com.hereliesaz.lexorcist.service.VideoProcessingWorker>()
                    .setInputData(
                        androidx.work.Data
                            .Builder()
                            .putString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_VIDEO_URI, uri.toString())
                            .putInt(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_CASE_ID, case.id)
                            .putString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_CASE_NAME, case.name)
                            .putString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.KEY_SPREADSHEET_ID, case.spreadsheetId)
                            .build(),
                    ).build()
            workManager.enqueue(workRequest)
            workManager.getWorkInfoByIdLiveData(workRequest.id).asFlow().collectLatest { workInfo: androidx.work.WorkInfo? ->
                if (workInfo != null) {
                    val progress = workInfo.progress.getString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.PROGRESS)
                    _videoProcessingProgress.value = progress
                    if (workInfo.state.isFinished) {
                        _videoProcessingProgress.value = null
                        loadEvidenceForSelectedCase()
                    }
                }
            }
        }
    }
}
