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
import com.hereliesaz.lexorcist.model.VideoMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProcessingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val evidenceRepository: EvidenceRepository,
    private val ocrProcessingService: OcrProcessingService,
    private val transcriptionService: TranscriptionService,
    private val logService: LogService
) {

    suspend fun processVideo(
        videoUri: Uri,
        caseId: Int,
        caseName: String,
        spreadsheetId: String,
        googleApiService: GoogleApiService,
        onProgress: (String) -> Unit
    ): Evidence {
        onProgress("Starting video processing...")
        logService.addLog("Processing video: $videoUri")

        val metadata = extractVideoMetadata(videoUri)

        onProgress("Extracting audio...")
        logService.addLog("Extracting audio...")
        val audioUri = extractAudio(videoUri)
        val (audioTranscript, error) = if (audioUri != null) {
            transcriptionService.transcribeAudio(audioUri)
        } else {
            Pair("Audio could not be extracted.", "Audio could not be extracted.")
        }

        if (error != null) {
            logService.addLog("Transcription failed: $error", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
        }

        onProgress("Extracting frames...")
        logService.addLog("Extracting frames...")
        val frameUris = extractKeyframes(videoUri)
        val ocrTextBuilder = StringBuilder()
        if (frameUris.isNotEmpty()) {
            logService.addLog("Extracted ${frameUris.size} keyframes")
            frameUris.forEachIndexed { index, uri ->
                val progressMessage = "Processing frame ${index + 1} of ${frameUris.size}..."
                onProgress(progressMessage)
                logService.addLog(progressMessage)
                val frameEvidence = ocrProcessingService.processImageFrame(
                    uri = uri,
                    context = context,
                    caseId = caseId,
                    spreadsheetId = spreadsheetId,
                    parentVideoId = null,
                )
                if (frameEvidence != null) {
                    logService.addLog("Found ${frameEvidence.content.length} characters in frame ${index + 1}")
                    ocrTextBuilder.append(frameEvidence.content).append("\n\n")
                } else {
                    logService.addLog("No evidence created for frame ${index + 1}", com.hereliesaz.lexorcist.model.LogLevel.DEBUG)
                }
            }
        }

        val combinedContent = "Audio Transcript:\n${audioTranscript ?: "Transcription failed."}\n\nOCR from Frames:\n$ocrTextBuilder"

        val videoEvidence =
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
                documentDate = System.currentTimeMillis(),
                allegationId = null,
                category = "Video Evidence",
                tags = listOf("video", "transcription", "ocr"),
                commentary = null,
                parentVideoId = null,
                entities = emptyMap(),
                audioTranscript = audioTranscript,
                videoOcrText = ocrTextBuilder.toString(),
                duration = metadata?.duration
            )
        evidenceRepository.addEvidence(videoEvidence)

        onProgress("Video processing complete.")
        return videoEvidence
    }

    suspend fun extractVideoMetadata(uri: Uri): VideoMetadata? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat() ?: 0f
                VideoMetadata(duration, width, height, frameRate)
            } catch (e: Exception) {
                Log.e("VideoProcessingService", "Error extracting metadata", e)
                null
            } finally {
                retriever.release()
            }
        }
    }

    suspend fun extractAudio(videoUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val outputFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
            var muxer: MediaMuxer? = null

            try {
                extractor.setDataSource(context, videoUri, null)
                val audioTrackIndex = findAudioTrack(extractor)
                if (audioTrackIndex == -1) {
                    Log.e("VideoProcessingService", "No audio track found in video")
                    return@withContext null
                }

                extractor.selectTrack(audioTrackIndex)
                val format = extractor.getTrackFormat(audioTrackIndex)

                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(format)
                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        break
                    }
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                Uri.fromFile(outputFile)
            } catch (e: Exception) {
                Log.e("VideoProcessingService", "Error extracting audio", e)
                null
            } finally {
                muxer?.stop()
                muxer?.release()
                extractor.release()
            }
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    suspend fun extractKeyframes(videoUri: Uri, intervalUs: Long = 5 * 1000 * 1000L): List<Uri> =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            val frameUris = mutableListOf<Uri>()
            try {
                retriever.setDataSource(context, videoUri)
                val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationString?.toLongOrNull() ?: 0L

                for (timeUs in 0L until durationMs * 1000L step intervalUs) {
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        val outputFile = File(context.cacheDir, "frame_${System.currentTimeMillis()}.jpg")
                        try {
                            outputFile.outputStream().use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            frameUris.add(Uri.fromFile(outputFile))
                        } catch (e: IOException) {
                            Log.e("VideoProcessingService", "Error saving frame", e)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoProcessingService", "Error extracting keyframes", e)
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    Log.e("VideoProcessingService", "Error releasing MediaMetadataRetriever", e)
                }
            }
            frameUris
        }
}
