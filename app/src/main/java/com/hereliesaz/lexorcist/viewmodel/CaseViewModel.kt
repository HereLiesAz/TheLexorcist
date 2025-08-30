package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.SortOrder
import com.hereliesaz.lexorcist.model.SheetFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaseViewModel @Inject constructor(
    application: Application,
    private val caseRepository: CaseRepository
) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val cases: StateFlow<List<Case>> = caseRepository.getCases()
        .combine(sortOrder) { cases, sortOrder ->
            when (sortOrder) {
                SortOrder.NAME_ASC -> cases.sortedBy { it.name }
                SortOrder.NAME_DESC -> cases.sortedByDescending { it.name }
                SortOrder.DATE_ASC -> cases.sortedBy { it.id }
                SortOrder.DATE_DESC -> cases.sortedByDescending { it.id }
                else -> cases.sortedByDescending { it.id }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _selectedCase.asStateFlow()

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val sheetFilters: StateFlow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations.asStateFlow()

    private val _plaintiffs = MutableStateFlow(sharedPref.getString("plaintiffs", "") ?: "")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()

    private val _defendants = MutableStateFlow(sharedPref.getString("defendants", "") ?: "")
    val defendants: StateFlow<String> = _defendants.asStateFlow()

    private val _court = MutableStateFlow(sharedPref.getString("court", "") ?: "")
    val court: StateFlow<String> = _court.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCases()
        loadDarkModePreference()
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        sharedPref.edit()
            .putBoolean("is_dark_mode", isDark)
            .apply()
    }

    private fun loadDarkModePreference() {
        _isDarkMode.value = sharedPref.getBoolean("is_dark_mode", false)
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun loadCases() {
        viewModelScope.launch {
            caseRepository.refreshCases()
        }
    }

    fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String
    ) {
        viewModelScope.launch {
            caseRepository.createCase(
                caseName,
                exhibitSheetName,
                caseNumber,
                caseSection,
                caseJudge,
                plaintiffs.value,
                defendants.value,
                court.value
            )
        }
    }

    fun selectCase(case: Case?) {
        _selectedCase.value = case
        if (case != null) {
            loadSheetFilters(case.spreadsheetId)
            loadAllegations(case.id, case.spreadsheetId)
        } else {
            _sheetFilters.value = emptyList()
            _allegations.value = emptyList()
        }
    }

    private fun loadSheetFilters(spreadsheetId: String) {
        viewModelScope.launch {
            caseRepository.refreshSheetFilters(spreadsheetId)
            caseRepository.getSheetFilters(spreadsheetId).collect {
                _sheetFilters.value = it
            }
        }
    }

    fun addSheetFilter(name: String, value: String) {
        val spreadsheetId = _selectedCase.value?.spreadsheetId ?: return
        viewModelScope.launch {
            caseRepository.addSheetFilter(spreadsheetId, name, value)
        }
    }

    private fun loadAllegations(caseId: Int, spreadsheetId: String) {
        viewModelScope.launch {
            caseRepository.refreshAllegations(caseId, spreadsheetId)
            caseRepository.getAllegations(caseId, spreadsheetId).collect {
                _allegations.value = it
            }
        }
    }

    fun addAllegation(allegationText: String) {
        val case = _selectedCase.value ?: return
        viewModelScope.launch {
            caseRepository.addAllegation(case.spreadsheetId, allegationText)
            // Refresh allegations after adding a new one
            loadAllegations(case.id, case.spreadsheetId)
        }
    }

    fun onPlaintiffsChanged(name: String) {
        _plaintiffs.value = name
        saveCaseInfo()
    }

    fun onDefendantsChanged(name: String) {
        _defendants.value = name
        saveCaseInfo()
    }

    fun onCourtChanged(name: String) {
        _court.value = name
        saveCaseInfo()
    }

    private fun saveCaseInfo() {
        sharedPref.edit()
            .putString("plaintiffs", _plaintiffs.value)
            .putString("defendants", _defendants.value)
            .putString("court", _court.value)
            .apply()
    }

    fun archiveCase(case: Case) {
        viewModelScope.launch {
            caseRepository.archiveCase(case)
        }
    }

    fun deleteCase(case: Case) {
        viewModelScope.launch {
            caseRepository.deleteCase(case)
        }
    }
}
