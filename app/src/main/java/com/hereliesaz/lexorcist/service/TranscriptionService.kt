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
    suspend fun transcribeAudio(uri: Uri): Result<String>

    /**
     * Transcribes the audio from the given video URI.
     * The default implementation returns an error, and must be overridden by services that support video.
     * @param uri The URI of the video file to transcribe.
     * @return A Result containing the transcribed text as a String, or an error.
     */
    suspend fun transcribeVideo(uri: Uri): Result<String> {
        return Result.Error(UnsupportedOperationException("Video transcription not supported by this service."))
    }
}
