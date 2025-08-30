package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.EvidenceRepository

class EvidenceViewModelFactory(
    private val application: Application,
    private val evidenceRepository: EvidenceRepository,
    private val authViewModel: AuthViewModel,
    private val caseViewModel: CaseViewModel,
    private val ocrViewModel: OcrViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EvidenceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EvidenceViewModel(application, evidenceRepository, authViewModel, caseViewModel, ocrViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
