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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
// Removed: import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.EvidenceRepository // Keep this specific import
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import com.google.api.services.drive.model.File as DriveFile
import com.hereliesaz.lexorcist.utils.Result as LexResult

@HiltWorker
class VideoProcessingWorker
    @AssistedInject
    constructor(
        @Assisted private val appContext: Context, // Made private val as it's used in methods
        @Assisted workerParams: WorkerParameters,
        private val evidenceRepository: EvidenceRepository,
        private val ocrProcessingService: OcrProcessingService,
        private val transcriptionService: TranscriptionService,
        private val googleApiService: GoogleApiService, // Injected GoogleApiService
        private val logService: LogService,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val videoUriString = inputData.getString(KEY_VIDEO_URI)
            val caseId = inputData.getInt(KEY_CASE_ID, -1)
            val caseName = inputData.getString(KEY_CASE_NAME)
            val spreadsheetId = inputData.getString(KEY_SPREADSHEET_ID) // Added

            if (videoUriString.isNullOrEmpty() || caseId == -1 || caseName.isNullOrEmpty() || spreadsheetId.isNullOrEmpty()) {
                Log.e(
                    TAG,
                    "Invalid input data: videoUriString=$videoUriString, caseId=$caseId, caseName=$caseName, spreadsheetId=$spreadsheetId",
                )
                return Result.failure()
            }

            val videoUri = Uri.parse(videoUriString)
            logService.addLog("Processing video: $videoUri")
            setProgressAsync(Data.Builder().putString(PROGRESS, "Starting video processing...").build())

            logService.addLog("Copying video to raw evidence folder...")
            val localUploadResult = evidenceRepository.uploadFile(videoUri, caseName, spreadsheetId)
            if (localUploadResult is LexResult.Error) {
                logService.addLog("Failed to copy video to local storage: ${localUploadResult.exception.message}")
                return Result.failure()
            }
            val videoPath = (localUploadResult as LexResult.Success).data
            val videoFile = File(videoPath)
            setProgressAsync(Data.Builder().putString(PROGRESS, "Raw evidence file saved.").build())
            logService.addLog("Video copied to raw evidence folder.")

            var uploadedDriveFile: DriveFile? = null
            // Use the directly injected googleApiService
            // The null check for googleApiService is removed as it's now directly injected.
            // If GoogleApiService cannot operate (e.g., no credentials), its methods should handle that.
            logService.addLog("Uploading video to Google Drive...")
            val evidenceFolderId = googleApiService.getOrCreateEvidenceFolder(caseName)
            if (evidenceFolderId != null) {
                when (val uploadResult = googleApiService.uploadFile(videoFile, evidenceFolderId, "video/mp4")) {
                    is LexResult.Success -> {
                        uploadedDriveFile = uploadResult.data
                        logService.addLog("Video uploaded to Drive with ID: ${uploadedDriveFile?.id}")
                    }
                    is LexResult.Error -> {
                        logService.addLog("Failed to upload video: ${uploadResult.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                    }
                    is LexResult.UserRecoverableError -> {
                        logService.addLog("User recoverable error while uploading video: ${uploadResult.exception.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                    }
                }
            } else {
                logService.addLog("Failed to get or create evidence folder for case: $caseName", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            }

            setProgressAsync(Data.Builder().putString(PROGRESS, "Extracting audio...").build())
            logService.addLog("Extracting audio...")
            val audioUri = extractAudio(videoUri)
            val (transcript, error) = if (audioUri != null) {
                transcriptionService.transcribeAudio(audioUri)
            } else {
                Pair(
                    "Audio could not be extracted.",
                    "Audio could not be extracted."
                )
            }

            if (error != null) {
                logService.addLog("Transcription failed: $error", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            }

            setProgressAsync(Data.Builder().putString(PROGRESS, "Extracting frames...").build())
            logService.addLog("Extracting frames...")
            val frameUris = extractKeyframes(videoUri)
            val ocrTextBuilder = StringBuilder()
            if (frameUris.isNotEmpty()) {
                logService.addLog("Extracted ${frameUris.size} keyframes")
                frameUris.forEachIndexed { index, uri ->
                    val progressMessage = "Processing frame ${index + 1} of ${frameUris.size}..."
                    setProgressAsync(Data.Builder().putString(PROGRESS, progressMessage).build())
                    logService.addLog(progressMessage)
                    val frameEvidence = ocrProcessingService.processImageFrame(
                        uri = uri,
                        context = appContext,
                        caseId = caseId, // Removed .toLong()
                        spreadsheetId = spreadsheetId,
                        parentVideoId = uploadedDriveFile?.id,
                    )
                    if (frameEvidence != null) {
                        logService.addLog("Found ${frameEvidence.content.length} characters in frame ${index + 1}")
                        ocrTextBuilder.append(frameEvidence.content).append("\n\n")
                    } else {
                        logService.addLog("No evidence created for frame ${index + 1}", com.hereliesaz.lexorcist.model.LogLevel.DEBUG)
                    }
                }
            }

            val combinedContent = "Audio Transcript:\n$transcript\n\nOCR from Frames:\n$ocrTextBuilder"

            val videoEvidence =
                com.hereliesaz.lexorcist.data.Evidence(
                    id = 0,
                    caseId = caseId.toLong(), // This is for Evidence data class, may need to be Int too
                    spreadsheetId = spreadsheetId,
                    type = "video",
                    content = combinedContent.toString(),
                    formattedContent = "```\n$combinedContent\n```",
                    mediaUri = videoUri.toString(),
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = uploadedDriveFile?.webViewLink ?: videoUri.toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    category = "Video Evidence",
                    tags = listOf("video", "transcription", "ocr"),
                    commentary = null,
                    parentVideoId = null,
                    entities = emptyMap(),
                )
            evidenceRepository.addEvidence(videoEvidence)

            return Result.success()
        }

        private suspend fun extractAudio(videoUri: Uri): Uri? {
            return withContext(Dispatchers.IO) {
                val extractor = MediaExtractor()
                val outputFile = File(appContext.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                var muxer: MediaMuxer? = null

                try {
                    extractor.setDataSource(appContext, videoUri, null)
                    val audioTrackIndex = findAudioTrack(extractor)
                    if (audioTrackIndex == -1) {
                        Log.e(TAG, "No audio track found in video")
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
                        bufferInfo.flags =
                            if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                MediaCodec.BUFFER_FLAG_KEY_FRAME
                            } else {
                                0
                            }

                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                        extractor.advance()
                    }

                    Uri.fromFile(outputFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting audio", e)
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

        private suspend fun extractKeyframes(videoUri: Uri): List<Uri> =
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                val frameUris = mutableListOf<Uri>()
                try {
                    retriever.setDataSource(appContext, videoUri)
                    val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationString?.toLongOrNull() ?: 0L // Ensure default to Long
                    val intervalUs = 5 * 1000 * 1000L // 5 seconds

                    for (timeUs in 0L until durationMs * 1000L step intervalUs) { // Ensure timeUs is Long
                        val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bitmap != null) {
                            val outputFile = File(appContext.cacheDir, "frame_${System.currentTimeMillis()}.jpg")
                            outputFile.outputStream().use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            frameUris.add(Uri.fromFile(outputFile))
                            bitmap.recycle() // Important to recycle bitmap
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting keyframes", e)
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
                    }
                }
                frameUris
            }

        companion object {
            const val KEY_VIDEO_URI = "KEY_VIDEO_URI"
            const val KEY_CASE_ID = "KEY_CASE_ID"
            const val KEY_CASE_NAME = "KEY_CASE_NAME"
            const val KEY_SPREADSHEET_ID = "KEY_SPREADSHEET_ID" // Added key
            const val PROGRESS = "PROGRESS"
            private const val TAG = "VideoProcessingWorker"
        }
    }
