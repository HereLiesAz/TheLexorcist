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
) : AndroidViewModel(application) {

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
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
}
