package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.Evidence // Correct import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import com.hereliesaz.lexorcist.data.TaggedEvidenceRepository
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.service.ScriptRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject



import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.service.ScriptRunner
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor

import java.io.File
@HiltViewModel
class EvidenceViewModel @Inject constructor(
    private val evidenceRepository: EvidenceRepository,
    private val caseRepository: CaseRepository,
    private val application: Application,
    private val authViewModel: AuthViewModel,
    private val ocrViewModel: OcrViewModel,
    private val googleApiService: GoogleApiService?
) : AndroidViewModel(application) {


    private val tag = "EvidenceViewModel"

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()

    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData: StateFlow<Map<String, List<String>>> = _taggedData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val scriptRunner = ScriptRunner()
    private var script: String = ""

    init {
        observeAuthChanges()
        observeOcrChanges()
    }

    private fun observeAuthChanges() {
        viewModelScope.launch {
            authViewModel.signOutEvent.collect {
                clearEvidenceData()
            }
        }
    }

    private fun observeOcrChanges() {
        viewModelScope.launch {
            ocrViewModel.newlyCreatedEvidence.collect { evidence ->
                evidence?.let {
                    // This needs to be updated to get the selected case from the UI
                    // and then add the evidence to that case.
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

    fun clearError() {
        _errorMessage.value = null
    }

    fun prepareEvidenceForEditing(evidence: Evidence) {
        _evidenceToEdit.value = evidence
    }

    fun clearEvidenceToEdit() {
        _evidenceToEdit.value = null
    }

    fun loadEvidenceForCase(caseItem: Case) {
        viewModelScope.launch {
            evidenceRepository.setCaseSpreadsheetId(caseItem.spreadsheetId)
            caseItem.scriptId?.let { evidenceRepository.setCaseScriptId(it) }
            evidenceRepository.getEvidenceForCase(caseItem.id.toLong()).collect {
                _evidenceList.value = it
            }
        }
    }

    fun updateEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.updateEvidence(evidence)
        }
    }

    fun deleteEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.deleteEvidence(evidence)
        }
    }

    fun getEvidence(id: Int): Flow<Evidence> {
        return evidenceRepository.getEvidence(id)
    }

    fun linkEvidence(fromId: Int, toId: Int) {
        viewModelScope.launch {
            val fromEvidence = evidenceRepository.getEvidenceById(fromId)
            if (fromEvidence != null) {
                val updatedLinkedIds = fromEvidence.linkedEvidenceIds.toMutableList()
                if (!updatedLinkedIds.contains(toId)) {
                    updatedLinkedIds.add(toId)
                }
                val updatedEvidence = fromEvidence.copy(linkedEvidenceIds = updatedLinkedIds)
                evidenceRepository.updateEvidence(updatedEvidence)
            }
        }
    }

    fun addEvidenceToUiList(uri: Uri, context: Context, caseId: Int?) {
        viewModelScope.launch {
            val evidence: Evidence? = when (context.contentResolver.getType(uri)) {
                "text/plain" -> parseTextFile(uri, context, caseId)
                "application/pdf" -> parsePdfFile(uri, context, caseId)
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri, context, caseId)
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, context, caseId)
                else -> {
                    _errorMessage.value = "Unsupported file type"
                    null
                }
            }
            evidence?.let {
                _uiEvidenceList.value = _uiEvidenceList.value + it
            }
        }
    }

    private fun parseTextFile(uri: Uri, context: Context, caseId: Int?): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(
                    id = 0,
                    caseId = caseId ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Text File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse text file"
            null
        }
    }

    fun importSmsMessages(context: Context, caseId: Int?) {
        viewModelScope.launch {
            val smsList = mutableListOf<Evidence>()
            val cursor = context.contentResolver.query(
                android.provider.Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val bodyIndex = it.getColumnIndex(android.provider.Telephony.Sms.BODY)
                    val dateIndex = it.getColumnIndex(android.provider.Telephony.Sms.DATE)
                    do {
                        val body = it.getString(bodyIndex)
                        val dateMillis = it.getLong(dateIndex)
                        smsList.add(
                            Evidence(
                                id = 0,
                                caseId = caseId ?: 0,
                                content = body,
                                timestamp = dateMillis,
                                sourceDocument = "SMS Message",
                                documentDate = dateMillis,
                                allegationId = null,
                                category = "SMS",
                                tags = emptyList()
                            )
                        )
                    } while (it.moveToNext())
                }
            }
            _uiEvidenceList.value = _uiEvidenceList.value + smsList
        }
    }

    fun exportToSheet(spreadsheetId: String) {
        viewModelScope.launch {
            googleApiService?.let { apiService ->
                val data = _uiEvidenceList.value.map { evidence -> listOf(evidence.content, evidence.sourceDocument, evidence.documentDate.toString()) }
                apiService.appendData(spreadsheetId, "Sheet1", data)
            }
        }
    }

    fun addDriveFileEvidenceToSelectedCase(uri: Uri, context: Context, case: Case) {
        viewModelScope.launch {
            googleApiService?.let { apiService ->
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(case.name) ?: return@launch
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    val mimeType = context.contentResolver.getType(uri)
                    context.contentResolver.openInputStream(uri)?.let { inputStream ->
                        val file = File(context.cacheDir, fileName)
                        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        val uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, mimeType ?: "application/octet-stream")
                        if (uploadedDriveFile != null) {
                            val newEvidenceEntry = Evidence(
                                id = 0,
                                caseId = case.id,
                                content = "Uploaded file: $fileName (Content not extracted for preview)",
                                timestamp = System.currentTimeMillis(),
                                sourceDocument = uploadedDriveFile.name ?: fileName,
                                documentDate = System.currentTimeMillis(),
                                allegationId = null,
                                category = mimeType ?: "file",
                                tags = listOf("drive_upload")
                            )
                            addEvidence(newEvidenceEntry)
                        } else {
                            _errorMessage.value = "Failed to upload file to Drive."
                        }
                    }
                }
            }
        }
    }

    fun addTextEvidenceToSelectedCase(text: String, context: Context, case: Case) {
        viewModelScope.launch {
            googleApiService?.let { apiService ->
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder(case.name) ?: return@launch
                val timestamp = System.currentTimeMillis()
                val file = File(context.cacheDir, "text-evidence-$timestamp.txt")
                file.writeText(text)
                val uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, "text/plain")
                if (uploadedDriveFile != null) {
                    val newEvidence = Evidence(
                        id = 0,
                        caseId = case.id,
                        content = text,
                        timestamp = timestamp,
                        sourceDocument = uploadedDriveFile.name ?: "Uploaded Text Evidence",
                        documentDate = timestamp,
                        allegationId = null,
                        category = "Text Upload",
                        tags = emptyList()
                    )
                    addEvidence(newEvidence)
                } else {
                    _errorMessage.value = "Failed to upload text evidence."
                }
            }
        }
    }

    private fun parsePdfFile(uri: Uri, context: Context, caseId: Int?): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = com.itextpdf.kernel.pdf.PdfReader(inputStream)
                val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(pdfReader)
                val text = buildString { for (i in 1..pdfDocument.numberOfPages) { append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))) } }
                pdfDocument.close()
                Evidence(
                    id = 0,
                    caseId = caseId ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "PDF File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse PDF file"
            null
        }
    }

    private fun parseSpreadsheetFile(uri: Uri, context: Context, caseId: Int?): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)
                val text = buildString { /* Implement parsing later */ }
                workbook.close()
                Evidence(
                    id = 0,
                    caseId = caseId ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Spreadsheet File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse spreadsheet file"
            null
        }
    }

    private fun parseDocFile(uri: Uri, context: Context, caseId: Int?): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (context.contentResolver.getType(uri) == "application/msword") {
                    org.apache.poi.hwpf.extractor.WordExtractor(org.apache.poi.hwpf.HWPFDocument(inputStream)).text
                } else {
                    org.apache.poi.xwpf.extractor.XWPFWordExtractor(org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)).text
                }
                Evidence(
                    id = 0,
                    caseId = caseId ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Document File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse document file"
            null
        }
    }

    fun addTextEvidence(caseId: Int, text: String) {
        viewModelScope.launch {
            evidenceRepository.addEvidence(
                caseId = caseId,
                content = text,
                sourceDocument = "Text Input",
                category = "Text Input",
                allegationId = null
            )
        }
    }

    fun addAudioEvidence(caseId: Int, transcript: String): Evidence {
        val newEvidence = Evidence(
            id = 0,
            caseId = caseId,
            content = transcript,
            timestamp = System.currentTimeMillis(),
            sourceDocument = "Audio Recording",
            documentDate = System.currentTimeMillis(),
            allegationId = null,
            category = "Audio",
            tags = emptyList()
        )
        addEvidence(newEvidence)
        return newEvidence
    }

    fun addDocumentEvidence(caseId: Int, uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val pdfReader = com.itextpdf.kernel.pdf.PdfReader(inputStream)
                val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(pdfReader)
                var text = ""
                for (i in 1..pdfDocument.numberOfPages) {
                    val page = pdfDocument.getPage(i)
                    text += com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page)
                }
                pdfDocument.close()
                evidenceRepository.addEvidence(
                    caseId = caseId,
                    content = text,
                    sourceDocument = "PDF Document",
                    category = "Document",
                    allegationId = null
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to parse document: ${e.message}"
            }
        }
    }

    fun setScript(scriptText: String) { this.script = scriptText }

    fun processUiEvidenceForReview() {
        viewModelScope.launch {
            val taggedList = _uiEvidenceList.value.map { evidence ->
                val parserResult = scriptRunner.runScript(script, evidence)
                TaggedEvidence(id = evidence, tags = parserResult.tags, content = evidence.content)
            }
            TaggedEvidenceRepository.setTaggedEvidence(taggedList)
        }
    }

    fun addTextEvidence(text: String, caseId: Int) {
        evidenceRepository.addEvidence(
            caseId = caseId,
            content = text,
            sourceDocument = "Spreadsheet",
            category = "Spreadsheet",
            allegationId = null
        )
    }

    fun addEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.addEvidence(evidence)
        }
    }
}
