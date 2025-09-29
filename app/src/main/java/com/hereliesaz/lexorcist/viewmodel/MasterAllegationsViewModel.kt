package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.*
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
    private val _selectedAllegations = MutableStateFlow<Set<SelectedAllegation>>(emptySet())

    // Public flows for the UI
    val selectedAllegations: StateFlow<List<MasterAllegation>> =
        combine(_masterAllegations, _selectedAllegations) { master, selected ->
            val selectedIds = selected.map { it.id }.toSet()
            master.filter { selectedIds.contains(it.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allegations: StateFlow<List<MasterAllegation>> =
        combine(
            _masterAllegations,
            _selectedAllegations,
            _searchQuery,
            _sortType
        ) { master, selected, query, sort ->
            val selectedIds = selected.map { it.id }.toSet()
            val updatedMaster = master.map {
                it.copy(isSelected = selectedIds.contains(it.id))
            }

            val filtered = if (query.isBlank()) {
                updatedMaster
            } else {
                updatedMaster.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            (it.description?.contains(query, ignoreCase = true) ?: false) ||
                            (it.category?.contains(query, ignoreCase = true) ?: false) ||
                            (it.type?.contains(query, ignoreCase = true) ?: false)
                }
            }

            when (sort) {
                AllegationSortType.TYPE ->
                    filtered.sortedWith(compareBy({ it.type ?: "" }, { it.category ?: "" }, { it.name }))
                AllegationSortType.CATEGORY ->
                    filtered.sortedWith(compareBy({ it.category ?: "" }, { it.type ?: "" }, { it.name }))
                AllegationSortType.NAME -> filtered.sortedBy { it.name }
                AllegationSortType.COURT_LEVEL -> filtered.sortedBy { it.courtLevel ?: "" }
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
                    _selectedAllegations.value = selected.toSet()
                } else {
                    _selectedAllegations.value = emptySet()
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
            val selectedAllegation = SelectedAllegation(id = allegation.id, name = allegation.name)

            val currentSelection = _selectedAllegations.value.toMutableSet()
            if (currentSelection.contains(selectedAllegation)) {
                currentSelection.remove(selectedAllegation)
            } else {
                currentSelection.add(selectedAllegation)
            }
            _selectedAllegations.value = currentSelection

            // Persist the change to the repository
            val case = caseRepository.selectedCase.value
                ?: throw IllegalStateException("Cannot toggle allegation when no case is selected.")

            caseAllegationSelectionRepository.updateSelectedAllegations(
                case.spreadsheetId,
                currentSelection.toList()
            )
        }
    }
}