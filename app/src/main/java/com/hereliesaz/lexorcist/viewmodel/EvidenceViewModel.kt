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

class EvidenceViewModel(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val selectedCase: Case?
) : AndroidViewModel(application) {

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _uiEvidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val uiEvidenceList: StateFlow<List<Evidence>> = _uiEvidenceList.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        selectedCase?.let {
            loadEvidenceForCase(it.id.toLong()) // Corrected: Convert Int to Long
        }
    }

    fun addEvidenceToUiList(evidence: Evidence) {
        _uiEvidenceList.value = _uiEvidenceList.value + evidence
    }

    private fun loadEvidenceForCase(caseId: Long) {
        viewModelScope.launch {
            evidenceRepository.getEvidenceForCase(caseId).collect {
                _evidenceList.value = it
            }
        }
    }

    fun addEvidenceToUiList(uri: Uri, context: Context) {
        // This function needs more logic from MainViewModel, including parsing different file types.
        // For now, it's a placeholder.
    }

    fun addEvidenceToSelectedCase(evidence: Evidence) {
        selectedCase?.let {
            viewModelScope.launch {
                evidenceRepository.addEvidence(it.spreadsheetId, evidence)
            }
        }
    }

    fun updateEvidence(evidence: Evidence) {
        selectedCase?.let {
            viewModelScope.launch {
                evidenceRepository.updateEvidence(it.spreadsheetId, evidence)
            }
        }
    }

    fun deleteEvidence(evidence: Evidence) {
        selectedCase?.let {
            viewModelScope.launch {
                evidenceRepository.deleteEvidence(it.spreadsheetId, evidence)
            }
        }
    }

    fun addEvidenceToSelectedCase(text: String, context: Context) {
        selectedCase?.let { case ->
            val newEvidence = Evidence(
                id = 0, // Placeholder ID for new evidence, repository should handle actual ID generation
                caseId = case.id, // Use selectedCase.id
                content = text,
                // amount = null, // Removed amount
                timestamp = System.currentTimeMillis(), // Changed to Long
                sourceDocument = "Text Input",
                documentDate = System.currentTimeMillis(), // Changed to Long
                allegationId = null, // Int? is fine with null
                category = "Text Input", // String is fine
                tags = emptyList() // List<String> is fine
            )
            addEvidenceToSelectedCase(newEvidence)
        }    
    }

    /**
     * Extracts text from a PDF document and adds it as evidence to the selected case.
     *
     * @param uri The URI of the PDF document.
     * @param context The context.
     */
    fun addDocumentEvidence(uri: Uri, context: Context) {
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

                selectedCase?.let { case ->
                    val newEvidence = Evidence(
                        id = 0,
                        caseId = case.id,
                        content = text,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "PDF Document",
                        documentDate = System.currentTimeMillis(),
                        allegationId = null,
                        category = "Document",
                        tags = emptyList()
                    )
                    addEvidenceToSelectedCase(newEvidence)
                }
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
    fun addSpreadsheetEvidence(uri: Uri, context: Context) {
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

                    selectedCase?.let { case ->
                        val newEvidence = Evidence(
                            id = 0,
                            caseId = case.id,
                            content = text,
                            timestamp = System.currentTimeMillis(),
                            sourceDocument = "Spreadsheet",
                            documentDate = System.currentTimeMillis(),
                            allegationId = null,
                            category = "Spreadsheet",
                            tags = emptyList()
                        )
                        addEvidenceToSelectedCase(newEvidence)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to parse spreadsheet: ${e.message}"
            }
        }
    }
}
