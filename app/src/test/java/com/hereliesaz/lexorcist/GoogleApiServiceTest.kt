package com.hereliesaz.lexorcist

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.AddSheetRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.any
import io.mockk.eq
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class GoogleApiServiceTest {

    private lateinit var googleApiService: GoogleApiService
    private lateinit var driveService: Drive
    private lateinit var sheetsService: Sheets

    @Before
    fun setup() {
        driveService = mockk(relaxed = true)
        sheetsService = mockk(relaxed = true)
        googleApiService = GoogleApiService(driveService, sheetsService)
    }

    @Test
    fun `getOrCreateAppRootFolder when folder exists returns existing folder id`() = runTest {
        // Given
        val fileList = FileList().setFiles(listOf(File().setId("test_id")))
        coEvery { driveService.files().list().setQ(any()).setSpaces(any()).execute() } returns fileList

        // When
        val folderId = googleApiService.getOrCreateAppRootFolder()

        // Then
        assertEquals("test_id", folderId)
    }

    @Test
    fun `getOrCreateAppRootFolder when folder does not exist creates and returns new folder id`() = runTest {
        // Given
        val fileList = FileList().setFiles(emptyList())
        val newFile = File().setId("new_test_id")
        coEvery { driveService.files().list().setQ(any()).setSpaces(any()).execute() } returns fileList
        coEvery { driveService.files().create(any()).setFields("id").execute() } returns newFile

        // When
        val folderId = googleApiService.getOrCreateAppRootFolder()

        // Then
        assertEquals("new_test_id", folderId)
    }

    @Test
    fun `createSpreadsheet returns new spreadsheet id`() = runTest {
        // Given
        val newFile = File().setId("new_spreadsheet_id")
        coEvery { driveService.files().create(any()).setFields("id").execute() } returns newFile

        // When
        val spreadsheetId = googleApiService.createSpreadsheet("Test Spreadsheet", "test_folder_id")

        // Then
        assertEquals("new_spreadsheet_id", spreadsheetId)
    }

    @Test
    fun `addSheet calls sheets service to add sheet`() = runTest {
        // Given
        val spreadsheetId = "test_spreadsheet_id"
        val sheetTitle = "Test Sheet"
        val spreadsheet = Spreadsheet().setSheets(emptyList())
        coEvery { sheetsService.spreadsheets().get(spreadsheetId).execute() } returns spreadsheet
        coEvery { sheetsService.spreadsheets().batchUpdate(any(), any()).execute() } returns mockk()

        // When
        googleApiService.addSheet(spreadsheetId, sheetTitle)

        // Then
        coVerify { sheetsService.spreadsheets().batchUpdate(eq(spreadsheetId), any()) }
    }

    @Test
    fun `appendData calls sheets service to append data`() = runTest {
        // Given
        val spreadsheetId = "test_spreadsheet_id"
        val sheetTitle = "Test Sheet"
        val values = listOf(listOf("a", "b"), listOf("c", "d"))
        coEvery { sheetsService.spreadsheets().values().append(any(), any(), any()).execute() } returns mockk()

        // When
        googleApiService.appendData(spreadsheetId, sheetTitle, values)

        // Then
        coVerify { sheetsService.spreadsheets().values().append(eq(spreadsheetId), eq("$sheetTitle!A1"), any()) }
    }
}
