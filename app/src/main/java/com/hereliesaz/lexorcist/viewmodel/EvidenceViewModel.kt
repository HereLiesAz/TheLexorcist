package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EvidenceViewModel @Inject constructor(
    application: Application,
    private val evidenceRepository: EvidenceRepository
) : AndroidViewModel(application) {

    private val _selectedEvidenceDetails = MutableStateFlow<Evidence?>(null)
    val selectedEvidenceDetails: StateFlow<Evidence?> = _selectedEvidenceDetails.asStateFlow()

    fun addTextEvidence(text: String, caseId: Long, spreadsheetId: String) {
        viewModelScope.launch {
            val newEvidence = Evidence(
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
                entities = emptyMap()
            )
            evidenceRepository.addEvidence(newEvidence)
        }
    }

    fun loadEvidenceDetails(evidenceId: Int) {
        viewModelScope.launch {
            _selectedEvidenceDetails.value = evidenceRepository.getEvidenceById(evidenceId)
        }
    }

    fun updateCommentary(evidenceId: Int, commentary: String) {
        viewModelScope.launch {
            evidenceRepository.updateCommentary(evidenceId, commentary)
            // Optionally, refresh the loaded evidence details if the update doesn't reflect immediately
            // For example, by calling loadEvidenceDetails(evidenceId) again or by updating the specific field.
            // For now, we assume the repository or underlying data source handles the update efficiently.
            // If not, and the UI doesn't update, we might need to reload:
            // _selectedEvidenceDetails.value = evidenceRepository.getEvidenceById(evidenceId)
            // Or, more efficiently, update the existing object if the repository confirms success:
            val currentEvidence = _selectedEvidenceDetails.value
            if (currentEvidence != null && currentEvidence.id == evidenceId) {
                _selectedEvidenceDetails.value = currentEvidence.copy(commentary = commentary)
            }
        }
    }

    // Call this when the details screen is left to clear the state
    fun clearEvidenceDetails() {
        _selectedEvidenceDetails.value = null
    }

    // ... any other existing code in your EvidenceViewModel
}
