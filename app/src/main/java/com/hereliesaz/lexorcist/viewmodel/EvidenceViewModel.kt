package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.TaggedEvidenceRepository
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.service.ScriptRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor

class EvidenceViewModel(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val authViewModel: AuthViewModel,
    private val caseViewModel: CaseViewModel,
    private val ocrViewModel: OcrViewModel
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
        observeCaseChanges()
        observeOcrChanges()
    }

    private fun observeAuthChanges() {
        viewModelScope.launch {
            authViewModel.signOutEvent.collect {
                clearEvidenceData()
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
                }
            }
        }
    }

    private fun observeOcrChanges() {
        viewModelScope.launch {
            ocrViewModel.newlyCreatedEvidence.collect { evidence ->
                evidence?.let {
                    val caseId = caseViewModel.selectedCase.value?.id
                    val evidenceWithCaseId = if (caseId != null) {
                        it.copy(caseId = caseId)
                    } else {
                        it
                    }
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

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadEvidenceForCase(caseId: Long) {
        viewModelScope.launch {
            evidenceRepository.getEvidenceForCase(caseId).collect {
                _evidenceList.value = it
            }
        }
    }

    fun addEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.addEvidence(evidence)
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

    fun addEvidenceToUiList(uri: Uri, context: Context) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") == true) {
                ocrViewModel.startImageReview(uri, context)
            } else {
                val evidence : Evidence? = when (mimeType) {
                    "text/plain" -> parseTextFile(uri, context)
                    "application/pdf" -> parsePdfFile(uri, context)
                    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri, context)
                    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, context)
                    else -> { Log.w(tag, "Unsupported file type: $mimeType for URI: $uri"); null }
                }
                evidence?.let {
                    _uiEvidenceList.value = _uiEvidenceList.value + it
                }
            }
        }
    }

    private fun parseTextFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(
                    id = 0,
                    caseId = caseViewModel.selectedCase.value?.id ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Text File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse text file", e); _errorMessage.value = "Failed to parse text file"; null }
    }

    private fun parsePdfFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)
                val text = buildString { for (i in 1..pdfDocument.numberOfPages) { append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))) } }
                pdfDocument.close()
                Evidence(
                    id = 0,
                    caseId = caseViewModel.selectedCase.value?.id ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "PDF File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse PDF file", e); _errorMessage.value = "Failed to parse PDF file"; null }
    }

    private fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val text = buildString { /* For now, content will be empty. Implement actual parsing later */ }
                workbook.close()
                Evidence(
                    id = 0,
                    caseId = caseViewModel.selectedCase.value?.id ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Spreadsheet File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse spreadsheet file", e); _errorMessage.value = "Failed to parse spreadsheet file"; null }
    }

    private fun parseDocFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (context.contentResolver.getType(uri) == "application/msword") {
                    WordExtractor(HWPFDocument(inputStream)).text
                } else {
                    XWPFWordExtractor(XWPFDocument(inputStream)).text
                }
                Evidence(
                    id = 0,
                    caseId = caseViewModel.selectedCase.value?.id ?: 0,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Document File",
                    tags = emptyList()
                )
            }
        } catch (e: Exception) { Log.e(tag, "Failed to parse document file", e); _errorMessage.value = "Failed to parse document file"; null }
    }

    fun importSmsMessages(context: Context) {
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
                                caseId = caseViewModel.selectedCase.value?.id ?: 0,
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

    fun exportToSheet() {
        viewModelScope.launch {
            authViewModel.googleApiService.value?.let { apiService ->
                val spreadsheetId = apiService.createSpreadsheet("Lexorcist Export")
                spreadsheetId?.let { id ->
                    val data = _uiEvidenceList.value.map { evidence -> listOf(evidence.content, evidence.sourceDocument, evidence.documentDate.toString()) }
                    apiService.appendData(id, "Sheet1", data)
                }
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

    fun addTextEvidence(text: String) {
        val caseId = caseViewModel.selectedCase.value?.id
        if (caseId == null) {
            val newEvidenceForUiList = Evidence(
                id = 0,
                caseId = 0,
                content = text,
                timestamp = System.currentTimeMillis(),
                sourceDocument = "Text Input (No Case)",
                documentDate = System.currentTimeMillis(),
                allegationId = null,
                category = "Local Text",
                tags = emptyList()
            )
            _uiEvidenceList.value = _uiEvidenceList.value + newEvidenceForUiList
            return
        }

        val newEvidence = Evidence(
            id = 0,
            caseId = caseId,
            content = text,
            timestamp = System.currentTimeMillis(),
            sourceDocument = "Text Input",
            documentDate = System.currentTimeMillis(),
            allegationId = null,
            category = "Text Input",
            tags = emptyList()
        )
        addEvidence(newEvidence)
    }

    fun prepareEvidenceForEditing(evidence: Evidence) {
        _evidenceToEdit.value = evidence
    }

    fun clearEvidenceToEdit() {
        _evidenceToEdit.value = null
    }
}
