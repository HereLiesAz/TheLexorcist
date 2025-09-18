package com.hereliesaz.lexorcist.service

import android.net.Uri
import com.hereliesaz.lexorcist.model.ProcessingState
import kotlinx.coroutines.flow.Flow

interface TranscriptionService {
    val processingState: Flow<ProcessingState>
    suspend fun start(uri: Uri)
    fun stop()
}
