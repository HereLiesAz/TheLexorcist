package com.hereliesaz.lexorcist.service

import android.content.Context
import android.net.Uri
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
import org.junit.After // @After is no longer strictly needed if mockedStaticUri is scoped locally
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
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

    @Mock // This mock is returned by Uri.parse, so it's still needed at class level
    private lateinit var mockParsedVideoUri: Uri

    private lateinit var worker: VideoProcessingWorker
    private lateinit var testVideoUriString: String
    // private lateinit var mockedStaticUri: MockedStatic<Uri> // Removed from class level

    private val testCaseIdInput = 1
    private val testCaseName = "Test Case"
    private val testSpreadsheetId = "test_spreadsheet_id"
    private lateinit var mockEvidence: Evidence

    @Before
    fun setup() {
        testVideoUriString = "file:///test.mp4"
        // Uri.parse mocking is moved to individual tests that need it.

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
    }

    // @After // No longer needed as static mocks are closed in finally blocks
    // fun tearDown() {
    //     // mockedStaticUri.close() // Removed
    // }

    private fun createWorker(inputData: Data): VideoProcessingWorker {
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
        return TestListenableWorkerBuilder<VideoProcessingWorker>(mockContext)
            .setInputData(inputData)
            .setWorkerFactory(workerFactory)
            .build()
    }

    @Test
    fun `doWork when video processing is successful returns success`() = runTest {
        var localMockedStaticUri: MockedStatic<Uri>? = null
        try {
            val inputData = Data.Builder()
                .putString(VideoProcessingWorker.KEY_VIDEO_URI, testVideoUriString)
                .putInt(VideoProcessingWorker.KEY_CASE_ID, testCaseIdInput)
                .putString(VideoProcessingWorker.KEY_CASE_NAME, testCaseName)
                .putString(VideoProcessingWorker.KEY_SPREADSHEET_ID, testSpreadsheetId)
                .build()
            worker = createWorker(inputData)

            localMockedStaticUri = Mockito.mockStatic(Uri::class.java)
            localMockedStaticUri.`when`<Uri> { Uri.parse(testVideoUriString) }.thenReturn(mockParsedVideoUri)
            
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
        } finally {
            localMockedStaticUri?.close()
        }
    }

    @Test
    fun `doWork when video processing service returns null returns failure`() = runTest {
        var localMockedStaticUri: MockedStatic<Uri>? = null
        try {
            val inputData = Data.Builder()
                .putString(VideoProcessingWorker.KEY_VIDEO_URI, testVideoUriString)
                .putInt(VideoProcessingWorker.KEY_CASE_ID, testCaseIdInput)
                .putString(VideoProcessingWorker.KEY_CASE_NAME, testCaseName)
                .putString(VideoProcessingWorker.KEY_SPREADSHEET_ID, testSpreadsheetId)
                .build()
            worker = createWorker(inputData)

            localMockedStaticUri = Mockito.mockStatic(Uri::class.java)
            localMockedStaticUri.`when`<Uri> { Uri.parse(testVideoUriString) }.thenReturn(mockParsedVideoUri)

            whenever(
                mockVideoProcessingService.processVideo(eq(mockParsedVideoUri), any(), any(), any(), any())
            ).thenReturn(null)

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            val outputData = (result as ListenableWorker.Result.Failure).outputData
            assertEquals("Video processing finished but no evidence was created.", outputData.getString(VideoProcessingWorker.RESULT_FAILURE))
        } finally {
            localMockedStaticUri?.close()
        }
    }

    @Test
    fun `doWork when video processing service throws exception returns failure`() = runTest {
        var localMockedStaticUri: MockedStatic<Uri>? = null
        try {
            val inputData = Data.Builder()
                .putString(VideoProcessingWorker.KEY_VIDEO_URI, testVideoUriString)
                .putInt(VideoProcessingWorker.KEY_CASE_ID, testCaseIdInput)
                .putString(VideoProcessingWorker.KEY_CASE_NAME, testCaseName)
                .putString(VideoProcessingWorker.KEY_SPREADSHEET_ID, testSpreadsheetId)
                .build()
            worker = createWorker(inputData)

            localMockedStaticUri = Mockito.mockStatic(Uri::class.java)
            localMockedStaticUri.`when`<Uri> { Uri.parse(testVideoUriString) }.thenReturn(mockParsedVideoUri)

            val exceptionMessage = "Test processing exception"
            whenever(
                mockVideoProcessingService.processVideo(eq(mockParsedVideoUri), any(), any(), any(), any())
            ).thenThrow(RuntimeException(exceptionMessage))

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            val outputData = (result as ListenableWorker.Result.Failure).outputData
            assertEquals("Video processing failed: $exceptionMessage", outputData.getString(VideoProcessingWorker.RESULT_FAILURE))
        } finally {
            localMockedStaticUri?.close()
        }
    }

    @Test
    fun `doWork when input data is missing returns failure`() = runTest {
        // No Uri.parse mocking needed here as it shouldn't be called
        val inputData = Data.EMPTY
        worker = createWorker(inputData)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertTrue(outputData.getString(VideoProcessingWorker.RESULT_FAILURE)?.startsWith("Invalid input data") == true)
    }
}
