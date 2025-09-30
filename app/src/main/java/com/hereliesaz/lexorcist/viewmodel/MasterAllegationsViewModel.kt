package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Allegation
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

    private val _masterAllegations = MutableStateFlow<List<MasterAllegation>>(emptyList())
    private val selectedAllegationsFromRepo: StateFlow<Set<Allegation>> = caseAllegationSelectionRepository.selectedAllegations

    val selectedAllegations: StateFlow<List<MasterAllegation>> =
        combine(_masterAllegations, selectedAllegationsFromRepo) { master, selected ->
            val selectedIds = selected.map { it.id.toString() }.toSet()
            master.filter { selectedIds.contains(it.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allegations: StateFlow<List<MasterAllegation>> =
        combine(
            _masterAllegations,
            selectedAllegationsFromRepo,
            _searchQuery,
            _sortType
        ) { master, selected, query, sort ->
            val selectedIds = selected.map { it.id.toString() }.toSet()
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
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortTypeChanged(sortType: AllegationSortType) {
        _sortType.value = sortType
    }

    fun toggleAllegationSelection(allegation: MasterAllegation) {
        viewModelScope.launch {
            val case = caseRepository.selectedCase.value
                ?: throw IllegalStateException("Cannot toggle allegation when no case is selected.")

            val allegationId = allegation.id?.toIntOrNull()
            if (allegationId == null) {
                // Handle error: MasterAllegation has no valid ID
                return@launch
            }

            val repoAllegation = Allegation(
                id = allegationId,
                spreadsheetId = case.spreadsheetId,
                name = allegation.name
            )

            if (allegation.isSelected) {
                caseAllegationSelectionRepository.removeAllegation(repoAllegation)
            } else {
                caseAllegationSelectionRepository.addAllegation(repoAllegation)
            }
        }
    }
}