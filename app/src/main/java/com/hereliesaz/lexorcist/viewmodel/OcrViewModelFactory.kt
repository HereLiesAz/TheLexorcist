package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner

class OcrViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OcrViewModel::class.java)) {
            val settingsManager = SettingsManager(application)
            val scriptRunner = ScriptRunner()
            @Suppress("UNCHECKED_CAST")
            return OcrViewModel(application, settingsManager, scriptRunner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
