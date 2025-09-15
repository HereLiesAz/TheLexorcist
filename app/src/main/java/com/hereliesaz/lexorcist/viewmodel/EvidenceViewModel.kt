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
    ) : AndroidViewModel(application) {
        private val _selectedEvidenceDetails = MutableStateFlow<Evidence?>(null)
        val selectedEvidenceDetails: StateFlow<Evidence?> = _selectedEvidenceDetails.asStateFlow()

        private val _navigateToTranscriptionScreen = MutableSharedFlow<Int>()
        val navigateToTranscriptionScreen = _navigateToTranscriptionScreen.asSharedFlow()

        private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        val evidenceList: StateFlow<List<Evidence>> =
            _evidenceList
                .combine(_searchQuery) { evidence, query ->
                    if (query.isBlank()) {
                        evidence
                    } else {
                        evidence.filter { it.content.contains(query, ignoreCase = true) }
                    }
                }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private var currentCaseIdForList: Long? = null
        private var currentSpreadsheetIdForList: String? = null

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

        fun toggleEvidenceSelection(evidenceId: Int) {
            val updatedList =
                _evidenceList.value.map {
                    if (it.id == evidenceId) {
                        it.copy(isSelected = !it.isSelected)
                    } else {
                        it
                    }
                }
            _evidenceList.value = updatedList

            val toggledEvidence = updatedList.find { it.id == evidenceId }
            if (toggledEvidence != null) {
                viewModelScope.launch {
                    evidenceRepository.updateEvidence(toggledEvidence)
                }
            }
        }

        fun clearEvidenceSelection() {
            val list =
                _evidenceList.value.map {
                    it.copy(isSelected = false)
                }
            _evidenceList.value = list
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
                    _selectedEvidenceDetails.value = evidence.copy(content = combinedContent.toString())
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
                    _selectedEvidenceDetails.value = currentDetails.copy(commentary = commentary)
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

        fun loadEvidenceForCase(
            caseId: Long,
            spreadsheetId: String,
        ) {
            currentCaseIdForList = caseId
            currentSpreadsheetIdForList = spreadsheetId
            viewModelScope.launch {
                _isLoading.value = true
                evidenceRepository.getEvidenceForCase(spreadsheetId, caseId).collectLatest { evidence ->
                    if (evidence.isEmpty()) {
                        _evidenceList.value = createPlaceholderEvidence()
                    } else {
                        _evidenceList.value = evidence
                    }
                }
                _isLoading.value = false
            }
        }

        private fun createPlaceholderEvidence(): List<Evidence> =
            listOf(
                Evidence(
                    id = -1,
                    caseId = 0,
                    spreadsheetId = "",
                    type = "placeholder",
                    content = "This is a placeholder item.",
                    timestamp = 0,
                    sourceDocument = "",
                    documentDate = 0,
                    allegationId = null,
                    category = "Placeholder",
                    tags = emptyList(),
                    commentary = null,
                    linkedEvidenceIds = emptyList(),
                    parentVideoId = null,
                    entities = emptyMap(),
                    isSelected = false,
                ),
                Evidence(
                    id = -2,
                    caseId = 0,
                    spreadsheetId = "",
                    type = "placeholder",
                    content = "Add your first piece of evidence to get started.",
                    timestamp = 0,
                    sourceDocument = "",
                    documentDate = 0,
                    allegationId = null,
                    category = "Placeholder",
                    tags = emptyList(),
                    commentary = null,
                    linkedEvidenceIds = emptyList(),
                    parentVideoId = null,
                    entities = emptyMap(),
                    isSelected = false,
                ),
            )

        fun updateEvidence(evidence: Evidence) {
            viewModelScope.launch {
                evidenceRepository.updateEvidence(evidence)
                val script = settingsManager.getScript()
                val result = scriptRunner.runScript(script, evidence)
                if (result is Result.Success) {
                    val updatedEvidence = evidence.copy(tags = evidence.tags + result.data)
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
                    val updatedEvidence = evidence.copy(allegationId = allegationId)
                    evidenceRepository.updateEvidence(updatedEvidence)
                    if (evidence.caseId == currentCaseIdForList && evidence.spreadsheetId == currentSpreadsheetIdForList) {
                        loadEvidenceForCase(evidence.caseId, evidence.spreadsheetId)
                    }
                }
            }
        }

        fun processImageEvidence(uri: Uri) {
            viewModelScope.launch {
                val caseId = currentCaseIdForList
                val spreadsheetId = currentSpreadsheetIdForList
                if (caseId != null && spreadsheetId != null) {
                    try {
                        ocrProcessingService.processImage(
                            uri = uri,
                            context = getApplication(),
                            caseId = caseId,
                            spreadsheetId = spreadsheetId,
                        )
                    } finally {
                        loadEvidenceForCase(caseId, spreadsheetId)
                    }
                }
            }
        }

        fun processAudioEvidence(uri: Uri) {
            viewModelScope.launch {
                if (currentCaseIdForList != null && currentSpreadsheetIdForList != null) {
                    val case = caseRepository.getCaseBySpreadsheetId(currentSpreadsheetIdForList!!)
                    if (case != null) {
                        val uploadResult = evidenceRepository.uploadFile(uri, case.name, case.spreadsheetId)
                        if (uploadResult is Result.Success) {
                            val transcribedText = transcriptionService.transcribeAudio(uri)

                            val newEvidence =
                                Evidence(
                                    id = 0,
                                    caseId = currentCaseIdForList!!,
                                    spreadsheetId = currentSpreadsheetIdForList!!,
                                    type = "audio",
                                    content = transcribedText,
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
                                loadEvidenceForCase(
                                    currentCaseIdForList!!,
                                    currentSpreadsheetIdForList!!,
                                )
                                _navigateToTranscriptionScreen.emit(newEvidenceWithId.id)
                            }
                        }
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
                evidenceRepository.updateTranscript(evidence, newTranscript, reason)
            }
        }

        fun processVideoEvidence(uri: Uri) {
            viewModelScope.launch {
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
                    }
                }
            }
        }
    }
