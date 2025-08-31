package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.GoogleApiService // Assuming this is Hilt-injected or via AuthViewModel
import com.hereliesaz.lexorcist.data.Case // For selected case interactions
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceDao
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.service.ScriptRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File // For java.io.File usage
import javax.inject.Inject

@HiltViewModel
class EvidenceViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context, // For file operations
    private val evidenceRepository: EvidenceRepository,
    private val evidenceDao: EvidenceDao, // As per error
    private val authViewModel: AuthViewModel, // Assuming this is a Hilt ViewModel
    private val caseViewModel: CaseViewModel, // Assuming this is a Hilt ViewModel
    private val ocrViewModel: OcrViewModel,   // Assuming this is a Hilt ViewModel
    private val googleApiService: GoogleApiService? // Injected, make non-nullable if always available
) : ViewModel() {

    private val tag = "EvidenceViewModel"

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()

    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    private val _taggedData = MutableStateFlow<Map<String, List<TaggedEvidence>>>(emptyMap())
    val taggedData: StateFlow<Map<String, List<TaggedEvidence>>> = _taggedData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val scriptRunner = ScriptRunner() // Consider injecting if it has dependencies
    private var currentScript: String = ""

    init {
        observeAuthChanges()
        observeCaseChanges()
        observeOcrChanges()
    }

    private fun observeAuthChanges() {
        viewModelScope.launch {
            authViewModel.isSignedIn.collect { isSignedIn ->
                if (!isSignedIn) {
                    clearEvidenceData()
                }
            }
        }
    }

    private fun observeCaseChanges() {
        viewModelScope.launch {
            caseViewModel.selectedCase.collect { case ->
                if (case != null) {
                    loadEvidenceForCase(case.id.toLong())
                } else {
                    _evidenceList.value = emptyList()
                    _uiEvidenceList.value = emptyList()
                }
            }
        }
    }
    
    private fun observeOcrChanges() {
        viewModelScope.launch {
            ocrViewModel.newlyCreatedEvidence.collect { evidence -> // Error still points here
                evidence?.let {
                    val currentCaseId = caseViewModel.selectedCase.value?.id?.toLong() ?: 0L
                    val evidenceWithCaseId = it.copy(caseId = currentCaseId)
                    _uiEvidenceList.value = _uiEvidenceList.value + evidenceWithCaseId
                }
            }
        }
    }

    private fun clearEvidenceData() {
        _evidenceList.value = emptyList()
        _uiEvidenceList.value = emptyList()
        _evidenceToEdit.value = null
        _taggedData.value = emptyMap()
    }

    fun clearError() { _errorMessage.value = null }
    fun prepareEvidenceForEditing(evidence: Evidence) { _evidenceToEdit.value = evidence }
    fun clearEvidenceToEdit() { _evidenceToEdit.value = null }

    fun loadEvidenceForCase(caseId: Long) {
        viewModelScope.launch {
            evidenceRepository.getEvidenceForCase(caseId).collect {
                _evidenceList.value = it
                _uiEvidenceList.value = it
            }
        }
    }
    
    fun addEvidence(evidence: Evidence) {
        viewModelScope.launch { evidenceRepository.addEvidence(evidence) }
    }

    fun updateEvidence(evidence: Evidence) {
        viewModelScope.launch { evidenceRepository.updateEvidence(evidence) }
    }

    fun deleteEvidence(evidence: Evidence) {
        viewModelScope.launch { evidenceRepository.deleteEvidence(evidence) }
    }

    // Corrected signature
    suspend fun getEvidenceById(id: Int): Evidence? {
        return evidenceRepository.getEvidence(id)
    }
    
    fun updateEvidenceCommentary(evidenceId: Int, commentary: String) {
        viewModelScope.launch {
            evidenceRepository.updateCommentary(evidenceId, commentary)
        }
    }

    fun addTextEvidenceToSelectedCase(text: String) {
        val caseId = caseViewModel.selectedCase.value?.id?.toLong() ?: run {
            _errorMessage.value = "No case selected to add text evidence."
            return
        }
        val newEvidence = Evidence(
            caseId = caseId,
            type = "text",
            content = text, // Corrected: data -> content
            timestamp = System.currentTimeMillis(),
            sourceDocument = "Manual Text Input",
            commentary = ""
        )
        addEvidence(newEvidence)
    }

    fun addUriContentToUiList(uri: Uri, mimeType: String?) {
        viewModelScope.launch {
            val caseIdForUi = caseViewModel.selectedCase.value?.id?.toLong() ?: 0L
            val parsedEvidence = when (mimeType) {
                "text/plain" -> parseTextFile(uri, caseIdForUi)
                "application/pdf" -> parsePdfFile(uri, caseIdForUi)
                else -> { _errorMessage.value = "Unsupported file type: $mimeType"; null }
            }
            parsedEvidence?.let {
                _uiEvidenceList.value = _uiEvidenceList.value + it
            }
        }
    }

    private fun parseTextFile(uri: Uri, caseId: Long): Evidence? {
        return try {
            applicationContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                // Corrected: data -> content
                Evidence(caseId = caseId, type = "text_file", content = text, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), commentary = "")
            }
        } catch (e: Exception) { _errorMessage.value = "Failed to parse text file"; null }
    }

    private fun parsePdfFile(uri: Uri, caseId: Long): Evidence? {
        return try {
            applicationContext.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = com.itextpdf.kernel.pdf.PdfReader(inputStream)
                val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(pdfReader)
                val textContent = buildString {
                    for (i in 1..pdfDocument.numberOfPages) {
                        try {
                            append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)))
                        } catch (e: Exception) {
                            Log.e(tag, "Error extracting text from page $i of PDF: $uri", e)
                        }
                    }
                }
                pdfDocument.close()
                // Corrected: data -> content
                Evidence(caseId = caseId, type = "pdf", content = textContent, timestamp = System.currentTimeMillis(), sourceDocument = uri.toString(), commentary = "")
            }
        } catch (e: Exception) { _errorMessage.value = "Failed to parse PDF file: ${e.message}"; Log.e(tag, "PDF Parsing error",e); null }
    }

    fun setScriptForProcessing(scriptText: String) { this.currentScript = scriptText }

    fun processUiEvidenceWithScript() {
        if (currentScript.isBlank()) {
            _errorMessage.value = "No script set for processing."
            return
        }
        viewModelScope.launch {
            val processedTaggedEvidence = _uiEvidenceList.value.mapNotNull { evidence ->
                try {
                    // Corrected: data -> content
                    val parserResult = scriptRunner.runScript(currentScript, evidence.content ?: "") 
                    // Corrected: data -> content
                    TaggedEvidence(id = evidence.id.toString(), caseId = evidence.caseId.toString(), tags = parserResult.tags, content = evidence.content ?: "")
                } catch (e: Exception) {
                    Log.e(tag, "Error running script on evidence id ${evidence.id}", e)
                    null
                }
            }
             _errorMessage.value = "Processing complete. Check logs for errors." // Placeholder for now
        }
    }
    
    fun uploadFileToDrive(caseForUpload: Case, file: File, mimeType: String) {
        viewModelScope.launch {
            if (googleApiService == null) { _errorMessage.value = "Google Api Service not available"; return@launch}
            val evidenceFolderId = googleApiService.getOrCreateEvidenceFolder(caseForUpload.name) ?: run {
                _errorMessage.value = "Could not get/create evidence folder in Drive."
                return@launch
            }
            val uploadedDriveFile = googleApiService.uploadFile(file, evidenceFolderId, mimeType)
            if (uploadedDriveFile != null) {
                val newEvidence = Evidence(
                    caseId = caseForUpload.id.toLong(),
                    type = mimeType,
                    content = "Uploaded to Drive: ${uploadedDriveFile.name}", // Corrected: data -> content
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uploadedDriveFile.id, 
                    commentary = ""
                )
                addEvidence(newEvidence)
            } else {
                _errorMessage.value = "Failed to upload file to Drive."
            }
        }
    }
}
