package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TranscriptionViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TranscriptionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
