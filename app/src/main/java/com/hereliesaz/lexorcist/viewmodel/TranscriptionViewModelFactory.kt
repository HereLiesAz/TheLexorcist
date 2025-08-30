package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository

class EvidenceViewModelFactory(
    private val application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val authViewModel: AuthViewModel
    private val caseRepository: CaseRepository,
    private val evidenceRepository: EvidenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvidenceViewModel(application, evidenceRepository, authViewModel) as T
            return EvidenceViewModel(application, caseRepository, evidenceRepository) as T
            return TranscriptionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
