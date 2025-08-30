package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Evidence
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
class EvidenceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var evidenceViewModel: EvidenceViewModel
    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var application: Application
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        evidenceRepository = mockk(relaxed = true)
        authViewModel = mockk(relaxed = true)
        evidenceViewModel = EvidenceViewModel(application, evidenceRepository, authViewModel)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addEvidence calls repository to add evidence`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1, content = "Test content", timestamp = 1L, sourceDocument = "doc", documentDate = 1L, allegationId = 1, category = "cat", tags = emptyList())

        // When
        evidenceViewModel.addEvidence(evidence)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { evidenceRepository.addEvidence(evidence) }
    }

    @Test
    fun `updateEvidence calls repository to update evidence`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1, content = "Test content", timestamp = 1L, sourceDocument = "doc", documentDate = 1L, allegationId = 1, category = "cat", tags = emptyList())

        // When
        evidenceViewModel.updateEvidence(evidence)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { evidenceRepository.updateEvidence(evidence) }
    }

    @Test
    fun `deleteEvidence calls repository to delete evidence`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1, content = "Test content", timestamp = 1L, sourceDocument = "doc", documentDate = 1L, allegationId = 1, category = "cat", tags = emptyList())

        // When
        evidenceViewModel.deleteEvidence(evidence)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { evidenceRepository.deleteEvidence(evidence) }
    }
}
