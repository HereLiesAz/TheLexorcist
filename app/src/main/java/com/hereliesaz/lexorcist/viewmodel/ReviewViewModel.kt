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

/**
 * ViewModel for the Review screen functionality.
 *
 * Currently focused on identifying similar text evidence to help users merge or clean up duplicates.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val cleanupService: CleanupService
) : ViewModel() {

    private val _similarTextGroups = MutableStateFlow<List<List<Evidence>>>(emptyList())
    /**
     * A flow of grouped evidence items that have similar text content.
     * Each inner list represents a group of potential duplicates.
     */
    val similarTextGroups: StateFlow<List<List<Evidence>>> = _similarTextGroups.asStateFlow()

    /**
     * Triggers the analysis of the provided evidence list to find similar text content.
     * Uses fuzzy matching or other heuristics defined in [CleanupService].
     */
    fun findSimilarTextEvidence(evidenceList: List<Evidence>) {
        viewModelScope.launch {
            _similarTextGroups.value = cleanupService.findSimilarTextEvidence(evidenceList)
        }
    }
}
