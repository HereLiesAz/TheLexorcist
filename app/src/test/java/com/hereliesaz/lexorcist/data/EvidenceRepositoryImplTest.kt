package com.hereliesaz.lexorcist.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class EvidenceRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var evidenceRepository: EvidenceRepository
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
    fun `getEvidenceForCase returns evidence from googleApiService`() = runTest {
        // Given
        val spreadsheetId = "test_spreadsheet_id"
        val caseId = 1L
        val sheetData = mapOf(
            "Evidence" to listOf(
                listOf("1", "1", "text", "content", "123", "doc", "456", "1", "cat", "tag1", "comment", "", "")
            )
        )
        coEvery { googleApiService.readSpreadsheet(spreadsheetId) } returns sheetData

        // When
        val result = evidenceRepository.getEvidenceForCase(spreadsheetId, caseId).first()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, result.size)
        assertEquals("content", result[0].content)
    }

    @Test
    fun `updateEvidence calls writeData on googleApiService`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1L, spreadsheetId = "sheet1", type = "text", content = "Test", timestamp = 0, sourceDocument = "doc", documentDate = 0, allegationId = null, category = "cat", tags = listOf("tag"))
        val sheetData = mapOf(
            "Evidence" to listOf(
                listOf("1", "1", "text", "old content", "0", "doc", "0", "", "cat", "tag")
            )
        )
        coEvery { googleApiService.readSpreadsheet("sheet1") } returns sheetData

        // When
        evidenceRepository.updateEvidence(evidence)

        // Then
        coVerify { googleApiService.writeData(eq("sheet1"), eq("Evidence"), any()) }
    }

    @Test
    fun `deleteEvidence calls clearSheet and writeData on googleApiService`() = runTest {
        // Given
        val evidence = Evidence(id = 1, caseId = 1L, spreadsheetId = "sheet1", type = "text", content = "Test", timestamp = 0, sourceDocument = "doc", documentDate = 0, allegationId = null, category = "cat", tags = listOf("tag"))
        val sheetData = mapOf(
            "Evidence" to listOf(
                listOf("1", "1", "text", "content", "0", "doc", "0", "", "cat", "tag"),
                listOf("2", "1", "text", "other content", "0", "doc", "0", "", "cat", "tag")
            )
        )
        coEvery { googleApiService.readSpreadsheet("sheet1") } returns sheetData

        // When
        evidenceRepository.deleteEvidence(evidence)

        // Then
        coVerify { googleApiService.clearSheet("sheet1", "Evidence") }
        coVerify { googleApiService.writeData("sheet1", "Evidence", any()) }
    }
}
