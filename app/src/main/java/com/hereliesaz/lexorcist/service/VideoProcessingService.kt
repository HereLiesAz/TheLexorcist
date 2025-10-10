package com.hereliesaz.lexorcist.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.LogLevel // Assuming this is your custom LogLevel
import com.hereliesaz.lexorcist.model.VideoMetadata
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProcessingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val evidenceRepository: EvidenceRepository,
    private val ocrProcessingService: OcrProcessingService,
    private val transcriptionService: TranscriptionService,
    private val logService: LogService,
    private val scriptRunner: ScriptRunner,
    private val settingsManager: SettingsManager
) {

    // Pair to hold VideoMetadata and parsed creation time
    private data class ExtractedMetadata(val videoMetadata: VideoMetadata, val parsedCreationTime: Long?)

    suspend fun processVideo(
        videoUri: Uri,
        caseId: Int,
        caseName: String,
        spreadsheetId: String,
        onProgress: (percent: Float, message: String) -> Unit
    ): Evidence? { 
        onProgress(0.0f, "Starting video processing...")
        logService.addLog("Processing video: $videoUri for case $caseName ($spreadsheetId)")

        val extractedMetadata = extractVideoMetadata(videoUri)
        val metadata = extractedMetadata?.videoMetadata
        val documentTimestamp = extractedMetadata?.parsedCreationTime ?: System.currentTimeMillis()
        onProgress(0.05f, "Video metadata extracted.")

        logService.addLog("Extracting audio from $videoUri")
        val audioUri = extractAudio(videoUri)
        onProgress(0.15f, "Audio extraction attempt complete.")

        val transcriptionResult = transcriptionService.transcribeAudio(videoUri)
        val audioTranscript = when (transcriptionResult) {
            is Result.Success -> {
                logService.addLog("Transcription successful: ${transcriptionResult.data.take(100)}...")
                onProgress(0.40f, "Audio transcription complete.")
                transcriptionResult.data
            }
            is Result.Error -> {
                val transcriptionError = transcriptionResult.exception.message ?: "Unknown transcription error"
                logService.addLog("Transcription failed: $transcriptionError", LogLevel.ERROR)
                onProgress(0.40f, "Audio transcription failed.")
                null
            }
            else -> {
                logService.addLog("Transcription in unknown state.", LogLevel.WARNING)
                onProgress(0.40f, "Audio transcription failed.")
                null
            }
        }

        onProgress(0.45f, "Extracting frames for OCR...")
        logService.addLog("Extracting frames from $videoUri")
        val frameUris = com.hereliesaz.lexorcist.utils.VideoUtils.extractFrames(context, videoUri)
        val ocrTextBuilder = StringBuilder()
        if (frameUris.isNotEmpty()) {
            logService.addLog("Extracted ${frameUris.size} keyframes for OCR.")
            val totalFrames = frameUris.size
            frameUris.forEachIndexed { index, frameUri ->
                val frameProgress = 0.45f + (0.45f * (index + 1) / totalFrames)
                val progressMessage = "Processing frame ${index + 1} of $totalFrames for OCR..."
                onProgress(frameProgress, progressMessage)
                logService.addLog(progressMessage)
                val ocrResult = ocrProcessingService.processImageFrame(
                    uri = frameUri,
                    context = context,
                    caseId = caseId,
                    spreadsheetId = spreadsheetId,
                    parentVideoId = null // This will be set later when the main evidence is created
                )
                if (ocrResult != null && ocrResult.content.isNotBlank()) {
                    logService.addLog("OCR for frame ${index + 1} found ${ocrResult.content.length} characters.")
                    ocrTextBuilder.append(ocrResult.content).append("\n\n")
                } else {
                    logService.addLog("No text found or error in OCR for frame ${index + 1}. URI: $frameUri", LogLevel.DEBUG)
                }
                frameUri.path?.let { File(it).delete() }
            }
        } else {
            logService.addLog("No keyframes extracted for OCR from $videoUri")
        }
        onProgress(0.90f, "Frame OCR complete.")

        val videoOcrText = ocrTextBuilder.toString().trim()
        val combinedContent = "Audio Transcript:\n${audioTranscript ?: "Transcription failed."}\n\nOCR from Frames:\n${if (videoOcrText.isNotEmpty()) videoOcrText else "No text extracted from frames."}"
        val fileHash = com.hereliesaz.lexorcist.utils.HashingUtils.getHash(context, videoUri)

        var videoEvidence =
            Evidence(
                id = 0,
                caseId = caseId.toLong(),
                spreadsheetId = spreadsheetId,
                type = "video",
                content = combinedContent,
                formattedContent = "```\n$combinedContent\n```",
                mediaUri = videoUri.toString(),
                timestamp = System.currentTimeMillis(),
                sourceDocument = videoUri.toString(),
                documentDate = documentTimestamp, // Use parsed creation time or fallback
                allegationId = null,
                allegationElementName = null, // Added
                category = "Video Evidence",
                tags = listOfNotNull(
                    "video",
                    if (audioTranscript?.isNotBlank() == true) "transcription" else null,
                    if (videoOcrText.isNotBlank()) "ocr" else null,
                    if ((audioTranscript == null || audioTranscript.isBlank()) && videoOcrText.isBlank()) "non-textual" else null
                ),
                commentary = null,
                parentVideoId = null,
                entities = emptyMap<String, List<String>>(), // TODO: Implement entity parsing for combined video content
                audioTranscript = audioTranscript,
                videoOcrText = videoOcrText.ifEmpty { null },
                duration = metadata?.duration,
                fileHash = fileHash
            )
        val script = settingsManager.getScript()
        var evidenceToSave = videoEvidence
        if (script.isNotBlank()) {
            logService.addLog("Running script on video evidence...")
            onProgress(0.92f, "Running script...")
            val scriptResult = scriptRunner.runScript(script, evidenceToSave)
            if (scriptResult is Result.Success) {
                val currentTagsVideo: List<String> = evidenceToSave.tags
                val newTagsFromScriptVideo: List<String> = scriptResult.data.tags
                logService.addLog("Script finished. Added tags: ${newTagsFromScriptVideo.joinToString(", ")}")
                val combinedTagsVideo: List<String> = (currentTagsVideo + newTagsFromScriptVideo).distinct()
                evidenceToSave = evidenceToSave.copy(tags = combinedTagsVideo)
            } else if (scriptResult is Result.Error) {
                logService.addLog("Script error: ${scriptResult.exception.message}", LogLevel.ERROR)
            }
        }

        onProgress(0.95f, "Saving video evidence...")
        val savedEvidence = evidenceRepository.addEvidence(evidenceToSave)
        if (savedEvidence == null) {
            logService.addLog("Failed to save main video evidence for $videoUri", LogLevel.ERROR)
            onProgress(1.0f, "Failed to save video evidence.")
            return null
        }

        // Now that the main evidence is saved and has an ID, update the parentVideoId of the frame evidences
        val finalEvidence = savedEvidence.copy(parentVideoId = savedEvidence.id.toString())
        evidenceRepository.updateEvidence(finalEvidence)


        logService.addLog("Video processing complete for $videoUri. Evidence ID: ${finalEvidence.id}")
        onProgress(1.0f, "Video processing complete.")
        return finalEvidence
    }

    private suspend fun extractVideoMetadata(uri: Uri): ExtractedMetadata? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 0f
                val creationTimeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                
                val parsedCreationTime = creationTimeString?.let {
                    // Try to parse different date formats from metadata
                    val dateFormats = listOf(
                        SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US),
                        SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US),
                        SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US) 
                    )
                    dateFormats.forEach { sdf -> 
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        try {
                            return@let sdf.parse(it)?.time
                        } catch (e: java.text.ParseException) {
                            // Continue to next format
                        }
                    }
                    Log.w("VideoProcessingService", "Could not parse date from metadata: $creationTimeString")
                    null // Return null if all formats fail
                }

                ExtractedMetadata(VideoMetadata(duration, width, height, frameRate), parsedCreationTime)
            } catch (e: Exception) {
                Log.e("VideoProcessingService", "Error extracting metadata for $uri", e)
                null
            } finally {
                try {
                    retriever.release()
                } catch (re: RuntimeException) {
                     Log.e("VideoProcessingService", "Error releasing MediaMetadataRetriever for $uri", re)
                }
            }
        }
    }

    private suspend fun extractAudio(videoUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val outputFile = File(context.cacheDir, "audio_ext_${System.currentTimeMillis()}.m4a")
            var muxer: MediaMuxer? = null
            var success = false

            try {
                extractor.setDataSource(context, videoUri, null)
                val audioTrackIndex = findAudioTrack(extractor)
                if (audioTrackIndex == -1) {
                    logService.addLog("No audio track found in video: $videoUri", LogLevel.INFO) // Changed to INFO
                    return@withContext null
                }

                extractor.selectTrack(audioTrackIndex)
                val format = extractor.getTrackFormat(audioTrackIndex)

                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(format)
                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()
                var sawEOS = false

                while (!sawEOS) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)

                    if (bufferInfo.size < 0) {
                        logService.addLog("Saw end of stream for audio extraction from $videoUri.", LogLevel.DEBUG)
                        sawEOS = true
                        bufferInfo.size = 0
                    } else {
                        bufferInfo.presentationTimeUs = extractor.sampleTime
                        bufferInfo.flags = extractor.sampleFlags 
                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                        extractor.advance()
                    }
                }
                success = true
                logService.addLog("Audio successfully extracted from $videoUri to ${outputFile.absolutePath}")
                Uri.fromFile(outputFile)
            } catch (e: Exception) {
                logService.addLog("Error extracting audio from $videoUri: ${e.message}", LogLevel.ERROR)
                Log.e("VideoProcessingService", "Error extracting audio from $videoUri", e)
                outputFile.delete()
                null
            } finally {
                try {
                    muxer?.stop()
                    muxer?.release()
                } catch (e: Exception) {
                    logService.addLog("Error stopping/releasing muxer for audio extraction from $videoUri: ${e.message}", LogLevel.INFO) // Changed to INFO
                }
                try {
                    extractor.release()
                } catch (e: Exception) {
                    logService.addLog("Error releasing extractor for audio extraction from $videoUri: ${e.message}", LogLevel.INFO) // Changed to INFO
                }
                if (!success && outputFile.exists()) {
                    outputFile.delete()
                }
            }
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                logService.addLog("Audio track found at index $i with MIME: $mime")
                return i
            }
        }
        logService.addLog("No audio track found after checking ${extractor.trackCount} tracks.")
        return -1
    }

}
