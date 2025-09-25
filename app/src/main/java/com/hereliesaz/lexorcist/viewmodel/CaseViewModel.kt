package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseSheetParser
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.TimelineEvent
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val TAG = "CaseViewModel"

@HiltViewModel
class CaseViewModel @Inject constructor(
    private val googleApiService: GoogleApiService,
    private val application: Application,
) : ViewModel() {
    private val _cases = MutableStateFlow<List<Case>>(emptyList())
    val cases: StateFlow<List<Case>> = _cases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentCase = MutableStateFlow<Case?>(null)
    val currentCase: StateFlow<Case?> = _currentCase.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isUserRecoverableError = MutableStateFlow<com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException?>(null)
    val isUserRecoverableError: StateFlow<com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException?> = _isUserRecoverableError.asStateFlow()

    val userRecoverableAuthIntent: StateFlow<Intent?> = isUserRecoverableError.map { it?.intent }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun showError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }

    fun clearCache() {
        _cases.value = emptyList()
        _currentCase.value = null
        _evidence.value = emptyList()
        _allegations.value = emptyList()
    }

    fun clearUserRecoverableAuthIntent() {
        _isUserRecoverableError.value = null
    }

    fun loadCasesFromRepository() {
        loadCases()
    }

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TIMESTAMP_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _evidence = MutableStateFlow<List<Evidence>>(emptyList())

    val evidence: StateFlow<List<Evidence>> = combine(_evidence, _sortOrder) { evidence, sortOrder ->
        when (sortOrder) {
            SortOrder.TIMESTAMP_ASC -> evidence.sortedBy { it.timestamp }
            SortOrder.TIMESTAMP_DESC -> evidence.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations.asStateFlow()

    private val _selectedAllegations = MutableStateFlow<Set<String>>(emptySet())
    val selectedAllegations: StateFlow<Set<String>> = _selectedAllegations.asStateFlow()

    val filteredEvidence: StateFlow<List<Evidence>> = combine(evidence, selectedAllegations) { allEvidence, selected ->
        if (selected.isEmpty()) {
            allEvidence
        } else {
            allEvidence.filter { it.allegationId in selected }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _timelineEvents = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val timelineEvents: StateFlow<List<TimelineEvent>> = _timelineEvents.asStateFlow()

    val uiState: StateFlow<CaseScreenUiState> = combine(
        currentCase, isLoading, error
    ) { case, loading, errorMsg ->
        CaseScreenUiState(case, loading, errorMsg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CaseScreenUiState())

    val caseDetailState: StateFlow<CaseDetailState> = combine(
        currentCase, evidence, allegations, isLoading, error
    ) { case, evidence, allegations, loading, errorMsg ->
        CaseDetailState(case, evidence, allegations, loading, errorMsg)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CaseDetailState()
    )

    val reviewScreenState: StateFlow<ReviewScreenState> = combine(
        evidence, allegations, selectedAllegations, isLoading, error
    ) { evidence, allegations, selected, loading, errorMsg ->
        ReviewScreenState(evidence, allegations, selected, loading, errorMsg)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReviewScreenState()
    )

    val timelineScreenState: StateFlow<TimelineScreenState> = combine(
        timelineEvents, isLoading, error
    ) { events, loading, errorMsg ->
        TimelineScreenState(events, loading, errorMsg)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TimelineScreenState()
    )

    private val _selectedEvidence = MutableStateFlow<Evidence?>(null)
    val selectedEvidence: StateFlow<Evidence?> = _selectedEvidence.asStateFlow()

    private val _exhibits = MutableStateFlow<List<Evidence>>(emptyList())
    val exhibits: StateFlow<List<Evidence>> = _exhibits.asStateFlow()

    val selectedCaseEvidenceList: StateFlow<List<Evidence>> = evidence

    fun toggleEvidenceSelection(evidenceId: String) {
        val evidenceToToggle = _evidence.value.find { it.id == evidenceId }
        if (_selectedEvidence.value == evidenceToToggle) {
            _selectedEvidence.value = null
        } else {
            _selectedEvidence.value = evidenceToToggle
        }
    }

    fun assignEvidenceToElement(evidence: Evidence, allegationId: String, allegationElementName: String) {
        val updatedEvidence = evidence.copy(allegationId = allegationId, allegationElementName = allegationElementName)
        updateEvidence(updatedEvidence)
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun toggleAllegationSelection(allegationId: String) {
        val currentSelected = _selectedAllegations.value.toMutableSet()
        if (currentSelected.contains(allegationId)) {
            currentSelected.remove(allegationId)
        } else {
            currentSelected.add(allegationId)
        }
        _selectedAllegations.value = currentSelected
    }

    fun clearAllCases() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // db.clearAllTables() // db reference is missing
            }
        }
    }

    fun loadCases() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val folderIdResult = googleApiService.getOrCreateAppRootFolder()
                if (folderIdResult is Result.Success) {
                    val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(folderIdResult.data)
                    val casesFromRegistry = googleApiService.getAllCasesFromRegistry(registryId)
                    _cases.value = casesFromRegistry
                } else if (folderIdResult is Result.Error) {
                    _error.value = folderIdResult.exception.message
                } else if (folderIdResult is Result.UserRecoverableError) {
                    _isUserRecoverableError.value = folderIdResult.exception
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCase(caseName: String, exhibitSheetName: String, caseNumber: String?, caseSection: String?, caseJudge: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val folderIdResult = googleApiService.getOrCreateAppRootFolder()
                if (folderIdResult is Result.Success) {
                    val folderId = folderIdResult.data
                    val spreadsheetIdResult = googleApiService.createSpreadsheet(caseName, folderId)
                    if (spreadsheetIdResult is Result.Success) {
                        val spreadsheetId = spreadsheetIdResult.data
                        if (spreadsheetId != null) {
                            googleApiService.addSheet(spreadsheetId, exhibitSheetName)
                            googleApiService.addSheet(spreadsheetId, "Allegations")
                            googleApiService.addSheet(spreadsheetId, "Evidence")
                            val newCase = Case(
                                id = (System.currentTimeMillis() / 1000).toInt(),
                                name = caseName,
                                spreadsheetId = spreadsheetId,
                                folderId = folderId,
                                lastModifiedTime = System.currentTimeMillis(),
                                caseNumber = caseNumber,
                                caseSection = caseSection,
                                court = caseJudge
                            )
                            val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(folderId)
                            val success = googleApiService.addCaseToRegistry(registryId, newCase)
                            if (success) {
                                val currentCases = _cases.value.toMutableList()
                                currentCases.add(newCase)
                                _cases.value = currentCases
                                setCurrentCase(newCase)
                            } else {
                                _error.value = "Failed to add case to registry."
                            }
                        } else {
                            _error.value = "Failed to create spreadsheet."
                        }
                    } else if (spreadsheetIdResult is Result.Error) {
                        _error.value = spreadsheetIdResult.exception.message
                    } else if (spreadsheetIdResult is Result.UserRecoverableError) {
                        _isUserRecoverableError.value = spreadsheetIdResult.exception
                    }
                } else if (folderIdResult is Result.Error) {
                    _error.value = folderIdResult.exception.message
                } else if (folderIdResult is Result.UserRecoverableError) {
                    _isUserRecoverableError.value = folderIdResult.exception
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCurrentCase(case: Case?) {
        _currentCase.value = case
        if (case != null) {
            loadEvidenceForCase(case.spreadsheetId, case.id.toLong())
            loadAllegationsForCase(case.spreadsheetId, case.id)
        }
    }

    private fun loadAllegationsForCase(spreadsheetId: String, caseId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allegations = withContext(Dispatchers.IO) {
                    googleApiService.getAllegationsForCase(spreadsheetId, caseId)
                }
                _allegations.value = allegations
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addAllegation(allegationText: String, allegationElementName: String) {
        val case = _currentCase.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    googleApiService.addAllegationToCase(case.spreadsheetId, allegationText, allegationElementName)
                }
                if (success) {
                    loadAllegationsForCase(case.spreadsheetId, case.id)
                } else {
                    _error.value = "Failed to add allegation."
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadEvidenceForCase(spreadsheetId: String, caseId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val evidenceFromSheet = withContext(Dispatchers.IO) {
                    googleApiService.getEvidenceForCase(spreadsheetId, caseId)
                }
                _evidence.value = evidenceFromSheet
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addEvidence(evidence: Evidence) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    googleApiService.addEvidenceToCase(evidence)
                }
                if (response != null) {
                    val currentEvidence = _evidence.value.toMutableList()
                    currentEvidence.add(evidence)
                    _evidence.value = currentEvidence
                } else {
                    _error.value = "Failed to add evidence."
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEvidence(evidence: Evidence) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    googleApiService.updateEvidenceInSheet(evidence)
                }
                if (success) {
                    val currentEvidence = _evidence.value.toMutableList()
                    val index = currentEvidence.indexOfFirst { it.id == evidence.id }
                    if (index != -1) {
                        currentEvidence[index] = evidence
                        _evidence.value = currentEvidence
                    }
                } else {
                    _error.value = "Failed to update evidence."
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEvidence(evidence: Evidence) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    googleApiService.deleteEvidenceFromSheet(evidence)
                }
                if (success) {
                    val currentEvidence = _evidence.value.toMutableList()
                    currentEvidence.removeAll { it.id == evidence.id }
                    _evidence.value = currentEvidence
                } else {
                    _error.value = "Failed to delete evidence."
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processUploadedCaseSheet(uri: Uri, caseName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val caseSheetData = application.contentResolver.openInputStream(uri)?.use { inputStream ->
                    CaseSheetParser.parse(inputStream)
                }

                if (caseSheetData != null) {
                    val (caseDetails, allegations, evidence) = caseSheetData

                    // 1. Create Case
                    val folderIdResult = googleApiService.getOrCreateAppRootFolder()
                    if (folderIdResult is Result.Success) {
                        val folderId = folderIdResult.data
                        val spreadsheetIdResult = googleApiService.createSpreadsheet(caseName, folderId)

                        if (spreadsheetIdResult is Result.Success) {
                            val spreadsheetId = spreadsheetIdResult.data
                            if (spreadsheetId != null) {
                                val newCase = Case(
                                    id = (System.currentTimeMillis() / 1000).toInt(),
                                    name = caseName,
                                    spreadsheetId = spreadsheetId,
                                    folderId = folderId,
                                    plaintiffs = caseDetails["Plaintiff(s)"] as String?,
                                    defendants = caseDetails["Defendant(s)"] as String?,
                                    court = caseDetails["Court"] as String?,
                                    lastModifiedTime = System.currentTimeMillis()
                                )

                                // 2. Add to Registry
                                val registryId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(folderId)
                                googleApiService.addCaseToRegistry(registryId, newCase)

                                // 3. Add Allegations sheet and data
                                googleApiService.addSheet(spreadsheetId, "Allegations")
                                allegations.forEach { allegation ->
                                    googleApiService.addAllegationToCase(spreadsheetId, allegation.text, allegation.allegationElementName)
                                }

                                // 4. Add Evidence sheet and data
                                googleApiService.addSheet(spreadsheetId, "Evidence")
                                evidence.forEach { ev ->
                                    googleApiService.addEvidenceToCase(ev.copy(spreadsheetId = spreadsheetId))
                                }

                                // 5. Update UI
                                val currentCases = _cases.value.toMutableList()
                                currentCases.add(newCase)
                                _cases.value = currentCases
                                setCurrentCase(newCase)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun handleUserRecoverableAuthException(exception: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
        // Logic to handle user recoverable auth exception
    }

    fun generateTimeline() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val events = mutableListOf<TimelineEvent>()
                evidence.value.forEach { ev ->
                    events.add(TimelineEvent(ev.timestamp, "Evidence", ev.content, ev))
                }
                _timelineEvents.value = events.sortedBy { it.timestamp }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveAndUploadHtml(context: Context, htmlContent: String, caseName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = File(context.filesDir, "$caseName.html")
                FileOutputStream(file).use { it.write(htmlContent.toByteArray()) }

                val caseFolderId = googleApiService.getOrCreateCaseFolder(caseName)
                if (caseFolderId != null) {
                    val result = googleApiService.uploadFile(file, caseFolderId, "text/html")
                    if (result is Result.Success) {
                        val uploadedFile = result.data
                        _currentCase.value?.let {
                            val updatedCase = it.copy(sourceHtmlSnapshotId = uploadedFile?.id)
                            updateCase(updatedCase)
                        }
                    } else if (result is Result.Error) {
                        _error.value = result.exception.message
                    }
                } else {
                    _error.value = "Could not find or create case folder."
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCase(case: Case) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = googleApiService.updateCaseInRegistry(case)
                if (success) {
                    val currentCases = _cases.value.toMutableList()
                    val index = currentCases.indexOfFirst { it.id == case.id }
                    if (index != -1) {
                        currentCases[index] = case
                        _cases.value = currentCases
                        if (_currentCase.value?.id == case.id) {
                            _currentCase.value = case
                        }
                    }
                } else {
                    _error.value = "Failed to update case."
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun executeScript(scriptId: String, functionName: String, parameters: List<Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = googleApiService.runGoogleAppsScript(scriptId, functionName, parameters)
            if (result is Result.Success<*>) {
                Log.d(TAG, "Script executed successfully: ${result.data}")
            } else if (result is Result.Error) {
                _error.value = result.exception.message
            } else if (result is Result.UserRecoverableError) {
                _isUserRecoverableError.value = result.exception
            }
            _isLoading.value = false
        }
    }

    fun archiveCase(case: Case) {
        viewModelScope.launch {
            val archivedCase = case.copy(isArchived = true)
            updateCase(archivedCase)
        }
    }

    fun deleteCase(case: Case) {
        viewModelScope.launch {
            val updatedCases = _cases.value.filterNot { it.id == case.id }
            _cases.value = updatedCases
            if (_currentCase.value?.id == case.id) {
                _currentCase.value = null
            }
        }
    }
}

data class CaseScreenUiState(
    val currentCase: Case? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CaseDetailState(
    val case: Case? = null,
    val evidence: List<Evidence> = emptyList(),
    val allegations: List<Allegation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ReviewScreenState(
    val evidence: List<Evidence> = emptyList(),
    val allegations: List<Allegation> = emptyList(),
    val selectedAllegations: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TimelineScreenState(
    val timelineEvents: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
