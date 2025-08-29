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
}
