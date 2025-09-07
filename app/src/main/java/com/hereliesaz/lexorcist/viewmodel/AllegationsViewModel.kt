package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.AllegationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllegationsViewModel
    @Inject
    constructor(
        private val allegationsRepository: AllegationsRepository,
    ) : ViewModel() {
        private val _allAllegations = MutableStateFlow<List<Allegation>>(emptyList())
        val allAllegations: StateFlow<List<Allegation>> = _allAllegations

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery

        val allegations: StateFlow<List<Allegation>> =
            combine(
                _allAllegations,
                _searchQuery,
            ) { allegations, query ->
                if (query.isBlank()) {
                    allegations
                } else {
                    allegations.filter { it.text.contains(query, ignoreCase = true) }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun onSearchQueryChanged(query: String) {
            _searchQuery.value = query
        }

        private val _selectedAllegation = MutableStateFlow<Allegation?>(null)
        val selectedAllegation: StateFlow<Allegation?> = _selectedAllegation

        private val _isDialogShown = MutableStateFlow(false)
        val isDialogShown: StateFlow<Boolean> = _isDialogShown

        fun onAllegationSelected(allegation: Allegation) {
            _selectedAllegation.value = allegation
            _isDialogShown.value = true
        }

        fun onDialogDismiss() {
            _isDialogShown.value = false
        }

        fun loadAllegations(caseId: String) {
            viewModelScope.launch {
                _allAllegations.value = allegationsRepository.getAllegations(caseId)
            }
        }
    }
