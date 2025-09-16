package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.LogService
import com.hereliesaz.lexorcist.service.OcrProcessingService
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.service.TranscriptionService
import com.hereliesaz.lexorcist.service.VideoProcessingWorker
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EvidenceViewModel
    @Inject
    constructor(
        application: Application,
        private val evidenceRepository: EvidenceRepository,
        private val caseRepository: CaseRepository,
        private val settingsManager: SettingsManager,
        private val scriptRunner: ScriptRunner,
        private val ocrProcessingService: OcrProcessingService,
        private val transcriptionService: TranscriptionService,
        private val sharedPreferences: SharedPreferences,
        private val workManager: WorkManager,
        private val logService: LogService,
    ) : AndroidViewModel(application) {
        private val _selectedEvidenceDetails = MutableStateFlow<Evidence?>(null)
        val selectedEvidenceDetails: StateFlow<Evidence?> = _selectedEvidenceDetails.asStateFlow()

        private val _navigateToTranscriptionScreen = MutableSharedFlow<Int>()
        val navigateToTranscriptionScreen = _navigateToTranscriptionScreen.asSharedFlow()

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _userMessage = MutableSharedFlow<String>()
        val userMessage = _userMessage.asSharedFlow()

        private val _videoProcessingProgress = MutableStateFlow<String?>(null)
        val videoProcessingProgress: StateFlow<String?> = _videoProcessingProgress.asStateFlow()

        private val _processingStatus = MutableStateFlow<String?>(null)
        val processingStatus: StateFlow<String?> = _processingStatus.asStateFlow()

        val logMessages: StateFlow<List<String>> = logService.logMessages

        private var currentCaseIdForList: Long? = null
        private var currentSpreadsheetIdForList: String? = null

        fun requestNavigationToTranscriptionScreen(evidenceId: Int) {
            viewModelScope.launch {
                _navigateToTranscriptionScreen.emit(evidenceId)
            }
        }

        fun addTextEvidence(
            text: String,
            caseId: Long,
            spreadsheetId: String,
        ) {
            viewModelScope.launch {
                val entities =
                    com.hereliesaz.lexorcist.DataParser
                        .tagData(text)
                val newEvidence =
                    Evidence(
                        id = 0,
                        caseId = caseId,
                        spreadsheetId = spreadsheetId,
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
                        linkedEvidenceIds = emptyList(),
                        parentVideoId = null,
                        entities = entities,
                    )
                evidenceRepository.addEvidence(newEvidence)
                if (caseId == currentCaseIdForList && spreadsheetId == currentSpreadsheetIdForList) {
                    loadEvidenceForCase(caseId, spreadsheetId)
                }
            }
        }


        fun loadEvidenceById(evidenceId: Int) {
            viewModelScope.launch {
                _selectedEvidenceDetails.value = evidenceRepository.getEvidenceById(evidenceId)
            }
        }

        fun onEvidenceSelected(evidence: Evidence) {
            viewModelScope.launch {
                if (evidence.type == "video") {
                    val allEvidence = evidenceRepository.getEvidenceForCase(evidence.spreadsheetId, evidence.caseId).first()
                    val childEvidence = allEvidence.filter { it.parentVideoId == evidence.sourceDocument }
                    val combinedContent = StringBuilder()
                    combinedContent.append("--- VIDEO TRANSCRIPT ---\n")
                    combinedContent.append(evidence.content)
                    combinedContent.append("\n\n--- OCR FROM FRAMES ---")
                    childEvidence.forEach {
                        combinedContent.append("\n\n--- Frame ---\n")
                        combinedContent.append(it.content)
                    }
                    _selectedEvidenceDetails.value = evidence.copy(
                        content = combinedContent.toString(),
                        formattedContent = evidence.formattedContent, 
                        mediaUri = evidence.mediaUri
                    )
                } else {
                    _selectedEvidenceDetails.value = evidence
                }
            }
        }

        fun onDialogDismiss() {
            _selectedEvidenceDetails.value = null
        }

        fun updateCommentary(
            evidenceId: Int,
            commentary: String,
        ) {
            viewModelScope.launch {
                evidenceRepository.updateCommentary(evidenceId, commentary)
                val currentDetails = _selectedEvidenceDetails.value
                if (currentDetails != null && currentDetails.id == evidenceId) {
                    _selectedEvidenceDetails.value = currentDetails.copy(
                        commentary = commentary,
                        formattedContent = currentDetails.formattedContent,
                        mediaUri = currentDetails.mediaUri
                    )
                }
                currentCaseIdForList?.let { caseId ->
                    currentSpreadsheetIdForList?.let { spreadsheetId ->
                        loadEvidenceForCase(caseId, spreadsheetId)
                    }
                }
            }
        }

        fun clearEvidenceDetails() {
            _selectedEvidenceDetails.value = null
        }

        fun clearLogs() {
            logService.clearLogs()
        }


        fun updateEvidence(evidence: Evidence) {
            viewModelScope.launch {
                evidenceRepository.updateEvidence(evidence)
                val script = settingsManager.getScript()
                val result = scriptRunner.runScript(script, evidence)
                if (result is Result.Success) {
                    val updatedEvidence = evidence.copy(
                        tags = evidence.tags + result.data,
                        formattedContent = evidence.formattedContent, 
                        mediaUri = evidence.mediaUri
                    )
                    evidenceRepository.updateEvidence(updatedEvidence)
                }
                if (evidence.caseId == currentCaseIdForList && evidence.spreadsheetId == currentSpreadsheetIdForList) {
                    loadEvidenceForCase(evidence.caseId, evidence.spreadsheetId)
                }
            }
        }

        fun deleteEvidence(evidence: Evidence) {
            viewModelScope.launch {
                evidenceRepository.deleteEvidence(evidence)
                if (evidence.caseId == currentCaseIdForList && evidence.spreadsheetId == currentSpreadsheetIdForList) {
                    loadEvidenceForCase(evidence.caseId, evidence.spreadsheetId)
                }
            }
        }

        fun onSearchQueryChanged(query: String) {
            _searchQuery.value = query
        }

        fun assignAllegationToEvidence(
            evidenceId: Int,
            allegationId: Int,
        ) {
            viewModelScope.launch {
                val evidence = evidenceRepository.getEvidenceById(evidenceId)
                if (evidence != null) {
                    val updatedEvidence = evidence.copy(
                        allegationId = allegationId,
                        formattedContent = evidence.formattedContent,
                        mediaUri = evidence.mediaUri
                    )
                    evidenceRepository.updateEvidence(updatedEvidence)
                    if (evidence.caseId == currentCaseIdForList && evidence.spreadsheetId == currentSpreadsheetIdForList) {
                        loadEvidenceForCase(evidence.caseId, evidence.spreadsheetId)
                    }
                }
            }
        }

        fun processImageEvidence(uri: Uri) {
            viewModelScope.launch {
                clearLogs()
                val caseId = currentCaseIdForList
                val spreadsheetId = currentSpreadsheetIdForList
                if (caseId != null && spreadsheetId != null) {
                    _isLoading.value = true
                    try {
                        _processingStatus.value = "Uploading image..."
                        val (newEvidence, message) = ocrProcessingService.processImage(
                            uri = uri,
                            context = getApplication(),
                            caseId = caseId,
                            spreadsheetId = spreadsheetId,
                        )
                        _processingStatus.value = "Image processing complete."
                        message?.let { viewModelScope.launch { _userMessage.emit(it) } }
                        if (newEvidence != null && newEvidence.content.isEmpty()) {
                            viewModelScope.launch { _userMessage.emit("No text found in the image.") }
                        }
                    } finally {
                        _isLoading.value = false
                        _processingStatus.value = null
                        loadEvidenceForCase(caseId, spreadsheetId)
                    }
                }
            }
        }

        fun processAudioEvidence(uri: Uri) {
            viewModelScope.launch {
                clearLogs()
                if (currentCaseIdForList != null && currentSpreadsheetIdForList != null) {
                    _isLoading.value = true
                    try {
                        _processingStatus.value = "Uploading audio..."
                        val case = caseRepository.getCaseBySpreadsheetId(currentSpreadsheetIdForList!!)
                        if (case != null) {
                            val uploadResult = evidenceRepository.uploadFile(uri, case.name, case.spreadsheetId)
                            if (uploadResult is Result.Success) {
                                viewModelScope.launch { _userMessage.emit("Raw evidence file saved.") }
                                _processingStatus.value = "Transcribing audio..."
                                val (transcribedText, message) = transcriptionService.transcribeAudio(uri)
                                message?.let { viewModelScope.launch { _userMessage.emit(it) } }
                                _processingStatus.value = "Audio processing complete."

                                val newEvidence =
                                    Evidence(
                                        id = 0,
                                        caseId = currentCaseIdForList!!,
                                        spreadsheetId = currentSpreadsheetIdForList!!,
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
                                if (newEvidenceWithId != null) {
                                    if (transcribedText.isNotEmpty()) {
                                        _navigateToTranscriptionScreen.emit(newEvidenceWithId.id)
                                    }
                                 }
                            }
                        }
                    } finally {
                        _isLoading.value = false
                        _processingStatus.value = null
                        loadEvidenceForCase(currentCaseIdForList!!, currentSpreadsheetIdForList!!)
                    }
                }
            }
        }

        fun updateTranscript(
            evidence: Evidence,
            newTranscript: String,
            reason: String,
        ) {
            viewModelScope.launch {
                logService.addLog("Updating transcript for evidence ${evidence.id}")
                val result = evidenceRepository.updateTranscript(evidence, newTranscript, reason)
                if (result is Result.Error) {
                    logService.addLog("Error updating transcript: ${result.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                } else {
                    logService.addLog("Transcript updated successfully")
                }
            }
        }

        fun processVideoEvidence(uri: Uri) {
            viewModelScope.launch {
                clearLogs()
                if (currentCaseIdForList != null && currentSpreadsheetIdForList != null) {
                    val case = caseRepository.getCaseBySpreadsheetId(currentSpreadsheetIdForList!!)
                    if (case != null) {
                        val workRequest =
                            OneTimeWorkRequestBuilder<VideoProcessingWorker>()
                                .setInputData(
                                    Data
                                        .Builder()
                                        .putString(VideoProcessingWorker.KEY_VIDEO_URI, uri.toString())
                                        .putInt(VideoProcessingWorker.KEY_CASE_ID, case.id)
                                        .putString(VideoProcessingWorker.KEY_CASE_NAME, case.name)
                                        .putString(VideoProcessingWorker.KEY_SPREADSHEET_ID, case.spreadsheetId)
                                        .build(),
                                ).build()
                        workManager.enqueue(workRequest)
                        workManager.getWorkInfoByIdLiveData(workRequest.id).asFlow().collectLatest { workInfo ->
                            if (workInfo != null) {
                                val progress = workInfo.progress.getString(VideoProcessingWorker.PROGRESS)
                                _videoProcessingProgress.value = progress
                                if (workInfo.state.isFinished) {
                                    _videoProcessingProgress.value = null // <<< Corrected here
                                    loadEvidenceForCase(currentCaseIdForList!!, currentSpreadsheetIdForList!!)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
