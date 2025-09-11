package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.AllegationsRepository // Changed import
import com.hereliesaz.lexorcist.data.MasterAllegation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MasterAllegationsViewModel @Inject constructor(
    private val allegationsRepository: AllegationsRepository // Changed to AllegationsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawMasterAllegations = MutableStateFlow<List<MasterAllegation>>(emptyList())

    // Publicly exposed allegations, filtered by search query
    val allegations: StateFlow<List<MasterAllegation>> = 
        combine(_rawMasterAllegations, _searchQuery) { allegationsList, query ->
            if (query.isBlank()) {
                allegationsList
            } else {
                allegationsList.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true) ||
                            it.category.contains(query, ignoreCase = true) ||
                            it.type.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            allegationsRepository.isMasterSheetIdInitialized.collectLatest { isInitialized ->
                if (isInitialized) {
                    refreshMasterAllegations()
                } else {
                    _rawMasterAllegations.value = emptyList()
                }
            }
        }
    }

    private suspend fun refreshMasterAllegations() {
        _rawMasterAllegations.value = allegationsRepository.getMasterAllegations()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addMasterAllegation(allegation: MasterAllegation) {
        viewModelScope.launch {
            val success = allegationsRepository.addAllegationToMasterList(allegation)
            if (success) {
                refreshMasterAllegations() // Refresh the list after adding
                // TODO: Optionally, expose success message to UI
            } else {
                // TODO: Optionally, expose error message to UI
            }
        }
    }
}
