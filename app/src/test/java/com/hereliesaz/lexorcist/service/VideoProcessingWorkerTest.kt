package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder // Correct import
import com.hereliesaz.lexorcist.data.Evidence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations // For openMocks
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class) // Use MockitoJUnitRunner for @Mock initialization if not using openMocks
class VideoProcessingWorkerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockVideoProcessingService: VideoProcessingService

    @Mock
    private lateinit var mockLogService: LogService

    private lateinit var worker: VideoProcessingWorker

    private val testVideoUri = Uri.parse("file:///test.mp4")
    private val testCaseIdInput = 1 // As an Int, like from worker inputData
    private val testCaseName = "Test Case"
    private val testSpreadsheetId = "test_spreadsheet_id"
    
    // Mock Evidence matching the Evidence.kt data class structure
    private val mockEvidence = Evidence(
        id = 1, // Int
        caseId = testCaseIdInput.toLong(), // Long
        spreadsheetId = testSpreadsheetId,
        type = "video",
        content = "Test content",
        formattedContent = "Test formatted content",
        mediaUri = testVideoUri.toString(),
        timestamp = System.currentTimeMillis(),
        sourceDocument = testVideoUri.toString(),
        documentDate = System.currentTimeMillis(),
        allegationId = null,
        category = "Video Evidence",
        tags = listOf("video"),
        commentary = null,
        linkedEvidenceIds = emptyList(),
        parentVideoId = null,
        entities = emptyMap(), // Corrected to emptyMap()
        isSelected = false,
        transcriptEdits = emptyList(),
        audioTranscript = null,
        videoOcrText = null,
        duration = null
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this) // Initialize @Mock annotated fields

        val inputData = Data.Builder()
            .putString(VideoProcessingWorker.KEY_VIDEO_URI, testVideoUri.toString())
            .putInt(VideoProcessingWorker.KEY_CASE_ID, testCaseIdInput)
            .putString(VideoProcessingWorker.KEY_CASE_NAME, testCaseName)
            .putString(VideoProcessingWorker.KEY_SPREADSHEET_ID, testSpreadsheetId)
            .build()

        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                return if (workerClassName == VideoProcessingWorker::class.java.name) {
                    VideoProcessingWorker(
                        appContext,
                        workerParameters,
                        mockVideoProcessingService,
                        mockLogService
                    )
                } else null
            }
        }

        worker = TestListenableWorkerBuilder<VideoProcessingWorker>(mockContext)
            .setInputData(inputData)
            .setWorkerFactory(workerFactory)
            .build()
    }

    @Test
    fun `doWork when video processing is successful returns success`() = runTest {
        whenever(
            mockVideoProcessingService.processVideo(
                eq(testVideoUri),
                eq(testCaseIdInput),
                eq(testCaseName),
                eq(testSpreadsheetId),
                any() // For the onProgress lambda
            )
        ).thenReturn(mockEvidence)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertTrue(outputData.getString(VideoProcessingWorker.RESULT_SUCCESS)?.contains("Video processed successfully") == true)
    }

    @Test
    fun `doWork when video processing service returns null returns failure`() = runTest {
        whenever(
            mockVideoProcessingService.processVideo(any(), any(), any(), any(), any())
        ).thenReturn(null)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals("Video processing finished but no evidence was created.", outputData.getString(VideoProcessingWorker.RESULT_FAILURE))
    }

    @Test
    fun `doWork when video processing service throws exception returns failure`() = runTest {
        val exceptionMessage = "Test processing exception"
        whenever(
            mockVideoProcessingService.processVideo(any(), any(), any(), any(), any())
        ).thenThrow(RuntimeException(exceptionMessage))

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals("Video processing failed: $exceptionMessage", outputData.getString(VideoProcessingWorker.RESULT_FAILURE))
    }

    @Test
    fun `doWork when input data is missing returns failure`() = runTest {
        val workerWithMissingData = TestListenableWorkerBuilder<VideoProcessingWorker>(mockContext)
            .setInputData(Data.EMPTY) // Missing required input
            .setWorkerFactory(worker.workerFactory) 
            .build()

        val result = workerWithMissingData.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertTrue(outputData.getString(VideoProcessingWorker.RESULT_FAILURE)?.startsWith("Invalid input data") == true)
    }
}
