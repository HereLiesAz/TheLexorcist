package com.hereliesaz.lexorcist

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.ValueRange
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class GoogleApiServiceTest {

    private lateinit var googleApiService: GoogleApiService
    private val credential: GoogleAccountCredential = mockk(relaxed = true)
    private val drive: Drive = mockk(relaxed = true)
    private val sheets: Sheets = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkConstructor(Drive.Builder::class)
        mockkConstructor(Sheets.Builder::class)

        every {
            constructedWith<Drive.Builder>(
                any(),
                any(),
                any()
            ).setApplicationName(any())
                .build()
        } returns drive

        every {
            constructedWith<Sheets.Builder>(
                any(),
                any(),
                any()
            ).setApplicationName(any())
                .build()
        } returns sheets

        googleApiService = GoogleApiService(credential, "The Lexorcist")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getOrCreateAppRootFolder when folder exists returns existing folder id`() = runTest {
        // Given
        val fileList = FileList().setFiles(listOf(File().setId("test_id")))
        val filesListMock = mockk<Drive.Files.List>()
        every { filesListMock.execute() } returns fileList
        every { filesListMock.setSpaces("drive") } returns filesListMock
        every { filesListMock.setQ(any()) } returns filesListMock
        val filesMock = mockk<Drive.Files>()
        every { filesMock.list() } returns filesListMock
        every { drive.files() } returns filesMock

        // When
        val folderId = googleApiService.getOrCreateAppRootFolder()

        // Then
        assertEquals("test_id", folderId)
        verify { filesListMock.setQ("mimeType='application/vnd.google-apps.folder' and name='Lexorcist' and trashed=false") }
    }

    @Test
    fun `getOrCreateAppRootFolder when folder does not exist creates and returns new folder id`() = runTest {
        // Given
        val fileList = FileList().setFiles(emptyList())
        val newFile = File().setId("new_test_id")

        val filesListMock = mockk<Drive.Files.List>()
        every { filesListMock.execute() } returns fileList
        every { filesListMock.setSpaces("drive") } returns filesListMock
        every { filesListMock.setQ(any()) } returns filesListMock

        val fileCreateMock = mockk<Drive.Files.Create>()
        every { fileCreateMock.setFields("id") } returns fileCreateMock
        every { fileCreateMock.execute() } returns newFile

        val filesMock = mockk<Drive.Files>()
        every { filesMock.list() } returns filesListMock
        every { filesMock.create(any()) } returns fileCreateMock

        every { drive.files() } returns filesMock

        // When
        val folderId = googleApiService.getOrCreateAppRootFolder()

        // Then
        assertEquals("new_test_id", folderId)
    }

    @Test
    fun `getOrCreateAppRootFolder handles IOException and returns null`() = runTest {
        // Given
        val filesListMock = mockk<Drive.Files.List>()
        every { filesListMock.execute() } throws IOException()
        every { filesListMock.setSpaces("drive") } returns filesListMock
        every { filesListMock.setQ(any()) } returns filesListMock
        val filesMock = mockk<Drive.Files>()
        every { filesMock.list() } returns filesListMock
        every { drive.files() } returns filesMock

        // When
        val folderId = googleApiService.getOrCreateAppRootFolder()

        // Then
        assertEquals(null, folderId)
    }

    @Test
    fun `getOrCreateCaseRegistrySpreadsheetId when sheet exists returns id`() = runTest {
        // Given
        val fileList = FileList().setFiles(listOf(File().setId("sheet_id")))
        val filesListMock = mockk<Drive.Files.List>()
        every { filesListMock.execute() } returns fileList
        every { filesListMock.setSpaces(any()) } returns filesListMock
        every { filesListMock.setQ(any()) } returns filesListMock
        val filesMock = mockk<Drive.Files>()
        every { filesMock.list() } returns filesListMock
        every { drive.files() } returns filesMock

        // When
        val spreadsheetId = googleApiService.getOrCreateCaseRegistrySpreadsheetId("folder_id")

        // Then
        assertEquals("sheet_id", spreadsheetId)
    }

    @Test
    fun `getOrCreateCaseRegistrySpreadsheetId when sheet does not exist creates it`() = runTest {
        // Given
        val fileList = FileList().setFiles(emptyList())
        val filesListMock = mockk<Drive.Files.List>()
        every { filesListMock.execute() } returns fileList
        every { filesListMock.setSpaces(any()) } returns filesListMock
        every { filesListMock.setQ(any()) } returns filesListMock

        val spreadsheet = mockk<Spreadsheet>()
        every { spreadsheet.setProperties(any()) } returns spreadsheet
        val createRequest = mockk<Sheets.Spreadsheets.Create>()
        val createdSheet = mockk<Spreadsheet>()
        every { createdSheet.spreadsheetId } returns "new_sheet_id"
        every { createRequest.setFields("spreadsheetId") } returns createRequest
        every { createRequest.execute() } returns createdSheet
        val spreadsheetsMock = mockk<Sheets.Spreadsheets>()
        every { spreadsheetsMock.create(any()) } returns createRequest
        every { sheets.spreadsheets() } returns spreadsheetsMock

        val updateRequest = mockk<Drive.Files.Update>()
        every { updateRequest.setAddParents(any()) } returns updateRequest
        every { updateRequest.execute() } returns mockk()
        val filesMock = mockk<Drive.Files>()
        every { filesMock.list() } returns filesListMock
        every { filesMock.update(any(), any()) } returns updateRequest
        every { drive.files() } returns filesMock


        // When
        val spreadsheetId = googleApiService.getOrCreateCaseRegistrySpreadsheetId("folder_id")

        // Then
        assertEquals("new_sheet_id", spreadsheetId)
    }

    @Test
    fun `createSpreadsheet returns success result`() = runTest {
        // Given
        val createdSheet = mockk<Spreadsheet>()
        every { createdSheet.spreadsheetId } returns "new_sheet_id"
        val createRequest = mockk<Sheets.Spreadsheets.Create>()
        every { createRequest.setFields("spreadsheetId") } returns createRequest
        every { createRequest.execute() } returns createdSheet
        val spreadsheetsMock = mockk<Sheets.Spreadsheets>()
        every { spreadsheetsMock.create(any()) } returns createRequest
        every { sheets.spreadsheets() } returns spreadsheetsMock

        val updateRequest = mockk<Drive.Files.Update>()
        every { updateRequest.setAddParents(any()) } returns updateRequest
        every { updateRequest.execute() } returns mockk()
        val filesMock = mockk<Drive.Files>()
        every { filesMock.update(any(), any()) } returns updateRequest
        every { drive.files() } returns filesMock

        // When
        val result = googleApiService.createSpreadsheet("title", "folder_id")

        // Then
        assert(result is com.hereliesaz.lexorcist.utils.Result.Success)
        assertEquals("new_sheet_id", (result as com.hereliesaz.lexorcist.utils.Result.Success).data)
    }

    @Test
    fun `getAllCasesFromRegistry returns list of cases`() = runTest {
        // Given
        val values = listOf(
            listOf("1", "Case 1", "sheet1", "script1", "pdf1", "html1", "template1", "folder1", "p1", "d1", "c1", "false", "123"),
            listOf("2", "Case 2", "sheet2") // less columns
        )
        val valueRange = ValueRange().setValues(values)
        val getRequest = mockk<Sheets.Spreadsheets.Values.Get>()
        every { getRequest.execute() } returns valueRange
        val valuesMock = mockk<Sheets.Spreadsheets.Values>()
        every { valuesMock.get(any(), any()) } returns getRequest
        val spreadsheetsMock = mockk<Sheets.Spreadsheets>()
        every { spreadsheetsMock.values() } returns valuesMock
        every { sheets.spreadsheets() } returns spreadsheetsMock

        // When
        val cases = googleApiService.getAllCasesFromRegistry("registry_id")

        // Then
        assertEquals(2, cases.size)
        assertEquals("Case 1", cases[0].name)
        assertEquals("Case 2", cases[1].name)
    }
}
