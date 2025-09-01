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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EvidenceViewModel @Inject constructor(
    application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val settingsManager: com.hereliesaz.lexorcist.data.SettingsManager,
    private val scriptRunner: com.hereliesaz.lexorcist.service.ScriptRunner
) : AndroidViewModel(application) {

    private val _selectedEvidenceDetails = MutableStateFlow<Evidence?>(null)
    val selectedEvidenceDetails: StateFlow<Evidence?> = _selectedEvidenceDetails.asStateFlow()

    private val _evidenceList = MutableStateFlow<List<Evidence>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val evidenceList: StateFlow<List<Evidence>> = _evidenceList
        .combine(_searchQuery) { evidence, query ->
            if (query.isBlank()) {
                evidence
            } else {
                evidence.filter { it.content.contains(query, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentCaseIdForList: Long? = null
    private var currentSpreadsheetIdForList: String? = null

    fun addTextEvidence(text: String, caseId: Long, spreadsheetId: String) {
        viewModelScope.launch {
            val entities = com.hereliesaz.lexorcist.DataParser.tagData(text)
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
                entities = entities
            )
            evidenceRepository.addEvidence(newEvidence)
            // Refresh list if it's for the same case
            if (caseId == currentCaseIdForList && spreadsheetId == currentSpreadsheetIdForList) {
                loadEvidenceForCase(caseId, spreadsheetId)
            }
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
            val currentDetails = _selectedEvidenceDetails.value
            if (currentDetails != null && currentDetails.id == evidenceId) {
                _selectedEvidenceDetails.value = currentDetails.copy(commentary = commentary)
            }
            // Refresh list if the updated item is in the current list
            currentCaseIdForList?.let { caseId ->
                currentSpreadsheetIdForList?.let { spreadsheetId ->
                    loadEvidenceForCase(caseId, spreadsheetId)
                }
            }
        }
    }

    fun clearEvidenceDetails() {
        _selectedEvidenceDetails.value = null
    }

    fun loadEvidenceForCase(caseId: Long, spreadsheetId: String) {
        currentCaseIdForList = caseId
        currentSpreadsheetIdForList = spreadsheetId
        viewModelScope.launch {
            _isLoading.value = true
            evidenceRepository.getEvidenceForCase(spreadsheetId, caseId).collectLatest {
                _evidenceList.value = it
            }
            _isLoading.value = false
        }
    }

    fun updateEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.updateEvidence(evidence)
            val script = settingsManager.getScript()
            val result = scriptRunner.runScript(script, evidence)
            if (result is com.hereliesaz.lexorcist.utils.Result.Success) {
                val updatedEvidence = evidence.copy(tags = evidence.tags + result.data.tags)
                evidenceRepository.updateEvidence(updatedEvidence)
            }
            // Refresh list if it's for the same case
            if (evidence.caseId == currentCaseIdForList && evidence.spreadsheetId == currentSpreadsheetIdForList) {
                loadEvidenceForCase(evidence.caseId, evidence.spreadsheetId)
            }
        }
    }

    fun deleteEvidence(evidence: Evidence) {
        viewModelScope.launch {
            evidenceRepository.deleteEvidence(evidence)
            // Refresh list if it's for the same case
            if (evidence.caseId == currentCaseIdForList && evidence.spreadsheetId == currentSpreadsheetIdForList) {
                loadEvidenceForCase(evidence.caseId, evidence.spreadsheetId)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
