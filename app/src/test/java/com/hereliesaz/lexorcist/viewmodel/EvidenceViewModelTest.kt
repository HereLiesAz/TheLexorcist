package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.EvidenceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        evidenceRepository = mockk(relaxed = true)
        evidenceViewModel = EvidenceViewModel(application, evidenceRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadEvidenceForCase returns evidence from repository`() = runTest {
        // Given
        val caseId = 1L
        val evidenceList = listOf(
            Evidence(id = 1, caseId = 1, content = "Evidence 1", timestamp = System.currentTimeMillis(), sourceDocument = "doc1", documentDate = System.currentTimeMillis()),
            Evidence(id = 2, caseId = 1, content = "Evidence 2", timestamp = System.currentTimeMillis(), sourceDocument = "doc2", documentDate = System.currentTimeMillis())
        )
        coEvery { evidenceRepository.getEvidenceForCase(caseId) } returns flowOf(evidenceList)

        // When
        evidenceViewModel.loadEvidenceForCase(caseId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(evidenceList, evidenceViewModel.evidenceList.value)
    }

    @Test
    fun `addEvidence calls repository`() = runTest {
        // Given
        val evidence = Evidence(id = 3, caseId = 1, content = "Evidence 3", timestamp = System.currentTimeMillis(), sourceDocument = "doc3", documentDate = System.currentTimeMillis())

        // When
        evidenceViewModel.addEvidence(evidence)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { evidenceRepository.addEvidence(evidence) }
    }

    @Test
    fun `deleteEvidence calls repository`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1, content = "Evidence 1", timestamp = System.currentTimeMillis(), sourceDocument = "doc1", documentDate = System.currentTimeMillis())

        // When
        evidenceViewModel.deleteEvidence(evidence)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { evidenceRepository.deleteEvidence(evidence) }
    }
}
