package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.service.OcrProcessingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrViewModel
    @Inject
    constructor(
        private val application: Application, // Keep application for AndroidViewModel
        // Injected the new service
        private val ocrProcessingService: OcrProcessingService,
        // Removed:
        // private val evidenceRepository: EvidenceRepository,
        // private val settingsManager: SettingsManager,
        // private val scriptRunner: ScriptRunner
    ) : AndroidViewModel(application) {
        fun performOcrOnUri(
            uri: Uri,
            context: Context,
        ) {
            viewModelScope.launch {
                Log.d("OcrViewModel", "Delegating performOcrOnUri to OcrProcessingService for URI: $uri")
                try {
                    val ocrText = ocrProcessingService.processImageFrame(
                        uri = uri,
                        context = context,
                    )
                    Log.d("OcrViewModel", "OcrProcessingService completed for URI: $uri, result: $ocrText")
                } catch (e: Exception) {
                    Log.e("OcrViewModel", "Error calling OcrProcessingService for URI: $uri", e)
                }
            }
        }

        // ... any other existing code in your OcrViewModel
    }
