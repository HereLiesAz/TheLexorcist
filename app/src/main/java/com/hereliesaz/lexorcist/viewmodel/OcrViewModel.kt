package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hereliesaz.lexorcist.service.OcrProcessingService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OcrViewModel
@Inject
constructor(
    private val application: Application,
    private val ocrProcessingService: OcrProcessingService,
) : AndroidViewModel(application) {
    // This ViewModel is currently not used in the main evidence processing flow.
    // The image processing is initiated from EvidenceViewModel.
    // The performOcrOnUri function has been removed to fix compilation errors.
}
