package com.hereliesaz.lexorcist.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.SortOrder
import com.hereliesaz.lexorcist.model.SheetFilter
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CaseViewModel
@Inject
constructor(
    @param:ApplicationContext private val applicationContext: Context,
    private val caseRepository: CaseRepository,
) : ViewModel() {
    private val sharedPref =
        applicationContext.getSharedPreferences("CaseInfoPrefs", Context.MODE_PRIVATE)

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val cases: StateFlow<List<Case>> =
        caseRepository.cases
            .combine(sortOrder) { cases, currentSortOrder ->
                when (currentSortOrder) {
                    SortOrder.NAME_ASC -> cases.sortedBy { it.name }
                    SortOrder.NAME_DESC -> cases.sortedByDescending { it.name }
                    SortOrder.DATE_ASC -> cases.sortedBy { it.id }
                    SortOrder.DATE_DESC -> cases.sortedByDescending { it.id }
                }
            }
            .combine(searchQuery) { cases, query ->
                if (query.isBlank()) {
                    cases
                } else {
                    cases.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedCase: StateFlow<Case?> =
        caseRepository.selectedCase.stateIn(viewModelScope, SharingStarted.Lazily, null)

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

    private val _userRecoverableAuthIntent = MutableStateFlow<Intent?>(null)
    val userRecoverableAuthIntent: StateFlow<Intent?> =
        _userRecoverableAuthIntent.asStateFlow()

    private val _selectedCaseEvidenceList =
        MutableStateFlow<List<com.hereliesaz.lexorcist.data.Evidence>>(emptyList())
    val selectedCaseEvidenceList: StateFlow<List<com.hereliesaz.lexorcist.data.Evidence>> =
        _selectedCaseEvidenceList.asStateFlow()

    private val _themeMode =
        MutableStateFlow(com.hereliesaz.lexorcist.ui.theme.ThemeMode.SYSTEM)
    val themeMode: StateFlow<com.hereliesaz.lexorcist.ui.theme.ThemeMode> =
        _themeMode.asStateFlow()

    init {
        loadThemeModePreference()
        // observeAuthChanges() //TODO: Re-enable this once AuthViewModel is provided correctly
    }

    private fun clearCaseData() {
        viewModelScope.launch { caseRepository.selectCase(null) }
        _sheetFilters.value = emptyList()
        _allegations.value = emptyList()
        _htmlTemplates.value = emptyList()
        _plaintiffs.value = ""
        _defendants.value = ""
        _court.value = ""
        _selectedCaseEvidenceList.value = emptyList()
        saveCaseInfoToSharedPrefs()
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearUserRecoverableAuthIntent() {
        _userRecoverableAuthIntent.value = null
    }

    fun setThemeMode(themeMode: com.hereliesaz.lexorcist.ui.theme.ThemeMode) {
        _themeMode.value = themeMode
        sharedPref.edit().putString("theme_mode", themeMode.name).apply()
    }

    private fun loadThemeModePreference() {
        val themeName =
            sharedPref.getString(
                "theme_mode",
                com.hereliesaz.lexorcist.ui.theme.ThemeMode.SYSTEM.name
            )
        _themeMode.value =
            com.hereliesaz.lexorcist.ui.theme.ThemeMode.valueOf(
                themeName ?: com.hereliesaz.lexorcist.ui.theme.ThemeMode.SYSTEM.name
            )
    }

    fun onSortOrderChange(newSortOrder: SortOrder) {
        _sortOrder.value = newSortOrder
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun loadCasesFromRepository() {
        viewModelScope.launch { caseRepository.refreshCases() }
    }

    fun loadHtmlTemplatesFromRepository() {
        viewModelScope.launch {
            caseRepository.refreshHtmlTemplates()
            caseRepository.getHtmlTemplates().collect {
                _htmlTemplates.value = it
            }
        }
    }

    fun importSpreadsheetWithRepository(spreadsheetId: String) {
        viewModelScope.launch { caseRepository.importSpreadsheet(spreadsheetId) }
    }

    fun createCase(
        caseName: String,
        exhibitSheetName: String,
        caseNumber: String,
        caseSection: String,
        caseJudge: String,
    ) {
        android.util.Log.d("CaseViewModel", "createCase called with name: $caseName")
        viewModelScope.launch {
            val result =
                caseRepository.createCase(
                    caseName,
                    exhibitSheetName,
                    caseNumber,
                    caseSection,
                    caseJudge,
                    plaintiffs.value,
                    defendants.value,
                    court.value,
                )
            when (result) {
                is Result.Success -> {
                    android.util.Log.d("CaseViewModel", "Case creation successful")
                }
                is Result.Error -> {
                    _errorMessage.value =
                        result.exception.message ?: "Unknown error during case creation"
                }
                is Result.UserRecoverableError -> {
                    _userRecoverableAuthIntent.value = result.exception.intent
                }
            }
        }
    }

    fun selectCase(case: Case?) {
        viewModelScope.launch {
            caseRepository.selectCase(case)
            if (case != null) {
                loadSheetFiltersFromRepository(case.spreadsheetId)
                loadAllegationsFromRepository(case.id, case.spreadsheetId)
                loadHtmlTemplatesFromRepository()
                loadEvidenceForSelectedCase()
            } else {
                _sheetFilters.value = emptyList()
                _allegations.value = emptyList()
                _htmlTemplates.value = emptyList()
                _selectedCaseEvidenceList.value = emptyList()
            }
        }
    }

    private fun loadSheetFiltersFromRepository(spreadsheetId: String) {
        viewModelScope.launch {
            caseRepository.refreshSheetFilters(spreadsheetId)
            caseRepository.getSheetFilters(spreadsheetId).collect {
                _sheetFilters.value = it
            }
        }
    }

    fun addSheetFilterWithRepository(
        name: String,
        value: String,
    ) {
        viewModelScope.launch {
            val spreadsheetId = selectedCase.value?.spreadsheetId ?: return@launch
            caseRepository.addSheetFilter(spreadsheetId, name, value)
        }
    }

    private fun loadAllegationsFromRepository(
        caseId: Int,
        spreadsheetId: String,
    ) {
        viewModelScope.launch {
            caseRepository.refreshAllegations(caseId, spreadsheetId)
            caseRepository.getAllegations(caseId, spreadsheetId).collect {
                _allegations.value = it
            }
        }
    }

    fun addAllegationWithRepository(allegationText: String) {
        viewModelScope.launch {
            val case = selectedCase.value ?: return@launch
            caseRepository.addAllegation(case.spreadsheetId, allegationText)
            loadAllegationsFromRepository(case.id, case.spreadsheetId)
        }
    }

    internal fun loadEvidenceForSelectedCase() {
        viewModelScope.launch {
            selectedCase.value?.let { case ->
                when (val result = caseRepository.getEvidenceForCase(case.spreadsheetId)) {
                    is Result.Success -> { // Changed from Result.Success<*> to Result.Success
                        _selectedCaseEvidenceList.value = result.data
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.exception.message ?: "Unknown error"
                    }
                    is Result.UserRecoverableError -> {
                        _userRecoverableAuthIntent.value = result.exception.intent
                    }
                }
            }
        }
    }

    fun onPlaintiffsChanged(name: String) {
        _plaintiffs.value = name
        saveCaseInfoToSharedPrefs()
    }

    fun onDefendantsChanged(name: String) {
        _defendants.value = name
        saveCaseInfoToSharedPrefs()
    }

    fun onCourtChanged(name: String) {
        _court.value = name
        saveCaseInfoToSharedPrefs()
    }

    private fun saveCaseInfoToSharedPrefs() {
        sharedPref
            .edit()
            .putString("plaintiffs", _plaintiffs.value)
            .putString("defendants", _defendants.value)
            .putString("court", _court.value)
            .apply()
    }

    fun archiveCaseWithRepository(case: Case) {
        viewModelScope.launch { caseRepository.archiveCase(case) }
    }

    fun deleteCaseWithRepository(case: Case) {
        viewModelScope.launch { caseRepository.deleteCase(case) }
    }

    fun clearCache() {
        viewModelScope.launch {
            caseRepository.clearCache()
            clearCaseData()
            // Clear shared preferences
            sharedPref.edit().clear().apply()
            // After clearing, reload the theme preference as it's also stored in sharedPref
            loadThemeModePreference()
        }
    }
}
