package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.CaseRepositoryImpl
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl

class AuthViewModelFactory(
    private val application: Application,
    private val evidenceRepository: EvidenceRepositoryImpl,
    private val caseRepository: CaseRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application, evidenceRepository, caseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
