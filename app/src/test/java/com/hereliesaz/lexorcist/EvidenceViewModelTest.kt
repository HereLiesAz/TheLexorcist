package com.hereliesaz.lexorcist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Date
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class EvidenceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var evidenceViewModel: EvidenceViewModel
    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var application: Application
    private lateinit var selectedCase: Case

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        evidenceRepository = mockk(relaxed = true)
        selectedCase = Case(id = 1, name = "Test Case", spreadsheetId = "sheet1")
        evidenceViewModel = EvidenceViewModel(application, evidenceRepository, selectedCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getEvidenceForCase returns evidence from repository`() = runTest {
        // Given
        val evidenceList = listOf(
            Evidence(id = 1, caseId = 1, content = "Evidence 1", timestamp = Date(), sourceDocument = "doc1", documentDate = Date()),
            Evidence(id = 2, caseId = 1, content = "Evidence 2", timestamp = Date(), sourceDocument = "doc2", documentDate = Date())
        )
        coEvery { evidenceRepository.getEvidenceForCase(selectedCase.id) } returns flowOf(evidenceList)

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(evidenceList, evidenceViewModel.evidenceList.value)
    }

    @Test
    fun `addEvidence calls repository`() = runTest {
        // Given
        val evidence = Evidence(id = 3, caseId = 1, content = "Evidence 3", timestamp = Date(), sourceDocument = "doc3", documentDate = Date())

        // When
        evidenceViewModel.addEvidenceToSelectedCase(evidence)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { evidenceRepository.addEvidence(selectedCase.spreadsheetId, evidence) }
    }

    @Test
    fun `deleteEvidence calls repository`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1, content = "Evidence 1", timestamp = Date(), sourceDocument = "doc1", documentDate = Date())

        // When
        evidenceViewModel.deleteEvidence(evidence)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { evidenceRepository.deleteEvidence(selectedCase.spreadsheetId, evidence) }
    }
}
