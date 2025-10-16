package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class VideoProcessingWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val videoProcessingService: VideoProcessingService,
    private val evidenceRepository: com.hereliesaz.lexorcist.data.EvidenceRepository,
    private val logService: LogService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val videoUriString = inputData.getString(KEY_VIDEO_URI)
        val caseId = inputData.getInt(KEY_CASE_ID, -1)
        val caseName = inputData.getString(KEY_CASE_NAME)
        val spreadsheetId = inputData.getString(KEY_SPREADSHEET_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                val errorMsg = "READ_MEDIA_VIDEO permission not granted"
                Log.e(TAG, errorMsg)
                val outputData = Data.Builder().putString(RESULT_FAILURE, errorMsg).build()
                return Result.failure(outputData)
            }
        } else {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val errorMsg = "READ_EXTERNAL_STORAGE permission not granted"
                Log.e(TAG, errorMsg)
                val outputData = Data.Builder().putString(RESULT_FAILURE, errorMsg).build()
                return Result.failure(outputData)
            }
        }

        if (videoUriString.isNullOrEmpty() || caseId == -1 || caseName.isNullOrEmpty() || spreadsheetId.isNullOrEmpty()) {
            val errorMsg = "Invalid input data: videoUriString=$videoUriString, caseId=$caseId, caseName=$caseName, spreadsheetId=$spreadsheetId"
            Log.e(TAG, errorMsg)
            val outputData = Data.Builder().putString(RESULT_FAILURE, errorMsg).build()
            return Result.failure(outputData)
        }

        val videoUri = videoUriString.toUri()

        try {
            val aggregatedText = videoProcessingService.processVideo(videoUri)

            if (aggregatedText.isNotBlank()) {
                val newEvidence = com.hereliesaz.lexorcist.data.Evidence(
                    caseId = caseId.toLong(),
                    spreadsheetId = spreadsheetId,
                    type = "video",
                    content = aggregatedText,
                    formattedContent = aggregatedText,
                    mediaUri = videoUriString,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = "Video Upload",
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    allegationElementName = null,
                    category = "Video",
                    tags = listOf("video", "transcription", "ocr")
                )
                evidenceRepository.addEvidence(newEvidence)
                val successMsg = "Video processed successfully."
                logService.addLog(successMsg)
                val outputData = Data.Builder().putString(RESULT_SUCCESS, successMsg).build()
                return Result.success(outputData)
            } else {
                val errorMsg = "Video processing finished but no text was extracted."
                logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
                val outputData = Data.Builder().putString(RESULT_FAILURE, errorMsg).build()
                return Result.failure(outputData)
            }

        } catch (e: Exception) {
            val errorMsg = "Video processing failed: ${e.message}"
            logService.addLog(errorMsg, com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            Log.e(TAG, errorMsg, e)
            val outputData = Data.Builder().putString(RESULT_FAILURE, errorMsg).build()
            return Result.failure(outputData)
        }
    }

    companion object {
        const val KEY_VIDEO_URI = "KEY_VIDEO_URI"
        const val KEY_CASE_ID = "KEY_CASE_ID"
        const val KEY_CASE_NAME = "KEY_CASE_NAME"
        const val KEY_SPREADSHEET_ID = "KEY_SPREADSHEET_ID"
        
        const val PROGRESS_PERCENT = "PROGRESS_PERCENT"
        const val PROGRESS_MESSAGE = "PROGRESS_MESSAGE"
        
        const val RESULT_SUCCESS = "RESULT_SUCCESS"
        const val RESULT_FAILURE = "RESULT_FAILURE"

        private const val TAG = "VideoProcessingWorker"
    }
}
