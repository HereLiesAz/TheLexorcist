package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.CaseRepository

class CaseViewModelFactory(
    private val application: Application,
    private val caseRepository: CaseRepository,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CaseViewModel(application, caseRepository, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
