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
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel

class VideoProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // TODO: This is not a good practice. The OCR logic should be extracted into a repository.
    private val ocrViewModel by lazy { OcrViewModel(applicationContext as Application) }

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

        // TODO: Get a proper instance of GoogleApiService
        val googleApiService: com.hereliesaz.lexorcist.GoogleApiService? = null

        val videoFile = File(applicationContext.cacheDir, "video_${System.currentTimeMillis()}.mp4")
        applicationContext.contentResolver.openInputStream(videoUri)?.use { input ->
            videoFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val evidenceFolderId = googleApiService?.getOrCreateEvidenceFolder(caseName)
        val uploadedVideo = evidenceFolderId?.let {
            googleApiService.uploadFile(videoFile, it, "video/mp4")
        }

        if (uploadedVideo == null) {
            Log.e(TAG, "Failed to upload video")
            return Result.failure()
        }

        Log.d(TAG, "Video uploaded to Drive with ID: ${uploadedVideo.id}")


        val audioUri = extractAudio(videoUri)
        if (audioUri == null) {
            Log.e(TAG, "Failed to extract audio")
            // Decide if this is a fatal error
        } else {
            Log.d(TAG, "Audio extracted to: $audioUri")
            // TODO: Pass audioUri to the speech-to-text pipeline
        }

        val frameUris = extractKeyframes(videoUri)
        if (frameUris.isNotEmpty()) {
            Log.d(TAG, "Extracted ${frameUris.size} keyframes")
            frameUris.forEach { uri ->
                ocrViewModel.performOcrOnUri(uri, applicationContext, caseId, uploadedVideo.id)
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
                    bufferInfo.flags = extractor.sampleFlags

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
