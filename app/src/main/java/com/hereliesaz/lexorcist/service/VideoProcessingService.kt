package com.hereliesaz.lexorcist.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.tracing.trace
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoProcessingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrProcessingService: OcrProcessingService,
    private val whisperTranscriptionService: WhisperTranscriptionService,
    private val dispatcherProvider: DispatcherProvider
) {

    suspend fun processVideo(uri: Uri): String = trace("processVideo") {
        return withContext(dispatcherProvider.io) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationString?.toLongOrNull() ?: 0

            val ocrText = StringBuilder()
            var currentTime = 0L
            while (currentTime < duration) {
                val frame = retriever.getFrameAtTime(currentTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val text = ocrProcessingService.processBitmap(frame)
                    if (text.isNotBlank()) {
                        ocrText.append(text).append("\n")
                    }
                }
                currentTime += 5000 // 5 seconds
            }

            val transcription = whisperTranscriptionService.transcribeVideo(uri)

            retriever.release()

            val aggregatedText = "## OCR Text\n$ocrText\n## Transcription\n$transcription"
            aggregatedText
        }
    }
}
