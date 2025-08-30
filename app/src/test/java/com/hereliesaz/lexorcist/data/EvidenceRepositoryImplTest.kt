package com.hereliesaz.lexorcist.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import io.mockk.any
import io.mockk.eq

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class EvidenceRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var evidenceDao: EvidenceDao
    private lateinit var googleApiService: GoogleApiService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        evidenceDao = mockk(relaxed = true)
        googleApiService = mockk(relaxed = true)
        evidenceRepository = EvidenceRepositoryImpl(evidenceDao)
        evidenceRepository.setGoogleApiService(googleApiService)
        evidenceRepository.setCaseSpreadsheetId("test_spreadsheet_id")
        evidenceRepository.setCaseScriptId("test_script_id")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteEvidence calls dao and googleApiService`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1, content = "Test", timestamp = 0, sourceDocument = "doc", documentDate = 0)

        // When
        evidenceRepository.deleteEvidence(evidence)

        // Then
        coVerify { evidenceDao.delete(evidence) }
        coVerify { googleApiService.deleteEvidenceFromCase("test_spreadsheet_id", 1) }
    }

    @Test
    fun `updateEvidence calls scriptRunner, dao, and googleApiService`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1, content = "Test", timestamp = 0, sourceDocument = "doc", documentDate = 0, tags = listOf("old_tag"))
        val scriptFile = com.google.api.services.script.model.File().setSource("parser.tags.add('new_tag');")
        val scriptContent = com.google.api.services.script.model.Content().setFiles(listOf(scriptFile))
        coEvery { googleApiService.getScript("test_script_id") } returns scriptContent

        // When
        evidenceRepository.updateEvidence(evidence)

        // Then
        val expectedEvidence = evidence.copy(tags = listOf("new_tag"))
        coVerify { evidenceDao.update(expectedEvidence) }
        coVerify { googleApiService.updateEvidenceInCase("test_spreadsheet_id", expectedEvidence) }
    }

    @Test
    fun `addEvidence calls scriptRunner, googleApiService, and dao`() = runTest {
        // Given
        val scriptFile = com.google.api.services.script.model.File().setSource("parser.tags.add('new_tag');")
        val scriptContent = com.google.api.services.script.model.Content().setFiles(listOf(scriptFile))
        coEvery { googleApiService.getScript("test_script_id") } returns scriptContent
        coEvery { googleApiService.addEvidenceToCase(any(), any()) } returns 5 // New row index

        // When
        evidenceRepository.addEvidence(1, "New Content", "new_doc", "new_cat", null)

        // Then
        coVerify { googleApiService.addEvidenceToCase(eq("test_spreadsheet_id"), any()) }
        coVerify { evidenceDao.insert(any()) }
    }
}
