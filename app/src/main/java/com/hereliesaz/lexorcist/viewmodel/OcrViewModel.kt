package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.service.OcrProcessingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
    application: Application,
    private val ocrProcessingService: OcrProcessingService
) : AndroidViewModel(application) {

    private val _processingState = MutableStateFlow<ProcessingState?>(null)
    val processingState: StateFlow<ProcessingState?> = _processingState

    fun performOcrOnUri(uri: Uri, context: Context, caseId: Long, spreadsheetId: String) {
        viewModelScope.launch {
            _processingState.value = ProcessingState("Starting OCR...", 0)
            ocrProcessingService.processImage(uri, context, caseId, spreadsheetId) { state ->
                _processingState.value = state
            }
            _processingState.value = null // Reset state when done
        }
    }
}
