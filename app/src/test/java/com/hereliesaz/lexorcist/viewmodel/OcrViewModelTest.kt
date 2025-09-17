package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.EvidenceRepository
import io.mockk.coVerify
import io.mockk.mockk
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
    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        evidenceRepository = mockk(relaxed = true)
        ocrViewModel = OcrViewModel(application, evidenceRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performOcrOnUri calls addEvidence`() = runTest {
        // Given
        val uri: Uri = mockk()
        val context: Context = mockk(relaxed = true)
        val caseId = 1L
        val parentVideoId = "video1"

        // When
        ocrViewModel.performOcrOnUri(uri, context, caseId, parentVideoId)
        testDispatcher.scheduler.runCurrent()

        // Then
        coVerify { evidenceRepository.addEvidence(any()) }
    }
}
