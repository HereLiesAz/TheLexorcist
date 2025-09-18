package com.hereliesaz.lexorcist

import android.app.Application
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class EvidenceProcessingTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var ocrViewModel: OcrViewModel
    private lateinit var application: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        evidenceRepository = mockk(relaxed = true)
        application = mockk(relaxed = true)
        ocrViewModel = OcrViewModel(application, evidenceRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `performOcrOnUri calls addEvidence`() = testDispatcher.runBlockingTest {
        // Given
        val uri = Uri.parse("content://com.hereliesaz.lexorcist/evidence/1")
        val caseId = 123L
        val parentVideoId = "video1"
        val evidenceSlot = slot<Evidence>()

        // When
        ocrViewModel.performOcrOnUri(uri, mockk(), caseId, parentVideoId)

        // Then
        coVerify(exactly = 1) { evidenceRepository.addEvidence(capture(evidenceSlot)) }

        val capturedEvidence = evidenceSlot.captured
        assertEquals(caseId, capturedEvidence.caseId)
        assertEquals(parentVideoId, capturedEvidence.parentVideoId)
        assertEquals(uri.toString(), capturedEvidence.mediaUri)
        assertEquals("image", capturedEvidence.type)
    }
}
