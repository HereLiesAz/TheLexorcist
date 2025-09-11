package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.data.MasterAllegationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MasterAllegationsViewModel @Inject constructor(
    private val repository: MasterAllegationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allegations: StateFlow<List<MasterAllegation>> =
        repository.getMasterAllegations()
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
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
