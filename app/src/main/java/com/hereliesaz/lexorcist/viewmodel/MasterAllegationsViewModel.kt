package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.data.MasterAllegationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
        private val repository: MasterAllegationRepository,
    ) : ViewModel() {
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        private val _selectedAllegations = MutableStateFlow<Set<MasterAllegation>>(emptySet())
        val selectedAllegations: StateFlow<Set<MasterAllegation>> = _selectedAllegations.asStateFlow()

        private val _sortType = MutableStateFlow(AllegationSortType.TYPE)
        val sortType: StateFlow<AllegationSortType> = _sortType.asStateFlow()

        val allegations: StateFlow<List<MasterAllegation>> =
            repository
                .getMasterAllegations()
                .combine(searchQuery) { allegations, query ->
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
                        AllegationSortType.TYPE -> allegations.sortedWith(compareBy({ it.type }, { it.category }, { it.name }))
                        AllegationSortType.CATEGORY -> allegations.sortedWith(compareBy({ it.category }, { it.type }, { it.name }))
                        AllegationSortType.NAME -> allegations.sortedBy { it.name }
                        AllegationSortType.COURT_LEVEL -> allegations.sortedBy { it.courtLevel }
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        fun onSearchQueryChanged(query: String) {
            _searchQuery.value = query
        }

        fun onSortTypeChanged(sortType: AllegationSortType) {
            _sortType.value = sortType
        }

        fun toggleAllegationSelection(allegation: MasterAllegation) {
            val currentSelection = _selectedAllegations.value
            _selectedAllegations.value =
                if (allegation in currentSelection) {
                    currentSelection - allegation
                } else {
                    currentSelection + allegation
                }
        }
    }
