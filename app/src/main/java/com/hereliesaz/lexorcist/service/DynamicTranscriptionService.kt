package com.hereliesaz.lexorcist.service

import android.net.Uri
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DynamicTranscriptionService @Inject constructor(
    private val settingsManager: SettingsManager,
    private val voskService: VoskTranscriptionService,
    private val whisperService: WhisperTranscriptionService
) : TranscriptionService {

    private fun getCurrentService(): TranscriptionService {
        return when (settingsManager.getTranscriptionService()) {
            "Whisper" -> whisperService
            // Default to Vosk if the setting is anything else or not set
            else -> voskService
        }
    }

    // Note: If the setting can change while a Flow is being collected,
    // this implementation of processingState might not reflect the change immediately
    // for an ongoing collection. However, new collections will get the updated service's flow.
    // For many use cases, this is sufficient. A more complex solution using flatMapLatest
    // on a flow of the setting itself could be used if needed.
    override val processingState: Flow<ProcessingState>
        get() = getCurrentService().processingState

    override suspend fun start(uri: Uri) {
        getCurrentService().start(uri)
    }

    override fun stop() {
        getCurrentService().stop()
    }

    override suspend fun transcribeAudio(uri: Uri): Result<String> {
        return getCurrentService().transcribeAudio(uri)
    }
}
