package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.EvidenceRepository
import kotlinx.coroutines.launch

class EvidenceDetailsViewModel(
    private val evidenceRepository: EvidenceRepository
) : ViewModel() {

    fun updateCommentary(id: Int, commentary: String) {
        viewModelScope.launch {
            evidenceRepository.updateCommentary(id, commentary)
        }
    }
}
