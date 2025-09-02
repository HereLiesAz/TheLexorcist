package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import com.hereliesaz.lexorcist.data.Allegation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

import com.hereliesaz.lexorcist.data.AllegationsRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

@HiltViewModel
class AllegationsViewModel @Inject constructor(
    private val allegationsRepository: AllegationsRepository
) : ViewModel() {

    private val _allegations = MutableStateFlow<List<Allegation>>(emptyList())
    val allegations: StateFlow<List<Allegation>> = _allegations

    fun loadAllegations(caseId: String) {
        viewModelScope.launch {
            _allegations.value = allegationsRepository.getAllegations(caseId)
        }
    }
}
