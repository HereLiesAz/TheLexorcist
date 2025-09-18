package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log // Added import for Log
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.hereliesaz.lexorcist.data.Evidence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.ArgumentMatchers // Import for anyString()
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any // Import for any<Type>()
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class VideoProcessingWorkerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockVideoProcessingService: VideoProcessingService

    @Mock
    private lateinit var mockLogService: LogService

    @Mock // Mock for the Uri that Uri.parse will return
    private lateinit var mockParsedVideoUri: Uri

    private lateinit var worker: VideoProcessingWorker
    private lateinit var testVideoUriString: String
    private lateinit var mockedStaticUri: MockedStatic<Uri> // To hold the static mock for Uri
    private lateinit var mockedStaticLog: MockedStatic<Log> // To hold the static mock for Log

    private val testCaseIdInput = 1
    private val testCaseName = "Test Case"
    private val testSpreadsheetId = "test_spreadsheet_id"
    private lateinit var mockEvidence: Evidence

    @Before
    fun setup() {
        testVideoUriString = "file:///test.mp4"

        mockedStaticUri = Mockito.mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Uri> { Uri.parse(testVideoUriString) }.thenReturn(mockParsedVideoUri)
        whenever(mockParsedVideoUri.toString()).thenReturn(testVideoUriString)

        // Initialize mockedStaticLog but without any specific stubbings if they are unnecessary
        mockedStaticLog = Mockito.mockStatic(Log::class.java)

        mockEvidence = Evidence(
            id = 1,
            caseId = testCaseIdInput.toLong(),
            spreadsheetId = testSpreadsheetId,
            type = "video",
            content = "Test content",
            formattedContent = "Test formatted content",
            mediaUri = testVideoUriString,
            timestamp = System.currentTimeMillis(),
            sourceDocument = testVideoUriString,
            documentDate = System.currentTimeMillis(),
            allegationId = null,
            category = "Video Evidence",
            tags = listOf("video"),
            commentary = null,
            linkedEvidenceIds = emptyList(),
            parentVideoId = null,
            entities = emptyMap(),
            isSelected = false,
            transcriptEdits = emptyList(),
            audioTranscript = null,
            videoOcrText = null,
            duration = null
        )

        val inputData = Data.Builder()
            .putString(VideoProcessingWorker.KEY_VIDEO_URI, testVideoUriString)
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

    @After
    fun tearDown() {
        mockedStaticUri.close() 
        mockedStaticLog.close() 
    }

    @Test
    fun `doWork when video processing is successful returns success`() = runTest {
        whenever(
            mockVideoProcessingService.processVideo(
                eq(mockParsedVideoUri),
                eq(testCaseIdInput),
                eq(testCaseName),
                eq(testSpreadsheetId),
                any()
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
            mockVideoProcessingService.processVideo(eq(mockParsedVideoUri), any(), any(), any(), any())
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
            mockVideoProcessingService.processVideo(eq(mockParsedVideoUri), any(), any(), any(), any())
        ).thenThrow(RuntimeException(exceptionMessage))

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals("Video processing failed: $exceptionMessage", outputData.getString(VideoProcessingWorker.RESULT_FAILURE))
    }

    @Test
    fun `doWork when input data is missing returns failure`() = runTest {
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

        val workerWithMissingData = TestListenableWorkerBuilder<VideoProcessingWorker>(mockContext)
            .setInputData(Data.EMPTY) 
            .setWorkerFactory(workerFactory) 
            .build()

        val result = workerWithMissingData.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertTrue(outputData.getString(VideoProcessingWorker.RESULT_FAILURE)?.startsWith("Invalid input data") == true)
    }
}
