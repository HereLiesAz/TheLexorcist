package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import android.app.Application
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import com.hereliesaz.lexorcist.utils.Result as LexResult // Alias for your Result class
import com.google.api.services.drive.model.File as DriveFile // Alias for Google Drive File

// TODO: If VideoProcessingWorker is intended to be a HiltWorker, it needs @HiltWorker and @AssistedInject constructor.
// For now, manually instantiating OcrViewModel and GoogleApiService.
class VideoProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val ocrViewModel by lazy {
        val evidenceRepository = EvidenceRepositoryImpl(null)
        val settingsManager = SettingsManager(applicationContext)
        val scriptRunner = ScriptRunner()
        OcrViewModel(
            applicationContext as Application,
            evidenceRepository,
            settingsManager,
            scriptRunner
        )
    }

    override suspend fun doWork(): Result {
        val videoUriString = inputData.getString(KEY_VIDEO_URI)
        val caseId = inputData.getInt(KEY_CASE_ID, -1)
        val caseName = inputData.getString(KEY_CASE_NAME)

        if (videoUriString.isNullOrEmpty() || caseId == -1 || caseName.isNullOrEmpty()) {
            Log.e(TAG, "Invalid input data")
            return Result.failure()
        }

        val videoUri = Uri.parse(videoUriString)
        Log.d(TAG, "Processing video: $videoUri for case: $caseName ($caseId)")

        // TODO: Get a proper instance of GoogleApiService. This should be injected.
        // For now, this will be null and dependent operations will be skipped.
        val googleApiService: com.hereliesaz.lexorcist.GoogleApiService? = null 

        val videoFile = File(applicationContext.cacheDir, "video_${System.currentTimeMillis()}.mp4")
        applicationContext.contentResolver.openInputStream(videoUri)?.use { input ->
            videoFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        var uploadedDriveFile: DriveFile? = null
        if (googleApiService != null) {
            val evidenceFolderId = googleApiService.getOrCreateEvidenceFolder(caseName)
            if (evidenceFolderId != null) {
                when (val uploadResult = googleApiService.uploadFile(videoFile, evidenceFolderId, "video/mp4")) {
                    is LexResult.Success -> {
                        uploadedDriveFile = uploadResult.data
                        Log.d(TAG, "Video uploaded to Drive with ID: ${uploadedDriveFile?.id}")
                    }
                    is LexResult.Error -> {
                        Log.e(TAG, "Failed to upload video: ${uploadResult.exception.message}")
                        // Decide if this is a fatal error for the worker
                        // return Result.failure() 
                    }
                }
            } else {
                Log.e(TAG, "Failed to get or create evidence folder for case: $caseName")
            }
        } else {
            Log.w(TAG, "GoogleApiService is null, skipping video upload.")
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
                ocrViewModel.performOcrOnUri(uri, applicationContext, caseId, uploadedDriveFile?.id)
            }
        }

        return Result.success()
    }

    private suspend fun extractAudio(videoUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val outputFile = File(applicationContext.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
            var muxer: MediaMuxer? = null

            try {
                extractor.setDataSource(applicationContext, videoUri, null)
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
                    if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        bufferInfo.flags = extractor.sampleFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        bufferInfo.flags = extractor.sampleFlags
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

    private suspend fun extractKeyframes(videoUri: Uri): List<Uri> {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            val frameUris = mutableListOf<Uri>()
            try {
                retriever.setDataSource(applicationContext, videoUri)
                val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationString?.toLongOrNull() ?: 0
                val intervalUs = 60 * 1000 * 1000L // 1 minute

                for (timeUs in 0 until durationMs * 1000 step intervalUs) {
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        val outputFile = File(applicationContext.cacheDir, "frame_${System.currentTimeMillis()}.jpg")
                        outputFile.outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        frameUris.add(Uri.fromFile(outputFile))
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting keyframes", e)
            } finally {
                retriever.release()
            }
            frameUris
        }
    }


    companion object {
        const val KEY_VIDEO_URI = "KEY_VIDEO_URI"
        const val KEY_CASE_ID = "KEY_CASE_ID"
        const val KEY_CASE_NAME = "KEY_CASE_NAME"
        private const val TAG = "VideoProcessingWorker"
    }
}
