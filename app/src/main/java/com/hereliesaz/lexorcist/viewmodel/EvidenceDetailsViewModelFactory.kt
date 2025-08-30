package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.EvidenceRepository

class EvidenceDetailsViewModelFactory(
    private val evidenceRepository: EvidenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvidenceDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvidenceDetailsViewModel(evidenceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
