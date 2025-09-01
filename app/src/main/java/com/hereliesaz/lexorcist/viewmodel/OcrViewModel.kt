package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
// import android.widget.Toast // Removed as service handles logging
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// import com.hereliesaz.lexorcist.data.EvidenceRepository // Removed
// import com.hereliesaz.lexorcist.data.SettingsManager // Removed
// import com.hereliesaz.lexorcist.service.ScriptRunner // Removed
import com.hereliesaz.lexorcist.service.OcrProcessingService // Added
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val application: Application, // Keep application for AndroidViewModel
    private val ocrProcessingService: OcrProcessingService // Injected the new service
    // Removed:
    // private val evidenceRepository: EvidenceRepository,
    // private val settingsManager: SettingsManager,
    // private val scriptRunner: ScriptRunner
) : AndroidViewModel(application) {

    fun performOcrOnUri(
        uri: Uri,
        context: Context, // Context might still be needed by service, or passed if service gets it from its own DI
        caseId: Int,
        spreadsheetId: String, // Added: spreadsheetId is required by the service
        parentVideoId: String?
    ) {
        viewModelScope.launch {
            Log.d("OcrViewModel", "Delegating performOcrOnUri to OcrProcessingService for URI: $uri, caseId: $caseId")
            try {
                ocrProcessingService.processImageFrame(
                    uri = uri,
                    context = context, // Pass context if needed by service; appContext if Application context is fine
                    caseId = caseId,
                    spreadsheetId = spreadsheetId,
                    parentVideoId = parentVideoId
                )
                // Optionally: Update UI state here to indicate success
                // Log.d("OcrViewModel", "OcrProcessingService completed for URI: $uri")
            } catch (e: Exception) {
                Log.e("OcrViewModel", "Error calling OcrProcessingService for URI: $uri", e)
                // Optionally: Update UI state here to indicate error
                // Toast.makeText(getApplication(), "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ... any other existing code in your OcrViewModel
}
