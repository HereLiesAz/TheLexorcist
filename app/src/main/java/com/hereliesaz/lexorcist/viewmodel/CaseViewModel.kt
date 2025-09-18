package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log // Added import for Log
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
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.viewmodel.TimelineSortType // Ensured import
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow // Ensured import
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
) : ViewModel() {
    private val sharedPref =
        applicationContext.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _processingStatus = MutableStateFlow<String?>(null)
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

    // New way of handling selectedCase in ViewModel
    private val _vmSelectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _vmSelectedCase.asStateFlow()

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val sheetFilters: StateFlow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    val allegations: StateFlow<List<Allegation>> =
        caseRepository.selectedCaseAllegations // This should probably also collect into a _vmAllegations
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

    private val _processingState = MutableStateFlow<ProcessingState?>(null)
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
                _logMessages.value = listOf(newLog) + _logMessages.value
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
                _isLoading.value = true // Indicate loading while processing the result
                try {
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
                    }
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun setStorageLocation(uri: android.net.Uri) {
        viewModelScope.launch {
            _isLoading.value = true
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
                _isLoading.value = false
            }
        }
    }

    private fun clearCaseData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                caseRepository.selectCase(null) 
                _sheetFilters.value = emptyList()
                _htmlTemplates.value = emptyList()
                _plaintiffs.value = ""
                _defendants.value = ""
                _court.value = ""
                saveCaseInfoToSharedPrefs()
            } finally {
                _isLoading.value = false
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
            _isLoading.value = true
            try {
                caseRepository.refreshHtmlTemplates()
                caseRepository.getHtmlTemplates().collect {
                    _htmlTemplates.value = it
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importSpreadsheetWithRepository(spreadsheetId: String) {
        viewModelScope.launch { 
            _isLoading.value = true
            try {
                caseRepository.importSpreadsheet(spreadsheetId) 
            } finally {
                _isLoading.value = false
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
            _isLoading.value = true
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
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCase(case: Case?) {
        Log.d("CaseViewModel", "--- CaseViewModel.selectCase ENTERED with case: ${case?.name ?: "null"} --- instance: $this, repo instance: $caseRepository")
        viewModelScope.launch {
            Log.d("CaseViewModel", "viewModelScope.launch in selectCase for case: ${case?.name ?: "null"}")
            Log.d("CaseViewModel", "isLoading SET TO true in selectCase for case: ${case?.name ?: "null"}")
            _isLoading.value = true
            try {
                caseRepository.selectCase(case) // This call should trigger the collector in init
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
                Log.d("CaseViewModel", "isLoading SET TO false in selectCase finally block for case: ${case?.name ?: "null"}")
                _isLoading.value = false
            }
        }
    }

    private fun loadSheetFiltersFromRepository(spreadsheetId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                caseRepository.refreshSheetFilters(spreadsheetId)
                caseRepository.getSheetFilters(spreadsheetId).collect {
                    _sheetFilters.value = it
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSheetFilterWithRepository(
        name: String,
        value: String,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val spreadsheetId = selectedCase.value?.spreadsheetId ?: return@launch
                caseRepository.addSheetFilter(spreadsheetId, name, value)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addAllegationWithRepository(allegationText: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val case = selectedCase.value ?: return@launch
                caseRepository.addAllegation(case.spreadsheetId, allegationText)
            } finally {
                _isLoading.value = false
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
            _isLoading.value = true
            try {
                val evidence = _selectedCaseEvidenceListInternal.value.find { it.id == evidenceId }
                if (evidence != null) {
                    val updatedEvidence = evidence.copy(allegationId = allegationId)
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
            } finally {
                _isLoading.value = false
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
            _isLoading.value = true
            try {
                caseRepository.archiveCase(case) 
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCaseWithRepository(case: Case) {
        viewModelScope.launch { 
            _isLoading.value = true
            try {
                caseRepository.deleteCase(case) 
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                caseRepository.clearCache()
                clearCaseData()
                sharedPref.edit().clear().apply()
                loadThemeModePreference()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                evidenceRepository.updateEvidence(evidence)
                val script = settingsManager.getScript()
                val result = scriptRunner.runScript(script, evidence)
                if (result is Result.Success) {
                    val updatedEvidence = evidence.copy(
                        tags = evidence.tags + result.data
                    )
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEvidence(evidence: com.hereliesaz.lexorcist.data.Evidence) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                evidenceRepository.deleteEvidence(evidence)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignAllegationToSelectedEvidence(allegationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                selectedEvidence.value.forEach { evidence ->
                    val updatedEvidence = evidence.copy(allegationId = allegationId)
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
                clearEvidenceSelection()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearLogs() {
        _logMessages.value = emptyList()
    }

    fun addTextEvidence(text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                 val currentCaseFromState = _vmSelectedCase.value
                Log.d("CaseViewModel", "addTextEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
                val caseToUse = currentCaseFromState ?: run {
                    Log.w("CaseViewModel", "addTextEvidence: No case selected from StateFlow, aborting.")
                    viewModelScope.launch { _userMessage.emit("Please select a case first to add text evidence.") }
                    _isLoading.value = false
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
                _isLoading.value = false
            }
        }
    }

    fun updateCommentary(evidenceId: Int, commentary: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                evidenceRepository.updateCommentary(evidenceId, commentary)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processImageEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processImageEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            _isLoading.value = true 
            clearLogs()
            val caseToUse = currentCaseFromState ?: run {
                Log.w("CaseViewModel", "processImageEvidence: No case selected from StateFlow, aborting.")
                viewModelScope.launch { _userMessage.emit("Please select a case first to add image evidence.") }
                _isLoading.value = false 
                return@launch
            }
            try {
                Log.d("CaseViewModel", "Processing image for case: ${caseToUse.name}")
                val (newEvidence, message) = ocrProcessingService.processImage(
                    uri = uri,
                    context = applicationContext,
                    caseId = caseToUse.id.toLong(),
                    spreadsheetId = caseToUse.spreadsheetId,
                ) {
                    _processingState.value = it
                }
                message?.let { 
                    Log.i("CaseViewModel", "Message from ocrProcessingService: $it")
                    viewModelScope.launch { _userMessage.emit(it) } 
                }
                if (newEvidence != null && newEvidence.content.isEmpty()) {
                    Log.i("CaseViewModel", "No text found in image for URI: $uri")
                    viewModelScope.launch { _userMessage.emit("No text found in the image.") }
                }
                Log.d("CaseViewModel", "Image processing finished for URI: $uri")
            } catch (e: Exception) {
                Log.e("CaseViewModel", "Error processing image URI: $uri", e)
                viewModelScope.launch { _userMessage.emit("Error processing image: ${e.message}") }
            } finally {
                _processingState.value = null
                _isLoading.value = false 
                Log.d("CaseViewModel", "processImageEvidence finally block. isLoading set to false.")
            }
        }
    }

    fun processAudioEvidence(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processAudioEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            withContext(Dispatchers.Main) { _isLoading.value = true }
            clearLogs()
            val caseToUse = currentCaseFromState ?: run {
                Log.w("CaseViewModel", "processAudioEvidence: No case selected from StateFlow, aborting.")
                viewModelScope.launch(Dispatchers.Main) { _userMessage.emit("Please select a case first to add audio evidence.") }
                withContext(Dispatchers.Main) { _isLoading.value = false }
                return@launch
            }

            try {
                Log.d("CaseViewModel", "Processing audio for case: ${caseToUse.name}")
                withContext(Dispatchers.Main) {
                    _processingStatus.value = "Uploading audio..."
                }
                val uploadResult = evidenceRepository.uploadFile(uri, caseToUse.name, caseToUse.spreadsheetId)
                if (uploadResult is Result.Success) {
                    Log.i("CaseViewModel", "Audio file uploaded: ${uploadResult.data}")
                    withContext(Dispatchers.Main) {
                        _userMessage.value = "Raw evidence file saved."
                        _processingStatus.value = "Transcribing audio..."
                    }

                    val (transcribedText, message) = transcriptionService.transcribeAudio(uri)
                    message?.let {
                        Log.i("CaseViewModel", "Message from transcriptionService: $it")
                        withContext(Dispatchers.Main) { _userMessage.value = it }
                    }
                    withContext(Dispatchers.Main) {
                        _processingStatus.value = "Audio processing complete."
                    }
                    Log.i("CaseViewModel", "Audio transcribed: $transcribedText")

                    val newEvidence =
                        com.hereliesaz.lexorcist.data.Evidence(
                            caseId = caseToUse.id.toLong(),
                            spreadsheetId = caseToUse.spreadsheetId,
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
                        withContext(Dispatchers.Main) {
                            _navigateToTranscriptionScreen.emit(newEvidenceWithId.id)
                        }
                    }
                } else if (uploadResult is Result.Error) {
                    Log.e("CaseViewModel", "Error uploading audio file: ${uploadResult.exception.message}", uploadResult.exception)
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = uploadResult.exception.message ?: "Error uploading audio file."
                    }
                } else if (uploadResult is Result.UserRecoverableError) {
                    Log.w("CaseViewModel", "User recoverable error uploading audio: ${uploadResult.exception.message}")
                    withContext(Dispatchers.Main) {
                        _userRecoverableAuthIntent.value = uploadResult.exception.intent
                    }
                }
            } catch (e: Exception) {
                Log.e("CaseViewModel", "Error processing audio URI: $uri", e)
                withContext(Dispatchers.Main) { _userMessage.emit("Error processing audio: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _processingStatus.value = null
                    Log.d("CaseViewModel", "processAudioEvidence finally block. isLoading set to false.")
                }
            }
        }
    }

    fun updateTranscript(evidence: com.hereliesaz.lexorcist.data.Evidence, newTranscript: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                logService.addLog("Updating transcript for evidence ${evidence.id}")
                val result = evidenceRepository.updateTranscript(evidence, newTranscript, reason)
                if (result is Result.Error) {
                    logService.addLog("Error updating transcript: ${result.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                } else {
                    logService.addLog("Transcript updated successfully")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processVideoEvidence(uri: android.net.Uri) {
        viewModelScope.launch {
            val currentCaseFromState = _vmSelectedCase.value
            Log.d("CaseViewModel", "processVideoEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
            _isLoading.value = true 
            clearLogs()
            val caseToUse = currentCaseFromState ?: run {
                Log.w("CaseViewModel", "processVideoEvidence: No case selected from StateFlow, aborting.")
                viewModelScope.launch { _userMessage.emit("Please select a case first to add video evidence.") }
                _isLoading.value = false 
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
            _isLoading.value = false // Set loading false after enqueuing
            Log.i("CaseViewModel", "Video processing work enqueued for URI: $uri")

            workManager.getWorkInfoByIdLiveData(workRequest.id).asFlow().collectLatest { workInfo: androidx.work.WorkInfo? ->
                if (workInfo != null) {
                    val progress = workInfo.progress.getString(com.hereliesaz.lexorcist.service.VideoProcessingWorker.PROGRESS)
                    _videoProcessingProgress.value = progress
                    Log.d("CaseViewModel", "Video processing progress for $uri: $progress, State: ${workInfo.state}")
                    if (workInfo.state.isFinished) {
                        _videoProcessingProgress.value = null
                        Log.i("CaseViewModel", "Video processing finished for URI: $uri, State: ${workInfo.state}")
                    }
                }
            }
        }
    }

    fun addPhotoGroupEvidence(photoUris: List<android.net.Uri>, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentCaseFromState = _vmSelectedCase.value
                Log.d("CaseViewModel", "addPhotoGroupEvidence: _vmSelectedCase.value AT START is: ${currentCaseFromState?.name ?: "null"}")
                val caseToUse = currentCaseFromState ?: run {
                    Log.w("CaseViewModel", "addPhotoGroupEvidence: No case selected from StateFlow, aborting.")
                    viewModelScope.launch { _userMessage.emit("Please select a case first to add photos.") }
                     _isLoading.value = false
                    return@launch
                }
                val savedPhotoPaths = mutableListOf<String>()

                photoUris.forEach { uri ->
                    val mimeType = applicationContext.contentResolver.getType(uri) ?: "image/jpeg"
                    when (val result = storageService.uploadFile(caseToUse.spreadsheetId, uri, mimeType)) {
                        is Result.Success -> {
                            savedPhotoPaths.add(result.data)
                        }
                        is Result.Error -> {
                            _errorMessage.value = "Failed to save photo: ${result.exception.message}"
                        }
                        is Result.UserRecoverableError -> {
                            _userRecoverableAuthIntent.value = result.exception.intent
                        }
                    }
                }

                if (savedPhotoPaths.isNotEmpty()) {
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
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
