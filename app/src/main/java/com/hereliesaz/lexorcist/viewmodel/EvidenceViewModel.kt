package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.Evidence // Correct import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class EvidenceViewModel @Inject constructor(
    private val evidenceRepository: EvidenceRepository,
    private val application: Application
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.service.ScriptRunner

class EvidenceViewModel(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val authViewModel: AuthViewModel
) : AndroidViewModel(application) {

    private var googleApiService: GoogleApiService? = null
    private val scriptRunner = ScriptRunner()

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()

    private val _taggedData = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val taggedData: StateFlow<Map<String, List<String>>> = _taggedData.asStateFlow()

    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        viewModelScope.launch {
            authViewModel.isSignedIn.collect { isSignedIn ->
                if (isSignedIn) {
                    googleApiService = authViewModel.googleApiService.value
                } else {
                    googleApiService = null
                    _evidenceList.value = emptyList()
                    _uiEvidenceList.value = emptyList()
                    _taggedData.value = emptyMap()
                    _evidenceToEdit.value = null
                }
            }
        }
    }

    fun loadEvidenceForCase(caseId: Long) {
        viewModelScope.launch {
            val caseItem = caseRepository.getCaseById(caseId.toInt())
            if (caseItem != null) {
                evidenceRepository.setCaseSpreadsheetId(caseItem.spreadsheetId)
                caseItem.scriptId?.let { evidenceRepository.setCaseScriptId(it) }
                evidenceRepository.getEvidenceForCase(caseId).collect {
                    _evidenceList.value = it
                }
            } else {
                _errorMessage.value = "Case not found"
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

    /**
     * Extracts text from a PDF document and adds it as evidence to the selected case.
     *
     * @param uri The URI of the PDF document.
     * @param context The context.
     */
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

    /**
     * Extracts text from a spreadsheet and adds it as evidence to the selected case.
     *
     * @param uri The URI of the spreadsheet.
     * @param context The context.
     */
    fun addSpreadsheetEvidence(caseId: Int, uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)
                    val stringBuilder = StringBuilder()
                    for (i in 0 until workbook.numberOfSheets) {
                        val sheet = workbook.getSheetAt(i)
                        stringBuilder.append("Sheet: ${sheet.sheetName}\n")
                        for (row in sheet) {
                            for (cell in row) {
                                stringBuilder.append(cell.toString()).append("\t")
                            }
                            stringBuilder.append("\n")
                        }
                    }
                    workbook.close()
                    val text = stringBuilder.toString()

                    evidenceRepository.addEvidence(
                        caseId = caseId,
                        content = text,
                        sourceDocument = "Spreadsheet",
                        category = "Spreadsheet",
                        allegationId = null
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to parse spreadsheet: ${e.message}"
            }
        }
    }

    fun addEvidenceToUiList(uri: Uri, context: Context) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            val evidence: Evidence? = when (mimeType) {
                "text/plain" -> parseTextFile(uri, context)
                "application/pdf" -> parsePdfFile(uri, context)
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri, context)
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, context)
                else -> {
                    _errorMessage.value = "Unsupported file type: $mimeType for URI: $uri"
                    null
                }
            }
            evidence?.let {
                _uiEvidenceList.value = _uiEvidenceList.value + it
            }
        }
    }

    private fun parseTextFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = 0,
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

    private fun parsePdfFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = com.itextpdf.kernel.pdf.PdfReader(inputStream)
                val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(pdfReader)
                var text = ""
                for (i in 1..pdfDocument.numberOfPages) {
                    val page = pdfDocument.getPage(i)
                    text += com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page)
                }
                pdfDocument.close()
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = 0,
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

    private fun parseSpreadsheetFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)
                val stringBuilder = StringBuilder()
                for (i in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(i)
                    stringBuilder.append("Sheet: ${sheet.sheetName}\n")
                    for (row in sheet) {
                        for (cell in row) {
                            stringBuilder.append(cell.toString()).append("\t")
                        }
                        stringBuilder.append("\n")
                    }
                }
                workbook.close()
                val text = stringBuilder.toString()
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = 0,
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

    private fun parseDocFile(uri: Uri, context: Context): Evidence? {
        return try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (context.contentResolver.getType(uri) == "application/msword") {
                    org.apache.poi.hwpf.extractor.WordExtractor(org.apache.poi.hwpf.HWPFDocument(inputStream)).text
                } else {
                    org.apache.poi.xwpf.extractor.XWPFWordExtractor(org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)).text
                }
                Evidence(
                    id = 0, // Placeholder ID
                    caseId = 0,
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
                                id = 0, // Placeholder ID
                                caseId = 0,
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
            googleApiService?.let { apiService ->
                val spreadsheetId = apiService.createSpreadsheet("Lexorcist Export")
                spreadsheetId?.let { id ->
                    val data = _uiEvidenceList.value.map { evidence -> listOf(evidence.content, evidence.sourceDocument, evidence.documentDate.toString()) } // Example data
                    apiService.appendData(id, "Sheet1", data)
                }
            }
        }
    }

    fun processUiEvidenceForReview(script: String) {
        viewModelScope.launch {
            val taggedList = _uiEvidenceList.value.map { evidence ->
                val parserResult = scriptRunner.runScript(script, evidence)
                com.hereliesaz.lexorcist.model.TaggedEvidence(id = evidence, tags = parserResult.tags, content = evidence.content)
            }
            com.hereliesaz.lexorcist.data.TaggedEvidenceRepository.setTaggedEvidence(taggedList)
        }
    }

    fun addTextEvidenceToSelectedCase(caseId: Int, text: String, context: Context) {
        viewModelScope.launch {
            googleApiService?.let { apiService ->
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder("Case $caseId") ?: return@launch
                val timestamp = System.currentTimeMillis()
                val file = java.io.File(context.cacheDir, "text-evidence-$timestamp.txt")
                file.writeText(text)
                val uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, "text/plain")
                if (uploadedDriveFile != null) {
                    val newEvidence = Evidence(
                        id = 0, // DB will assign ID
                        caseId = caseId,
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

    fun storeTaggedDataToSheet(caseId: Int, newTaggedData: Map<String, List<String>>) {
        viewModelScope.launch {
            googleApiService?.let { apiService ->
                // This needs a selected case to get the spreadsheetId
                // This logic needs to be revisited
            }
        }
    }

    fun addDriveFileEvidenceToSelectedCase(caseId: Int, uri: Uri, context: Context) {
        viewModelScope.launch {
            googleApiService?.let { apiService ->
                val rawEvidenceFolderId = apiService.getOrCreateEvidenceFolder("Case $caseId") ?: return@launch
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val fileName = cursor.getString(nameIndex)
                    val mimeType = context.contentResolver.getType(uri)
                    context.contentResolver.openInputStream(uri)?.let { inputStream ->
                        val file = java.io.File(context.cacheDir, fileName)
                        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        val uploadedDriveFile = apiService.uploadFile(file, rawEvidenceFolderId, mimeType ?: "application/octet-stream")
                        if (uploadedDriveFile != null) {
                            val newEvidenceEntry = Evidence(
                                id = 0, // DB will assign ID
                                caseId = caseId,
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

    fun prepareEvidenceForEditing(evidence: Evidence) {
        _evidenceToEdit.value = evidence
    }

    fun clearEvidenceToEdit() {
        _evidenceToEdit.value = null
    }
}
