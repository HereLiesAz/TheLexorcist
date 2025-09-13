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
import androidx.work.WorkerParameters
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.OcrProcessingService
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
        private val ocrProcessingService: OcrProcessingService, // Changed from OcrViewModel
        private val credentialHolder: CredentialHolder,
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
            Log.d(TAG, "Processing video: $videoUri for case: $caseName ($caseId), spreadsheetId: $spreadsheetId")

            val videoFile = File(applicationContext.cacheDir, "video_${System.currentTimeMillis()}.mp4")
            applicationContext.contentResolver.openInputStream(videoUri)?.use { input ->
                videoFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            var uploadedDriveFile: DriveFile? = null
            val googleApiService = credentialHolder.googleApiService // Get from CredentialHolder
            if (googleApiService == null) {
                Log.e(TAG, "GoogleApiService is not available. Skipping Drive upload.")
            } else {
                val evidenceFolderId = googleApiService.getOrCreateEvidenceFolder(caseName)
                if (evidenceFolderId != null) {
                    when (val uploadResult = googleApiService.uploadFile(videoFile, evidenceFolderId, "video/mp4")) {
                        is LexResult.Success -> {
                            uploadedDriveFile = uploadResult.data
                            Log.d(TAG, "Video uploaded to Drive with ID: ${uploadedDriveFile?.id}")
                        }
                        is LexResult.Error -> {
                            Log.e(TAG, "Failed to upload video: ${uploadResult.exception.message}")
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get or create evidence folder for case: $caseName")
                }
            }

            val audioUri = extractAudio(videoUri)
            if (audioUri == null) {
                Log.e(TAG, "Failed to extract audio")
            } else {
                Log.d(TAG, "Audio extracted to: $audioUri")
                // TODO: Pass audioUri to the speech-to-text pipeline (TranscriptionService)
            }

            val frameUris = extractKeyframes(videoUri)
            if (frameUris.isNotEmpty()) {
                Log.d(TAG, "Extracted ${frameUris.size} keyframes")
                frameUris.forEach { uri ->
                    ocrProcessingService.processImageFrame(
                        uri = uri,
                        context = appContext,
                        caseId = caseId,
                        spreadsheetId = spreadsheetId, // Pass spreadsheetId
                        parentVideoId = uploadedDriveFile?.id,
                    )
                }
            }

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
                    val intervalUs = 60 * 1000 * 1000L // 1 minute

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
            private const val TAG = "VideoProcessingWorker"
        }
    }
