package com.hereliesaz.lexorcist.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
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
class EvidenceRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var googleApiService: GoogleApiService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        googleApiService = mockk(relaxed = true)
        evidenceRepository = EvidenceRepositoryImpl(googleApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getEvidenceForCase does not crash`() = runTest {
        evidenceRepository.getEvidenceForCase("spreadsheet_id", 1L)
    }

    @Test
    fun `getEvidenceById does not crash`() = runTest {
        evidenceRepository.getEvidenceById(1)
    }

    @Test
    fun `getEvidence does not crash`() = runTest {
        evidenceRepository.getEvidence(1)
    }

    @Test
    fun `addEvidence does not crash`() = runTest {
        val evidence = Evidence(id = 1, caseId = 1, content = "Test", timestamp = 0, sourceDocument = "doc", documentDate = 0, type = "type", allegationId = 1, category = "cat", tags = listOf(), commentary = "com", spreadsheetId = "s1")
        evidenceRepository.addEvidence(evidence)
    }

    @Test
    fun `updateEvidence does not crash`() = runTest {
        val evidence = Evidence(id = 1, caseId = 1, content = "Test", timestamp = 0, sourceDocument = "doc", documentDate = 0, type = "type", allegationId = 1, category = "cat", tags = listOf(), commentary = "com", spreadsheetId = "s1")
        evidenceRepository.updateEvidence(evidence)
    }

    @Test
    fun `deleteEvidence does not crash`() = runTest {
        val evidence = Evidence(id = 1, caseId = 1, content = "Test", timestamp = 0, sourceDocument = "doc", documentDate = 0, type = "type", allegationId = 1, category = "cat", tags = listOf(), commentary = "com", spreadsheetId = "s1")
        evidenceRepository.deleteEvidence(evidence)
    }

    @Test
    fun `updateCommentary does not crash`() = runTest {
        evidenceRepository.updateCommentary(1, "commentary")
    }
}
