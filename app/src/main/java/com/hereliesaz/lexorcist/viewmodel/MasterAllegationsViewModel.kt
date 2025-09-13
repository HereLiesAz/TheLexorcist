package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.CaseAllegationSelectionRepository
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.data.MasterAllegationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
enum class AllegationSortType {
    TYPE,
    CATEGORY,
    NAME,
    COURT_LEVEL,
}

@HiltViewModel
class MasterAllegationsViewModel
@Inject
constructor(
    private val masterAllegationRepository: MasterAllegationRepository,
    private val caseAllegationSelectionRepository: CaseAllegationSelectionRepository,
    private val caseRepository: CaseRepository,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAllegations = MutableStateFlow<Set<MasterAllegation>>(emptySet())
    val selectedAllegations: StateFlow<Set<MasterAllegation>> =
        _selectedAllegations.asStateFlow()

    private val _sortType = MutableStateFlow(AllegationSortType.TYPE)
    val sortType: StateFlow<AllegationSortType> = _sortType.asStateFlow()

    val allegations: StateFlow<List<MasterAllegation>> =
        caseRepository.selectedCase
            .flatMapLatest { case ->
                val selectedAllegationsFlow =
                    if (case != null) {
                        caseAllegationSelectionRepository.getSelectedAllegations(case.spreadsheetId)
                    } else {
                        MutableStateFlow(emptyList())
                    }

                masterAllegationRepository
                    .getMasterAllegations()
                    .combine(selectedAllegationsFlow) { master, selected ->
                        master.map { it.copy(isSelected = selected.contains(it.name)) }
                    }.combine(searchQuery) { allegations, query ->
                        if (query.isBlank()) {
                            allegations
                        } else {
                            allegations.filter {
                                it.name.contains(query, ignoreCase = true) ||
                                        it.description.contains(query, ignoreCase = true) ||
                                        it.category.contains(query, ignoreCase = true) ||
                                        it.type.contains(query, ignoreCase = true)
                            }
                        }
                    }.combine(_sortType) { allegations, sortType ->
                        when (sortType) {
                            AllegationSortType.TYPE ->
                                allegations.sortedWith(
                                    compareBy({ it.type }, { it.category }, { it.name }),
                                )
                            AllegationSortType.CATEGORY ->
                                allegations.sortedWith(
                                    compareBy({ it.category }, { it.type }, { it.name }),
                                )
                            AllegationSortType.NAME -> allegations.sortedBy { it.name }
                            AllegationSortType.COURT_LEVEL ->
                                allegations.sortedBy { it.courtLevel }
                        }
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortTypeChanged(sortType: AllegationSortType) {
        _sortType.value = sortType
    }

    fun toggleAllegationSelection(allegation: MasterAllegation) {
        viewModelScope.launch {
            val case = caseRepository.selectedCase.firstOrNull() ?: return@launch
            val currentSelection =
                (
                        caseAllegationSelectionRepository
                            .getSelectedAllegations(case.spreadsheetId)
                            .firstOrNull() ?: emptyList()
                        ).toMutableSet()

            if (allegation.name in currentSelection) {
                currentSelection.remove(allegation.name)
            } else {
                currentSelection.add(allegation.name)
            }
            caseAllegationSelectionRepository.updateSelectedAllegations(
                case.spreadsheetId,
                currentSelection.toList(),
            )
        }
    }
}
