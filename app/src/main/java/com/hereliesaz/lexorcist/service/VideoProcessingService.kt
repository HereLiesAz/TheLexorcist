package com.hereliesaz.lexorcist.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoProcessingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrProcessingService: OcrProcessingService,
    private val dispatcherProvider: DispatcherProvider
) {

    companion object {
        private const val FRAME_EXTRACTION_INTERVAL_MS = 5000L
        private const val TAG = "VideoProcessingService"
    }

    suspend fun processVideo(uri: Uri): String = withContext(dispatcherProvider.io()) {
        val retriever = MediaMetadataRetriever()
        val ocrText = StringBuilder()
        try {
            retriever.setDataSource(context, uri)

            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationString?.toLongOrNull() ?: 0

            var currentTime = 0L
            while (currentTime < duration) {
                val frame = retriever.getFrameAtTime(currentTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    try {
                        val text = ocrProcessingService.recognizeTextFromBitmap(frame)
                        if (text.isNotBlank()) {
                            ocrText.append(text).append("\n")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "OCR failed for frame at ${currentTime}ms", e)
                    } finally {
                        frame.recycle()
                    }
                }
                currentTime += FRAME_EXTRACTION_INTERVAL_MS
            }
        } finally {
            retriever.release()
        }

        "## OCR Text\n$ocrText"
    }
}