package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.service.CleanupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val cleanupService: CleanupService
) : ViewModel() {

    private val _similarTextGroups = MutableStateFlow<List<List<Evidence>>>(emptyList())
    val similarTextGroups: StateFlow<List<List<Evidence>>> = _similarTextGroups.asStateFlow()

    fun findSimilarTextEvidence(evidenceList: List<Evidence>) {
        viewModelScope.launch {
            _similarTextGroups.value = cleanupService.findSimilarTextEvidence(evidenceList)
        }
    }
}
