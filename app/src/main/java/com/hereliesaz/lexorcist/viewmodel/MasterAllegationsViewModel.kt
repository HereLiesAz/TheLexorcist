package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.CaseAllegationSelectionRepository
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.data.repository.LegalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AllegationSortType {
    TYPE,
    CATEGORY,
    NAME,
    COURT_LEVEL,
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MasterAllegationsViewModel @Inject constructor(
    private val legalRepository: LegalRepository,
    private val caseAllegationSelectionRepository: CaseAllegationSelectionRepository,
    private val caseRepository: CaseRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(AllegationSortType.TYPE)
    val sortType: StateFlow<AllegationSortType> = _sortType.asStateFlow()

    // Internal state holders
    private val _masterAllegations = MutableStateFlow<List<MasterAllegation>>(emptyList())
    private val _selectedAllegationNames = MutableStateFlow<Set<String>>(emptySet())

    // Public flows for the UI
    val selectedAllegations: StateFlow<List<MasterAllegation>> =
        combine(_masterAllegations, _selectedAllegationNames) { master, selectedNames ->
            master.filter { selectedNames.contains(it.name) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allegations: StateFlow<List<MasterAllegation>> =
        combine(
            _masterAllegations,
            _selectedAllegationNames,
            _searchQuery,
            _sortType
        ) { master, selectedNames, query, sort ->
            val updatedMaster = master.map { it.copy(isSelected = selectedNames.contains(it.name)) }

            val filtered = if (query.isBlank()) {
                updatedMaster
            } else {
                updatedMaster.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true) ||
                            it.category.contains(query, ignoreCase = true) ||
                            it.type.contains(query, ignoreCase = true)
                }
            }

            when (sort) {
                AllegationSortType.TYPE ->
                    filtered.sortedWith(compareBy({ it.type }, { it.category }, { it.name }))
                AllegationSortType.CATEGORY ->
                    filtered.sortedWith(compareBy({ it.category }, { it.type }, { it.name }))
                AllegationSortType.NAME -> filtered.sortedBy { it.name }
                AllegationSortType.COURT_LEVEL -> filtered.sortedBy { it.courtLevel }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            legalRepository.getMasterAllegations().collect {
                _masterAllegations.value = it
            }
        }
        viewModelScope.launch {
            caseRepository.selectedCase.collectLatest { case ->
                if (case != null) {
                    val selected = caseAllegationSelectionRepository.getSelectedAllegations(case.spreadsheetId).firstOrNull() ?: emptyList()
                    _selectedAllegationNames.value = selected.toSet()
                } else {
                    _selectedAllegationNames.value = emptySet()
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortTypeChanged(sortType: AllegationSortType) {
        _sortType.value = sortType
    }

    fun toggleAllegationSelection(allegation: MasterAllegation) {
        viewModelScope.launch {
            val currentSelection = _selectedAllegationNames.value.toMutableSet()
            if (allegation.name in currentSelection) {
                currentSelection.remove(allegation.name)
            } else {
                currentSelection.add(allegation.name)
            }
            _selectedAllegationNames.value = currentSelection

            // Persist the change to the repository
            val case = caseRepository.selectedCase.firstOrNull()
            if (case != null) {
                caseAllegationSelectionRepository.updateSelectedAllegations(
                    case.spreadsheetId,
                    currentSelection.toList()
                )
            }
        }
    }
}
