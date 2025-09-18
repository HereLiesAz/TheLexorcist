package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
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
    private val googleApiService: GoogleApiService,
    private val logService: LogService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val videoUriString = inputData.getString(KEY_VIDEO_URI)
        val caseId = inputData.getInt(KEY_CASE_ID, -1)
        val caseName = inputData.getString(KEY_CASE_NAME)
        val spreadsheetId = inputData.getString(KEY_SPREADSHEET_ID)

        if (videoUriString.isNullOrEmpty() || caseId == -1 || caseName.isNullOrEmpty() || spreadsheetId.isNullOrEmpty()) {
            Log.e(
                TAG,
                "Invalid input data: videoUriString=$videoUriString, caseId=$caseId, caseName=$caseName, spreadsheetId=$spreadsheetId"
            )
            return Result.failure()
        }

        val videoUri = Uri.parse(videoUriString)

        try {
            videoProcessingService.processVideo(
                videoUri = videoUri,
                caseId = caseId,
                caseName = caseName,
                spreadsheetId = spreadsheetId,
                googleApiService = googleApiService
            ) { progress ->
                setProgressAsync(Data.Builder().putString(PROGRESS, progress).build())
            }
        } catch (e: Exception) {
            logService.addLog("Video processing failed: ${e.message}", com.hereliesaz.lexorcist.model.LogLevel.ERROR)
            Log.e(TAG, "Video processing failed", e)
            return Result.failure()
        }

        return Result.success()
    }

    companion object {
        const val KEY_VIDEO_URI = "KEY_VIDEO_URI"
        const val KEY_CASE_ID = "KEY_CASE_ID"
        const val KEY_CASE_NAME = "KEY_CASE_NAME"
        const val KEY_SPREADSHEET_ID = "KEY_SPREADSHEET_ID"
        const val PROGRESS = "PROGRESS"
        private const val TAG = "VideoProcessingWorker"
    }
}
