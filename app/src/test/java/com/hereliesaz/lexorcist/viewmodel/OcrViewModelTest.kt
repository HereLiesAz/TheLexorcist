package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.service.OcrProcessingService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
    private lateinit var mockOcrProcessingService: OcrProcessingService
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        mockOcrProcessingService = mockk(relaxed = true) // Create a relaxed mock for OcrProcessingService
        ocrViewModel = OcrViewModel(application, mockOcrProcessingService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performOcrOnUri calls processImage on service and updates state`() = runTest {
        // Given
        val uri: Uri = mockk()
        val context: Context = mockk(relaxed = true)
        val caseId = 1L
        val spreadsheetId = "spreadsheet1"
        // Correctly type the slot for a non-suspending lambda
        val onProgressLambdaSlot = slot<(ProcessingState) -> Unit>()

        coEvery {
            mockOcrProcessingService.processImage(
                uri = eq(uri),
                context = eq(context),
                caseId = eq(caseId),
                spreadsheetId = eq(spreadsheetId),
                onProgress = capture(onProgressLambdaSlot)
            )
        } coAnswers { // coAnswers allows us to invoke the captured lambda
            // Simulate some progress updates by invoking the captured lambda
            onProgressLambdaSlot.captured.invoke(ProcessingState.InProgress(0.5f))
            onProgressLambdaSlot.captured.invoke(ProcessingState.Completed("Success"))
            Pair(mockk(), "Processed") // Return a mocked Evidence and a message
        }

        // When
        ocrViewModel.performOcrOnUri(uri, context, caseId, spreadsheetId)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutines launched on main dispatcher complete

        // Then
        coVerify {
            mockOcrProcessingService.processImage(
                uri = uri,
                context = context,
                caseId = caseId,
                spreadsheetId = spreadsheetId,
                onProgress = any() 
            )
        }
    }
}
