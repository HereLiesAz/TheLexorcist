package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// import com.hereliesaz.lexorcist.GoogleApiService // Already injected as directGoogleApiService
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.TaggedEvidenceRepository // Import for static usage
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import com.itextpdf.kernel.pdf.PdfReader as ITextPdfReader
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EvidenceViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val evidenceRepository: EvidenceRepository,
    private val caseRepository: CaseRepository, // Assuming needed
    private val authViewModel: AuthViewModel,
    private val ocrViewModel: OcrViewModel,
    private val directGoogleApiService: com.hereliesaz.lexorcist.GoogleApiService? // Fully qualified to be safe
    // For CaseViewModel interaction to get selected case:
    // private val caseViewModel: CaseViewModel // Uncomment if CaseViewModel is created and Hilt-injectable
) : AndroidViewModel(appContext as Application) {

    private val tag = "EvidenceViewModel"

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _uiStagedEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiStagedEvidenceList: StateFlow<List<Evidence>> = _uiStagedEvidenceList.asStateFlow()

    private val _evidenceToEdit = MutableStateFlow<Evidence?>(null)
    val evidenceToEdit: StateFlow<Evidence?> = _evidenceToEdit.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentSelectedCase = MutableStateFlow<Case?>(null)
    val currentSelectedCase: StateFlow<Case?> = _currentSelectedCase.asStateFlow()

    private val localScriptRunner = ScriptRunner()

    init {
        observeAuthChanges()
        observeOcrCreatedEvidence() // Renamed for clarity
        observeGoogleApiServiceChanges()
    }

    private fun observeGoogleApiServiceChanges() {
        viewModelScope.launch {
            authViewModel.currentGoogleApiService.collect { service ->
                Log.d(tag, "GoogleApiService updated in EvidenceViewModel: ${service != null}")
            }
        }
    }

    private fun observeAuthChanges() {
        viewModelScope.launch {
            authViewModel.signOutEvent.collect {
                clearEvidenceData()
                _currentSelectedCase.value = null
            }
        }
    }

    private fun observeOcrCreatedEvidence() {
        viewModelScope.launch {
            try {
                ocrViewModel.newlyCreatedEvidence.collect { ocrGeneratedEvidence ->
                    val selectedCase = _currentSelectedCase.value
                    if (selectedCase != null) {
                        val evidenceWithCaseId = ocrGeneratedEvidence.copy(spreadsheetId = selectedCase.spreadsheetId)
                        addEvidence(evidenceWithCaseId)
                        Log.i(tag, "OCR evidence added to case ${selectedCase.id}: ${evidenceWithCaseId.content.take(30)}")
                    } else {
                        Log.w(tag, "OCR evidence created but no case currently selected: ${ocrGeneratedEvidence.content.take(50)}")
                        _errorMessage.value = "OCR complete. Select or create a case to save this evidence."
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "CRITICAL: Error observing OcrViewModel.newlyCreatedEvidence. This might be a Hilt setup or build issue.", e)
                _errorMessage.value = "Error processing OCR results. Please try again."
            }
        }
    }

    private fun clearEvidenceData() {
        _evidenceList.value = emptyList()
        _uiStagedEvidenceList.value = emptyList()
        _evidenceToEdit.value = null
    }

    fun clearError() { _errorMessage.value = null }
    fun prepareEvidenceForEditing(evidence: Evidence) { _evidenceToEdit.value = evidence }
    fun clearEvidenceToEdit() { _evidenceToEdit.value = null }

    fun loadEvidenceForCase(selectedCase: Case) {
        _currentSelectedCase.value = selectedCase
        viewModelScope.launch {
            evidenceRepository.getEvidenceForCase(
                spreadsheetId = selectedCase.spreadsheetId,
                caseId = selectedCase.id.toLong()
            ).collect {
                _evidenceList.value = it
            }
        }
    }

    fun getEvidence(id: Int): Flow<Evidence?> {
        return evidenceRepository.getEvidence(id)
    }

    suspend fun getEvidenceById(id: Int): Evidence? {
        return evidenceRepository.getEvidenceById(id)
    }

    fun linkEvidence(fromId: Int, toId: Int) {
        viewModelScope.launch {
            val fromEvidence = getEvidenceById(fromId)
            if (fromEvidence != null) {
                val updatedLinkedIds = fromEvidence.linkedEvidenceIds.toMutableList()
                if (!updatedLinkedIds.contains(toId)) { updatedLinkedIds.add(toId) }
                updateEvidence(fromEvidence.copy(linkedEvidenceIds = updatedLinkedIds))
            }
        }
    }

    fun addEvidence(evidence: Evidence) {
        viewModelScope.launch {
            try {
                evidenceRepository.addEvidence(evidence)
                 _currentSelectedCase.value?.let { loadEvidenceForCase(it) }
            } catch (e: Exception) {
                Log.e(tag, "Error adding evidence", e)
                _errorMessage.value = "Failed to save evidence: ${e.message}"
            }
        }
    }

    fun updateEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.updateEvidence(evidence)
            _currentSelectedCase.value?.let { loadEvidenceForCase(it) }
        }
    }

    fun deleteEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.deleteEvidence(evidence)
            _currentSelectedCase.value?.let { loadEvidenceForCase(it) }
        }
    }

    fun addTextEvidence(text: String) {
        val selectedCase = _currentSelectedCase.value
        if (selectedCase != null) {
            addTextEvidenceToSelectedCase(text, selectedCase)
        } else {
            _errorMessage.value = "Cannot add text evidence: No case selected."
            Log.w(tag, "addTextEvidence called but no case is selected.")
        }
    }

    fun addTextEvidenceToSelectedCase(text: String, selectedCase: Case) {
        val newEvidence = Evidence(
            id = 0,
            spreadsheetId = selectedCase.spreadsheetId,
            content = text,
            timestamp = System.currentTimeMillis(),
            sourceDocument = "Manual Text Input",
            documentDate = System.currentTimeMillis(),
            allegationId = null,
            category = "Text Note",
            tags = emptyList()
        )
        addEvidence(newEvidence)
    }

    fun addDriveFileEvidenceToSelectedCase(uri: Uri, selectedCase: Case) {
        viewModelScope.launch {
            val service = authViewModel.currentGoogleApiService.value ?: run {
                _errorMessage.value = "Not signed in or Google Service not available."
                return@launch
            }
            val rawEvidenceFolderId = service.getOrCreateEvidenceFolder(selectedCase.name) ?: run {
                _errorMessage.value = "Could not get/create evidence folder in Drive."
                return@launch
            }
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                val fileName = if (nameIndex != -1) cursor.getString(nameIndex) else "unknown_drive_file"
                val mimeType = appContext.contentResolver.getType(uri)
                try {
                    appContext.contentResolver.openInputStream(uri)?.let { inputStream ->
                        val tempFile = File(appContext.cacheDir, fileName)
                        tempFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        val uploadResult = service.uploadFile(tempFile, rawEvidenceFolderId, mimeType ?: "application/octet-stream")
                        tempFile.delete()
                        when (uploadResult) {
                            is Result.Success -> {
                                val uploadedDriveFile = uploadResult.data
                                addEvidence(Evidence(
                                    id = 0,
                                    spreadsheetId = selectedCase.spreadsheetId,
                                    content = "Uploaded Drive file: $fileName. Link: ${uploadedDriveFile.webViewLink}",
                                    sourceDocument = uploadedDriveFile.id ?: fileName,
                                    documentDate = System.currentTimeMillis(),
                                    allegationId = null,
                                    category = mimeType ?: "Drive File",
                                    tags = listOf("drive_upload"),
                                    timestamp = System.currentTimeMillis()
                                ))
                            }
                            is Result.Error -> {
                                _errorMessage.value = "Failed to upload file to Google Drive: ${uploadResult.exception.message}"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error uploading Drive file", e)
                    _errorMessage.value = "Error uploading file: ${e.message}"
                }
            }
        }
    }

    fun stageFileForEvidence(uri: Uri, caseIdForContext: Long? = _currentSelectedCase.value?.id?.toLong()) {
        viewModelScope.launch {
            val mimeType = appContext.contentResolver.getType(uri)
            val evidence = when (mimeType) {
                "text/plain" -> parseTextFile(uri)
                "application/pdf" -> parsePdfFile(uri)
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> parseSpreadsheetFile(uri)
                "application/msword" -> parseDocFile(uri, true)
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocFile(uri, false)
                else -> { _errorMessage.value = "Unsupported file type: $mimeType"; null }
            }
            evidence?.let { _uiStagedEvidenceList.value = _uiStagedEvidenceList.value + it }
        }
    }

    private fun parseTextFile(uri: Uri): Evidence? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                val text = it.readText()
                Evidence(
                    id = 0,
                    spreadsheetId = _currentSelectedCase.value?.spreadsheetId ?: "",
                    content = text,
                    sourceDocument = uri.toString(),
                    category = "Text File Import",
                    timestamp = System.currentTimeMillis(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse text file: ${e.message}"
            Log.e(tag, "parseTextFile failed for $uri", e)
            null
        }
    }

    private fun parsePdfFile(uri: Uri): Evidence? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.let { inputStream ->
                val pdfReader = ITextPdfReader(inputStream)
                val pdfDocument = ITextPdfDocument(pdfReader)
                val text = buildString {
                    for (i in 1..pdfDocument.numberOfPages) {
                        try {
                           append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)))
                        } catch (pageEx: Exception) {
                           Log.e(tag, "Error extracting text from PDF page $i of $uri", pageEx)
                           append("\n[Error extracting page $i]\n")
                        }
                    }
                }
                pdfDocument.close()
                Evidence(
                    id = 0,
                    spreadsheetId = _currentSelectedCase.value?.spreadsheetId ?: "",
                    content = text,
                    sourceDocument = uri.toString(),
                    category = "PDF Import",
                    timestamp = System.currentTimeMillis(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse PDF file: ${e.message}"
            Log.e(tag, "parsePdfFile failed for $uri", e)
            null
        }
    }

    private fun parseSpreadsheetFile(uri: Uri): Evidence? {
         return try {
            appContext.contentResolver.openInputStream(uri)?.let { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
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
                    stringBuilder.append("\n")
                }
                workbook.close()
                Evidence(
                    id = 0,
                    spreadsheetId = _currentSelectedCase.value?.spreadsheetId ?: "",
                    content = stringBuilder.toString(),
                    sourceDocument = uri.toString(),
                    category = "Spreadsheet Import",
                    timestamp = System.currentTimeMillis(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse spreadsheet file: ${e.message}"
            Log.e(tag, "parseSpreadsheetFile failed for $uri", e)
            null
        }
    }

    private fun parseDocFile(uri: Uri, isLegacyDoc: Boolean): Evidence? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.let { inputStream ->
                val text = if (isLegacyDoc) {
                    WordExtractor(HWPFDocument(inputStream)).text
                } else {
                    XWPFWordExtractor(XWPFDocument(inputStream)).text
                }
                Evidence(
                    id = 0,
                    spreadsheetId = _currentSelectedCase.value?.spreadsheetId ?: "",
                    content = text,
                    sourceDocument = uri.toString(),
                    category = if (isLegacyDoc) "Word Document (DOC)" else "Word Document (DOCX)",
                    timestamp = System.currentTimeMillis(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    tags = emptyList()
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to parse document file: ${e.message}"
            Log.e(tag, "parseDocFile failed for $uri", e)
            null
        }
    }

    fun importSmsMessages() {
        viewModelScope.launch {
            val smsList = mutableListOf<Evidence>()
            try {
                val cursor = appContext.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    null, null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                        val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                        do {
                            val body = it.getString(bodyIndex)
                            val dateMillis = it.getLong(dateIndex)
                            smsList.add(
                                Evidence(
                                    id = 0,
                                    spreadsheetId = _currentSelectedCase.value?.spreadsheetId ?: "",
                                    content = body,
                                    timestamp = dateMillis,
                                    sourceDocument = "SMS Import",
                                    documentDate = dateMillis,
                                    category = "SMS",
                                    allegationId = null,
                                    tags = emptyList()
                                )
                            )
                        } while (it.moveToNext())
                    }
                }
                _uiStagedEvidenceList.value = _uiStagedEvidenceList.value + smsList
            } catch (e: Exception) {
                Log.e(tag, "Error importing SMS messages", e)
                _errorMessage.value = "Failed to import SMS: ${e.message}. Check READ_SMS permission."
            }
        }
    }

    fun exportToSheet(spreadsheetId: String, sheetName: String) {
        viewModelScope.launch {
            val service = authViewModel.currentGoogleApiService.value
            if (service == null) {
                 _errorMessage.value = "Google Account not signed in or service unavailable."
                return@launch
            }
            try {
                val dataToExport = _evidenceList.value.map { ev ->
                    listOf(ev.id.toString(), ev.content.take(100), ev.timestamp.toString())
                }
                if (dataToExport.isNotEmpty()) {
                    service.appendData(spreadsheetId, sheetName, dataToExport)
                } else {
                    _errorMessage.value = "No evidence to export."
                }
            } catch (e: Exception) {
                Log.e(tag, "Error exporting to sheet", e)
                _errorMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun processUiStagedEvidenceForReview(scriptToRun: String) {
        viewModelScope.launch {
            val taggedList = _uiStagedEvidenceList.value.map { evidence ->
                val parserResult = localScriptRunner.runScript(scriptToRun, evidence)
                TaggedEvidence(evidence = evidence, tags = parserResult.tags, content = evidence.content)
            }
            TaggedEvidenceRepository.setTaggedEvidence(taggedList)
            Log.d(tag, "Processed ${taggedList.size} staged evidence items for review. Set in TaggedEvidenceRepository.")
        }
    }
}
