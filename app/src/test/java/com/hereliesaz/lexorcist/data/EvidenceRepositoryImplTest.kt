package com.hereliesaz.lexorcist.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hereliesaz.lexorcist.GoogleApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class EvidenceRepositoryImplTest {

    // This rule is needed to test code that uses Architecture Components with background tasks
    @get:Rule
    val instantTaskExecutorRule = androidx.arch.core.executor.testing.InstantTaskExecutorRule()

    private lateinit var evidenceRepository: EvidenceRepository
    private lateinit var googleApiService: GoogleApiService

    // Test data
    private val evidence1 = Evidence(
        id = 1,
        caseId = 101,
        spreadsheetId = "test_spreadsheet_id",
        type = "NOTE",
        content = "This is the first piece of evidence.",
        timestamp = System.currentTimeMillis(),
        sourceDocument = "doc-1.pdf",
        documentDate = System.currentTimeMillis() - 10000,
        allegationId = 1,
        category = "Financial",
        tags = listOf("receipt", "expense"),
        commentary = "Initial commentary for evidence 1."
    )
    private val evidence2 = Evidence(
        id = 2,
        caseId = 101,
        spreadsheetId = "test_spreadsheet_id",
        type = "IMAGE",
        content = "path/to/image.jpg",
        timestamp = System.currentTimeMillis(),
        sourceDocument = "doc-2.pdf",
        documentDate = System.currentTimeMillis() - 20000,
        allegationId = 2,
        category = "Correspondence",
        tags = listOf("email", "screenshot"),
        commentary = "Initial commentary for evidence 2."
    )

    @Before
    fun setup() {
        // Mock the GoogleApiService
        googleApiService = mockk(relaxed = true)

        // Initialize the repository with the mocked service
        evidenceRepository = EvidenceRepositoryImpl(googleApiService)
    }

    // A helper function to create a mock sheet from a list of evidence
    private fun createMockSheet(evidenceList: List<Evidence>): Map<String, List<List<Any>>> {
        val sheetData = evidenceList.map {
            listOf(
                it.id.toString(),
                it.caseId.toString(),
                it.type,
                it.content,
                it.timestamp.toString(),
                it.sourceDocument,
                it.documentDate.toString(),
                it.allegationId?.toString() ?: "",
                it.category,
                it.tags.joinToString(","),
                it.commentary ?: "",
                it.linkedEvidenceIds.joinToString(","),
                it.parentVideoId ?: ""
            )
        }
        return mapOf("Evidence" to sheetData)
    }

    @Test
    fun `deleteEvidence should remove the item and update the sheet`() = runTest {
        // Given
        val initialEvidenceList = listOf(evidence1, evidence2)
        val initialSheet = createMockSheet(initialEvidenceList)
        coEvery { googleApiService.readSpreadsheet(evidence1.spreadsheetId) } returns initialSheet

        // When
        evidenceRepository.deleteEvidence(evidence1)

        // Then
        // Verify that the sheet is cleared first
        coVerify { googleApiService.clearSheet(evidence1.spreadsheetId, "Evidence") }

        // Verify that the remaining evidence is written back to the sheet
        val expectedSheet = createMockSheet(listOf(evidence2))
        coVerify { googleApiService.writeData(evidence1.spreadsheetId, "Evidence", expectedSheet.getValue("Evidence")) }
    }

    @Test
    fun `updateEvidence should modify the item and update the sheet`() = runTest {
        // Given
        val initialEvidenceList = listOf(evidence1, evidence2)
        val initialSheet = createMockSheet(initialEvidenceList)
        coEvery { googleApiService.readSpreadsheet(evidence1.spreadsheetId) } returns initialSheet
        val updatedEvidence = evidence1.copy(content = "This is the updated content.")

        // When
        evidenceRepository.updateEvidence(updatedEvidence)

        // Then
        val expectedSheetData = createMockSheet(listOf(updatedEvidence, evidence2))["Evidence"]
        coVerify { googleApiService.writeData(updatedEvidence.spreadsheetId, "Evidence", expectedSheetData!!) }
    }

    @Test
    fun `addEvidence should append the item to the sheet`() = runTest {
        // Given
        val newEvidence = evidence1.copy(id = 3) // Ensure a unique ID for the new item

        // When
        evidenceRepository.addEvidence(newEvidence)

        // Then
        val expectedSheetData = createMockSheet(listOf(newEvidence))["Evidence"]
        coVerify { googleApiService.appendData(newEvidence.spreadsheetId, "Evidence", expectedSheetData!!) }
    }
}
