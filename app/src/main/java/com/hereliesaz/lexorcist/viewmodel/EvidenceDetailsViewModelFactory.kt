package com.hereliesaz.lexorcist.viewmodel

import android.app.Application // Added import
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.EvidenceRepository

// Factory now needs Application
class EvidenceDetailsViewModelFactory(
    private val application: Application, // Added Application parameter
    private val evidenceRepository: EvidenceRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvidenceDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass arguments in the correct order
            return EvidenceDetailsViewModel(application, evidenceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
