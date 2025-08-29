package com.hereliesaz.lexorcist

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.hereliesaz.lexorcist.model.Evidence
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GoogleApiServiceTest {

    private lateinit var googleApiService: GoogleApiService
    private val sheetsService: Sheets = mockk()

    @Before
    fun setUp() {
        googleApiService = GoogleApiService(mockk(relaxed = true), "testApp")
        val field = googleApiService.javaClass.getDeclaredField("sheetsService")
        field.isAccessible = true
        field.set(googleApiService, sheetsService)
    }

    @Test
    fun `test getEvidenceForCase`() = runBlocking {
        // Given
        val spreadsheetId = "test_spreadsheet_id"
        val caseId = 1
        val values = listOf(
            listOf("Content", "Timestamp", "Source Document", "Document Date", "Tags", "Allegation ID", "Category"),
            listOf("Test content", "1672531200000", "test.txt", "1672531200000", "tag1,tag2", "1", "Medical")
        )
        val valueRange = ValueRange().setValues(values)
        coEvery { sheetsService.spreadsheets().values().get(spreadsheetId, any()).execute() } returns valueRange

        // When
        val evidence = googleApiService.getEvidenceForCase(spreadsheetId, caseId)

        // Then
        assertEquals(1, evidence.size)
        assertEquals("Test content", evidence[0].content)
        assertEquals(1672531200000L, evidence[0].timestamp)
        assertEquals("test.txt", evidence[0].sourceDocument)
        assertEquals(1672531200000L, evidence[0].documentDate)
        assertEquals(listOf("tag1", "tag2"), evidence[0].tags)
        assertEquals(1, evidence[0].allegationId)
        assertEquals("Medical", evidence[0].category)
    }

    @Test
    fun `test addEvidenceToCase`() = runBlocking {
        // Given
        val spreadsheetId = "test_spreadsheet_id"
        val evidence = Evidence(
            content = "Test content",
            timestamp = 1672531200000L,
            sourceDocument = "test.txt",
            documentDate = 1672531200000L,
            tags = listOf("tag1", "tag2"),
            allegationId = 1,
            category = "Medical"
        )
        coEvery { sheetsService.spreadsheets().values().append(any(), any(), any()).execute() } returns mockk()
        coEvery { sheetsService.spreadsheets().get(any()).execute() } returns mockk(relaxed = true)


        // When
        val result = googleApiService.addEvidenceToCase(spreadsheetId, evidence)

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `test deleteEvidenceFromCase`() = runBlocking {
        // Given
        val spreadsheetId = "test_spreadsheet_id"
        val evidenceId = 0
        coEvery { googleApiService.getSheetId(any(), any()) } returns 1
        coEvery { sheetsService.spreadsheets().batchUpdate(any(), any()).execute() } returns mockk()

        // When
        val result = googleApiService.deleteEvidenceFromCase(spreadsheetId, evidenceId)

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `test updateEvidenceInCase`() = runBlocking {
        // Given
        val spreadsheetId = "test_spreadsheet_id"
        val evidence = Evidence(
            id = 0,
            content = "Updated content",
            timestamp = 1672531200000L,
            sourceDocument = "test.txt",
            documentDate = 1672531200000L,
            tags = listOf("tag1", "tag2"),
            allegationId = 1,
            category = "Medical"
        )
        coEvery { sheetsService.spreadsheets().values().update(any(), any(), any()).execute() } returns mockk()

        // When
        val result = googleApiService.updateEvidenceInCase(spreadsheetId, evidence)

        // Then
        assertEquals(true, result)
    }
}
