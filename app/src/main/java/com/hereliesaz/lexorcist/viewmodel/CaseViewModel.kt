package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.data.Case
// import com.hereliesaz.lexorcist.data.CaseDao // Removed CaseDao
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.SortOrder
import com.hereliesaz.lexorcist.model.SheetFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val applicationContext: Context,
    private val caseRepository: CaseRepository
    // private val caseDao: CaseDao // Removed CaseDao
    // private val authViewModel: AuthViewModel // Assuming this is also a @HiltViewModel
) : ViewModel() {

    private val sharedPref = applicationContext.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Assuming caseRepository.getAllCases() exists and returns Flow<List<Case>>
    val cases: StateFlow<List<Case>> = caseRepository.getAllCases()
        .combine(sortOrder) { cases, currentSortOrder ->
            when (currentSortOrder) {
                SortOrder.NAME_ASC -> cases.sortedBy { it.name }
                SortOrder.NAME_DESC -> cases.sortedByDescending { it.name }
                SortOrder.DATE_ASC -> cases.sortedBy { it.id } 
                SortOrder.DATE_DESC -> cases.sortedByDescending { it.id }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedCase = MutableStateFlow<Case?>(null)
    val selectedCase: StateFlow<Case?> = _selectedCase.asStateFlow()

    private val _sheetFilters = MutableStateFlow<List<SheetFilter>>(emptyList())
    val sheetFilters: StateFlow<List<SheetFilter>> = _sheetFilters.asStateFlow()

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations.asStateFlow()

    private val _htmlTemplates = MutableStateFlow<List<DriveFile>>(emptyList())
    val htmlTemplates: StateFlow<List<DriveFile>> = _htmlTemplates.asStateFlow()

    private val _plaintiffs = MutableStateFlow(sharedPref.getString("plaintiffs", "") ?: "")
    val plaintiffs: StateFlow<String> = _plaintiffs.asStateFlow()

    private val _defendants = MutableStateFlow(sharedPref.getString("defendants", "") ?: "")
    val defendants: StateFlow<String> = _defendants.asStateFlow()

    private val _court = MutableStateFlow(sharedPref.getString("court", "") ?: "")
    val court: StateFlow<String> = _court.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedCaseEvidenceList = MutableStateFlow<List<com.hereliesaz.lexorcist.data.Evidence>>(emptyList())
    val selectedCaseEvidenceList: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> = _selectedCaseEvidenceList.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        loadDarkModePreference()
        // observeAuthChanges() //TODO: Re-enable this once AuthViewModel is provided correctly
    }

    private fun clearCaseData() {
        _selectedCase.value = null
        _sheetFilters.value = emptyList()
        _allegations.value = emptyList()
        _htmlTemplates.value = emptyList()
        _plaintiffs.value = ""
        _defendants.value = ""
        _court.value = ""
        _selectedCaseEvidenceList.value = emptyList()
        saveCaseInfoToSharedPrefs()
    }

    fun showError(message: String) { _errorMessage.value = message }
    fun clearError() { _errorMessage.value = null }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        sharedPref.edit().putBoolean("is_dark_mode", isDark).apply()
    }

    private fun loadDarkModePreference() {
        _isDarkMode.value = sharedPref.getBoolean("is_dark_mode", false)
    }

    fun onSortOrderChange(newSortOrder: SortOrder) { _sortOrder.value = newSortOrder }

    fun loadCasesFromRepository() {
        viewModelScope.launch {
            // Logic to load/refresh cases, e.g., caseRepository.refreshCasesFromRemote()
        }
    }
    
    fun loadHtmlTemplatesFromRepository() {
        viewModelScope.launch {
            // caseRepository.refreshHtmlTemplates()
            // caseRepository.getHtmlTemplates().collect { _htmlTemplates.value = it }
        }
    }

    fun importSpreadsheetWithRepository(spreadsheetId: String) {
        viewModelScope.launch { caseRepository.importSpreadsheet(spreadsheetId) } // Corrected method name
    }

    // Renamed from createNewCaseWithRepository
    fun createCase(
        caseName: String, exhibitSheetName: String, caseNumber: String,
        caseSection: String, caseJudge: String
    ) {
        viewModelScope.launch {
            caseRepository.createCase( // Corrected method name
                caseName, exhibitSheetName, caseNumber, caseSection, caseJudge,
                plaintiffs.value, defendants.value, court.value
            )
        }
    }

    fun selectCase(case: Case?) {
        _selectedCase.value = case
        if (case != null) {
            loadSheetFiltersFromRepository(case.spreadsheetId)
            loadAllegationsFromRepository(case.id, case.spreadsheetId)
        } else {
            _sheetFilters.value = emptyList()
            _allegations.value = emptyList()
        }
    }

    private fun loadSheetFiltersFromRepository(spreadsheetId: String) {
        viewModelScope.launch {
            caseRepository.refreshSheetFilters(spreadsheetId)
            caseRepository.getSheetFilters(spreadsheetId).collect { _sheetFilters.value = it }
        }
    }

    fun addSheetFilterWithRepository(name: String, value: String) {
        val spreadsheetId = _selectedCase.value?.spreadsheetId ?: return
        viewModelScope.launch { caseRepository.addSheetFilter(spreadsheetId, name, value) }
    }

    private fun loadAllegationsFromRepository(caseId: Int, spreadsheetId: String) {
        viewModelScope.launch {
            caseRepository.refreshAllegations(caseId, spreadsheetId)
            caseRepository.getAllegations(caseId, spreadsheetId).collect { _allegations.value = it }
        }
    }

    fun addAllegationWithRepository(allegationText: String) {
        val case = _selectedCase.value ?: return
        viewModelScope.launch {
            caseRepository.addAllegation(case.spreadsheetId, allegationText)
            loadAllegationsFromRepository(case.id, case.spreadsheetId)
        }
    }

    fun onPlaintiffsChanged(name: String) { _plaintiffs.value = name; saveCaseInfoToSharedPrefs() }
    fun onDefendantsChanged(name: String) { _defendants.value = name; saveCaseInfoToSharedPrefs() }
    fun onCourtChanged(name: String) { _court.value = name; saveCaseInfoToSharedPrefs() }

    private fun saveCaseInfoToSharedPrefs() {
        sharedPref.edit()
            .putString("plaintiffs", _plaintiffs.value)
            .putString("defendants", _defendants.value)
            .putString("court", _court.value)
            .apply()
    }

    fun archiveCaseWithRepository(case: Case) {
        viewModelScope.launch { caseRepository.archiveCase(case) } // Corrected method name
    }

    fun deleteCaseWithRepository(case: Case) {
        viewModelScope.launch { caseRepository.deleteCase(case) }
    }
}
