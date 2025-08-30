package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.EvidenceRepository

class CaseViewModelFactory(
    private val application: Application,
    private val caseRepository: CaseRepository,
    private val authViewModel: AuthViewModel
class EvidenceDetailsViewModelFactory(
    private val evidenceRepository: EvidenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvidenceDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CaseViewModel(application, caseRepository, authViewModel) as T
            return EvidenceDetailsViewModel(evidenceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
