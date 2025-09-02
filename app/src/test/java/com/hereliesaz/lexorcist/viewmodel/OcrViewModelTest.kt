package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.service.OcrProcessingService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class OcrViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var ocrViewModel: OcrViewModel
    private lateinit var ocrProcessingService: OcrProcessingService
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        ocrProcessingService = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        ocrViewModel = OcrViewModel(application, ocrProcessingService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performOcrOnUri delegates to OcrProcessingService`() =
        runTest {
            // Given
            val uri: Uri = mockk()
            val context: Context = mockk()
            val caseId = 123
            val spreadsheetId = "spreadsheet-123"
            val parentVideoId = "video-456"
            coEvery { ocrProcessingService.processImageFrame(any(), any(), any(), any(), any()) } returns Unit

            // When
            ocrViewModel.performOcrOnUri(uri, context, caseId, spreadsheetId, parentVideoId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { ocrProcessingService.processImageFrame(uri, context, caseId, spreadsheetId, parentVideoId) }
        }
}
