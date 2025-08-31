package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating a [TranscriptionViewModel].
 * This factory is necessary because TranscriptionViewModel is a plain AndroidViewModel
 * and not a HiltViewModel.
 */
@Suppress("UNCHECKED_CAST")
class TranscriptionViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TranscriptionViewModel::class.java) -> {
                TranscriptionViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class. This factory only creates TranscriptionViewModel.")
        }
    }
}
