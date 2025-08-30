package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.Evidence // Correct import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// import java.util.Date // Removed import

import com.hereliesaz.lexorcist.GoogleApiService
import java.io.File

class EvidenceViewModel(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val googleApiService: GoogleApiService?
) : AndroidViewModel(application) {

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()

    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun prepareEvidenceForEditing(evidence: Evidence) {
        _evidenceToEdit.value = evidence
    }

    fun clearEvidenceToEdit() {
        _evidenceToEdit.value = null
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

    fun exportToSheet() {
        viewModelScope.launch {
            googleApiService?.let { apiService ->
                val spreadsheetId = apiService.createSpreadsheet("Lexorcist Export")
                spreadsheetId?.let { id ->
                    val data = _uiEvidenceList.value.map { evidence -> listOf(evidence.content, evidence.sourceDocument, evidence.documentDate.toString()) }
                    apiService.appendData(id, "Sheet1", data)
                }
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

                val newEvidence = Evidence(
                    id = 0,
                    caseId = caseId,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = "PDF Document",
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Document",
                    tags = emptyList()
                )
                addEvidence(newEvidence)
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

                    val newEvidence = Evidence(
                        id = 0,
                        caseId = caseId,
                        content = text,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Spreadsheet",
                        documentDate = System.currentTimeMillis(),
                        allegationId = null,
                        category = "Spreadsheet",
                        tags = emptyList()
                    )
                    addEvidence(newEvidence)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to parse spreadsheet: ${e.message}"
            }
        }
    }
}
