package com.hereliesaz.lexorcist.service

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class VideoProcessingWorkerTest {

    private lateinit var context: Context
    private lateinit var worker: VideoProcessingWorker

    private val evidenceRepository: EvidenceRepository = mock()
    private val ocrProcessingService: OcrProcessingService = mock()
    private val transcriptionService: TranscriptionService = mock()
    private val googleApiService: GoogleApiService = mock()
    private val logService: LogService = mock()

    @Before
    fun setup() {
        context = mock()
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return VideoProcessingWorker(
                    appContext,
                    workerParameters,
                    evidenceRepository,
                    ocrProcessingService,
                    transcriptionService,
                    googleApiService,
                    logService
                )
            }
        }
        worker = TestListenableWorkerBuilder<VideoProcessingWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
    }

    @Test
    fun testSuccessfulWork() = runBlocking {
        // Given
        whenever(evidenceRepository.uploadFile(any(), any(), any()))
            .thenReturn(com.hereliesaz.lexorcist.utils.Result.Success("path"))
        whenever(transcriptionService.transcribeAudio(any()))
            .thenReturn(Pair("transcript", null))
        whenever(ocrProcessingService.processImageFrame(any(), any(), any(), any(), any()))
            .thenReturn(mock())

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun testTranscriptionError() = runBlocking {
        // Given
        whenever(evidenceRepository.uploadFile(any(), any(), any()))
            .thenReturn(com.hereliesaz.lexorcist.utils.Result.Success("path"))
        whenever(transcriptionService.transcribeAudio(any()))
            .thenReturn(Pair("", "error"))

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
