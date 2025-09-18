package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Evidence // Assuming Evidence is used by the service
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.service.OcrProcessingService
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
import org.mockito.Answers
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class) // Use MockitoJUnitRunner for @Mock initialization
class OcrViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule() // For LiveData

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockOcrProcessingService: OcrProcessingService

    @Captor
    private lateinit var onProgressLambdaCaptor: ArgumentCaptor<(ProcessingState) -> Unit>

    private lateinit var ocrViewModel: OcrViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher) // Set main dispatcher for coroutines
        ocrViewModel = OcrViewModel(mockApplication, mockOcrProcessingService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset main dispatcher
    }

    @Test
    fun `performOcrOnUri calls processImage on service and updates state`() = runTest(testDispatcher) {
        // Given
        val testUri: Uri = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS) // Use mock with deep stubs for Uri if methods are called on it
        val mockContext: Context = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        val testCaseId = 1L
        val testSpreadsheetId = "spreadsheet1"

        val mockEvidence: Evidence = mock() // Mock the evidence returned by the service

        // Mock the behavior of ocrProcessingService.processImage
        whenever(
            mockOcrProcessingService.processImage(
                eq(testUri),
                eq(mockContext),
                eq(testCaseId),
                eq(testSpreadsheetId),
                capture(onProgressLambdaCaptor)
            )
        ).doAnswer { invocation ->
            // Simulate invoking the captured lambda by the service
            val onProgress = invocation.getArgument<(ProcessingState) -> Unit>(4)
            onProgress.invoke(ProcessingState.InProgress(0.5f))
            onProgress.invoke(ProcessingState.Completed("Success from mock"))
            Pair(mockEvidence, "Processed from mock") // Return a mocked Evidence and a message
        }

        // When
        ocrViewModel.performOcrOnUri(testUri, mockContext, testCaseId, testSpreadsheetId)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutines launched on main dispatcher complete

        // Then
        verify(mockOcrProcessingService).processImage(
            eq(testUri),
            eq(mockContext),
            eq(testCaseId),
            eq(testSpreadsheetId),
            any() // Verify it was called with any lambda for onProgress (or capture and check)
        )

        // Optionally, assert that the ViewModel's LiveData/StateFlow for processingState was updated.
        // This would require collecting the flow in the test using something like Turbine or testDispatcher.runCurrent().
        // For example (simplified, assuming processingState is a StateFlow exposed by ViewModel):
        // val resultState = ocrViewModel.processingState.value // Or collect if it's a Flow
        // assert(resultState is ProcessingState.Completed)
        // assertEquals("Success from mock", (resultState as ProcessingState.Completed).message)
    }
}
