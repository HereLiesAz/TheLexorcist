package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
import android.util.Log
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
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any // For general any
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

    @Mock
    private lateinit var mockParsedVideoUri: Uri

    private lateinit var worker: VideoProcessingWorker
    private lateinit var testVideoUriString: String
    private val testCaseIdInput = 1
    private val testCaseName = "Test Case"
    private val testSpreadsheetId = "test_spreadsheet_id"
    private lateinit var mockEvidence: Evidence
    private lateinit var mockedStaticUri: MockedStatic<Uri> // For mocking Uri.parse
    private lateinit var mockedStaticLog: MockedStatic<Log> // For mocking android.util.Log

    @Before
    fun setup() {
        testVideoUriString = "file:///test.mp4" // Define before use

        // Mock Uri.parse
        mockedStaticUri = Mockito.mockStatic(Uri::class.java)
        mockedStaticUri.`when`<Uri> { Uri.parse(testVideoUriString) }.thenReturn(mockParsedVideoUri)
        
        // Mock android.util.Log
        mockedStaticLog = Mockito.mockStatic(Log::class.java)
        // Add explicit stubs for Log.e methods using Mockito.anyString() and any<Type>() for Kotlin
        mockedStaticLog.`when`<Int> { Log.e(Mockito.anyString(), Mockito.anyString()) }.thenReturn(0)
        mockedStaticLog.`when`<Int> { Log.e(Mockito.anyString(), Mockito.anyString(), any<Throwable>()) }.thenReturn(0)

        // REMOVED: Unnecessary stubbing for mockParsedVideoUri.toString()
        // whenever(mockParsedVideoUri.toString()).thenReturn(testVideoUriString)

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
            .putString(VideoProcessingWorker.KEY_CASE_NAME, testCaseName) // CORRECTED TYPO
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
        mockedStaticUri.close() // Close the static mock for Uri
        mockedStaticLog.close() // Close the static mock for Log
    }

    @Test
    fun `doWork when video processing is successful returns success`() = runTest {
        whenever(
            mockVideoProcessingService.processVideo(
                eq(mockParsedVideoUri), // This should now match correctly
                eq(testCaseIdInput),
                eq(testCaseName),
                eq(testSpreadsheetId),
                any()
            )
        ).thenReturn(mockEvidence)

        val result = worker.doWork()

        assertTrue("Result should be Success, was $result", result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertTrue(
            "Output data should contain success message", 
            outputData.getString(VideoProcessingWorker.RESULT_SUCCESS)?.contains("Video processed successfully") == true
        )
    }

    @Test
    fun `doWork when video processing service returns null returns failure`() = runTest {
        whenever(
            mockVideoProcessingService.processVideo(eq(mockParsedVideoUri), any(), any(), any(), any())
        ).thenReturn(null)

        val result = worker.doWork()

        assertTrue("Result should be Failure, was $result", result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals(
            "Video processing finished but no evidence was created.", 
            outputData.getString(VideoProcessingWorker.RESULT_FAILURE)
        )
    }

    @Test
    fun `doWork when video processing service throws exception returns failure`() = runTest {
        val exceptionMessage = "Test processing exception"
        whenever(
            mockVideoProcessingService.processVideo(eq(mockParsedVideoUri), any(), any(), any(), any())
        ).thenThrow(RuntimeException(exceptionMessage))

        val result = worker.doWork()

        assertTrue("Result should be Failure, was $result", result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals(
            "Video processing failed: $exceptionMessage", 
            outputData.getString(VideoProcessingWorker.RESULT_FAILURE)
        )
    }

    @Test
    fun `doWork when input data is missing returns failure`() = runTest {
        // Need to re-initialize Uri mock for this specific test if it runs independently or setup is not run before each test
        // However, with JUnit, @Before runs before each @Test, so mockedStaticUri should still be active.
        // If Uri.parse is called by the worker with an empty string or null, it might throw an error before our check.
        // Let's ensure the Uri.parse for the specific empty/null case is handled if necessary or assume the worker handles it.
        // The worker is expected to fail due to missing KEY_VIDEO_URI, etc., before parsing a potentially problematic Uri string.

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

        assertTrue("Result should be Failure, was $result", result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertTrue(
            "Failure message should indicate invalid input data",
            outputData.getString(VideoProcessingWorker.RESULT_FAILURE)?.startsWith("Invalid input data") == true
        )
    }
}
