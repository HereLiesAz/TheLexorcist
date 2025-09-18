package com.hereliesaz.lexorcist.service

import android.net.Uri
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.Result // Added import for Result
import kotlinx.coroutines.flow.Flow

interface TranscriptionService {
    val processingState: Flow<ProcessingState>
    suspend fun start(uri: Uri) // This might be for continuous transcription or a specific session
    fun stop()

    /**
     * Transcribes the audio from the given URI.
     * @param uri The URI of the audio file to transcribe.
     * @return A Result containing the transcribed text as a String, or an error.
     */
    suspend fun transcribeAudio(uri: Uri): Result<String> // Added transcribeAudio function
}
