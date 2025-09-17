package com.hereliesaz.lexorcist.service

import android.app.Application
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.AppendValuesResponse
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleApiService @Inject constructor(
    private val credentialHolder: CredentialHolder,
    private val application: Application
) {
    private val applicationName = "The Lexorcist" // Or load from string resource via application context
    private val gsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    private fun getDriveService(): Drive? {
        return credentialHolder.credential?.let { cred ->
            Drive.Builder(httpTransport, gsonFactory, cred)
                .setApplicationName(applicationName)
                .build()
        }
    }

    private fun getSheetsService(): Sheets? {
        return credentialHolder.credential?.let { cred ->
            Sheets.Builder(httpTransport, gsonFactory, cred)
                .setApplicationName(applicationName)
                .build()
        }
    }

    suspend fun getOrCreateAppRootFolder(): Result<String> =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
            val folderName = "Lexorcist"
            val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false"
            Log.d("GoogleApiService", "Querying for root folder with: $query")
            try {
                val files =
                    drive
                        .files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .execute()
                        .files
                Log.d("GoogleApiService", "Found ${files?.size ?: 0} root folder(s).")
                if (files.isNullOrEmpty()) {
                    Log.d("GoogleApiService", "Root folder not found, creating it.")
                    val fileMetadata =
                        File().apply {
                            name = folderName
                            mimeType = "application/vnd.google-apps.folder"
                        }
                    val createdFile =
                        drive
                            .files()
                            .create(fileMetadata)
                            .setFields("id")
                            .execute()
                    Log.d("GoogleApiService", "Root folder created with ID: ${createdFile.id}")
                    Result.Success(createdFile.id)
                } else {
                    Log.d("GoogleApiService", "Using existing root folder with ID: ${files[0].id}")
                    Result.Success(files[0].id)
                }
            } catch (e: UserRecoverableAuthIOException) {
                Log.e("GoogleApiService", "User recoverable auth error getting or creating app root folder.", e)
                Result.UserRecoverableError(e)
            } catch (e: IOException) {
                Log.e("GoogleApiService", "Failed to get or create app root folder.", e)
                Result.Error(e)
            }
        }

    suspend fun getFileMetadata(fileId: String): Result<File> = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
        try {
            val file = drive.files().get(fileId).setFields("id, name, modifiedTime").execute()
            Result.Success(file)
        } catch (e: UserRecoverableAuthIOException) {
            Result.UserRecoverableError(e)
        } catch (e: IOException) {
            Result.Error(e)
        }
    }

    suspend fun updateFile(
        fileId: String,
        file: java.io.File,
        mimeType: String,
    ): Result<File?> =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
            try {
                val fileMetadata =
                    File().apply {
                        name = file.name
                    }
                val mediaContent = FileContent(mimeType, file)
                val uploadedFile =
                    drive
                        .files()
                        .update(fileId, fileMetadata, mediaContent)
                        .setFields("id, name, webViewLink")
                        .execute()
                Result.Success(uploadedFile)
            } catch (e: IOException) {
                Result.Error(e)
            }
        }

    suspend fun listFiles(folderId: String): Result<List<File>> = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
        try {
            val files = drive.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setFields("files(id, name, modifiedTime)")
                .execute()
                .files
            Result.Success(files)
        } catch (e: UserRecoverableAuthIOException) {
            Result.UserRecoverableError(e)
        } catch (e: IOException) {
            Result.Error(e)
        }
    }

    suspend fun downloadFile(fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
        try {
            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            Result.Success(outputStream.toByteArray())
        } catch (e: UserRecoverableAuthIOException) {
            Result.UserRecoverableError(e)
        } catch (e: IOException) {
            Result.Error(e)
        }
    }

    suspend fun uploadFolder(
        folder: java.io.File,
        parentId: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
            try {
                // 1. Create the folder on Google Drive
                val folderMetadata =
                    File().apply {
                        name = folder.name
                        mimeType = "application/vnd.google-apps.folder"
                        parents = listOf(parentId)
                    }
                val createdFolder =
                    drive
                        .files()
                        .create(folderMetadata)
                        .setFields("id")
                        .execute()

                // 2. List files in the local folder
                val filesToUpload = folder.listFiles() ?: emptyArray()

                // 3. Upload each file
                for (file in filesToUpload) {
                    if (file.isFile) {
                        val mimeType = when (file.extension.lowercase()) {
                            "jpg", "jpeg" -> "image/jpeg"
                            "png" -> "image/png"
                            "mp3" -> "audio/mpeg"
                            "m4a" -> "audio/mp4"
                            "mp4" -> "video/mp4"
                            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            else -> "application/octet-stream"
                        }
                        uploadFile(file, createdFolder.id, mimeType)
                    }
                }
                Result.Success(Unit)
            } catch (e: IOException) {
                Result.Error(e)
            }
        }

    suspend fun createFolder(folderName: String, parentId: String): Result<String> =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
            try {
                val fileMetadata =
                    File().apply {
                        name = folderName
                        mimeType = "application/vnd.google-apps.folder"
                        parents = listOf(parentId)
                    }
                val createdFile =
                    drive
                        .files()
                        .create(fileMetadata)
                        .setFields("id")
                        .execute()
                Result.Success(createdFile.id)
            } catch (e: UserRecoverableAuthIOException) {
                Result.UserRecoverableError(e)
            } catch (e: IOException) {
                Result.Error(e)
            }
        }

    suspend fun getOrCreateCaseRegistrySpreadsheetId(folderId: String): String =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: throw IOException("Credential not available for Drive service")
            val sheets = getSheetsService() ?: throw IOException("Credential not available for Sheets service")
            try {
                val fileName = "CaseRegistry"
                val query =
                    "mimeType='application/vnd.google-apps.spreadsheet' " +
                        "and name='$fileName' and trashed=false and '$folderId' in parents"
                val files =
                    drive
                        .files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .execute()
                        .files
                if (files.isNullOrEmpty()) {
                    val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(fileName))
                    val createdSheet =
                        sheets
                            .spreadsheets()
                            .create(spreadsheet)
                            .setFields("spreadsheetId")
                            .execute()
                    drive
                        .files()
                        .update(createdSheet.spreadsheetId, null)
                        .setAddParents(folderId)
                        .execute()
                    createdSheet.spreadsheetId
                } else {
                    files[0].id
                }
            } catch (e: IOException) {
                throw IOException("Failed to get or create case registry spreadsheet", e)
            }
        }

    suspend fun createSpreadsheet(
        title: String,
        folderId: String,
    ): Result<String?> =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
            val sheets = getSheetsService() ?: return@withContext Result.Error(IOException("Credential not available for Sheets service"))
            Log.d("GoogleApiService", "Creating spreadsheet with title: $title in folder: $folderId")
            try {
                val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(title))
                val createdSheet =
                    sheets
                        .spreadsheets()
                        .create(spreadsheet)
                        .setFields("spreadsheetId")
                        .execute()
                Log.d("GoogleApiService", "Spreadsheet created with ID: ${createdSheet.spreadsheetId}")
                drive
                    .files()
                    .update(createdSheet.spreadsheetId, null)
                    .setAddParents(folderId)
                    .execute()
                Log.d("GoogleApiService", "Spreadsheet moved to folder: $folderId")
                Result.Success(createdSheet.spreadsheetId)
            } catch (e: UserRecoverableAuthIOException) {
                Log.e("GoogleApiService", "User recoverable auth error creating spreadsheet.", e)
                Result.UserRecoverableError(e)
            } catch (e: IOException) {
                Log.e("GoogleApiService", "Failed to create spreadsheet.", e)
                Result.Error(e)
            }
        }

    @Suppress("ktlint:standard:max-line-length")
    suspend fun createAllegationsSheet(): String? =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext null
            val sheets = getSheetsService() ?: return@withContext null
            try {
                val folderIdResult = getOrCreateAppRootFolder()
                if (folderIdResult is Result.Success) {
                    val folderId = folderIdResult.data
                    val fileName = "Lexorcist - Allegations"
                    val query = "mimeType='application/vnd.google-apps.spreadsheet' and name='$fileName' and trashed=false and '$folderId' in parents"
                    val files =
                        drive
                            .files()
                            .list()
                            .setQ(query)
                            .setSpaces("drive")
                            .execute()
                            .files
                    if (files.isNullOrEmpty()) {
                        val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(fileName))
                        val createdSheet =
                            sheets
                                .spreadsheets()
                                .create(spreadsheet)
                                .setFields("spreadsheetId")
                                .execute()
                        drive
                            .files()
                            .update(createdSheet.spreadsheetId, null)
                            .setAddParents(folderId)
                            .execute()
                        android.util.Log.d("GoogleApiService", "Created allegations sheet with ID: ${createdSheet.spreadsheetId}")
                        createdSheet.spreadsheetId
                    } else {
                        files[0].id
                    }
                } else {
                    null
                }
            } catch (e: IOException) {
                null
            }
        }

    suspend fun writeData(
        spreadsheetId: String,
        sheetName: String,
        values: List<List<Any>>,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext false
            try {
                val body = ValueRange().setValues(values)
                sheets
                    .spreadsheets()
                    .values()
                    .update(spreadsheetId, sheetName, body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }

    suspend fun clearSheet(
        spreadsheetId: String,
        sheetName: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext false
            try {
                val request = ClearValuesRequest()
                sheets
                    .spreadsheets()
                    .values()
                    .clear(spreadsheetId, sheetName, request)
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }

    @Suppress("ktlint:standard:max-line-length")
    suspend fun getOrCreateCaseFolder(caseName: String): String? {
        val drive = getDriveService() ?: return null
        val appRootFolderIdResult = getOrCreateAppRootFolder()
        if (appRootFolderIdResult is Result.Success) {
            val appRootFolderId = appRootFolderIdResult.data
            return withContext(Dispatchers.IO) {
                try {
                    val query = "mimeType='application/vnd.google-apps.folder' and name='$caseName' and trashed=false and '$appRootFolderId' in parents"
                    val files =
                        drive
                            .files()
                            .list()
                            .setQ(query)
                            .setSpaces("drive")
                            .execute()
                            .files
                    if (files.isNullOrEmpty()) {
                        val fileMetadata =
                            File().apply {
                                name = caseName
                                mimeType = "application/vnd.google-apps.folder"
                                parents = listOf(appRootFolderId)
                            }
                        drive
                            .files()
                            .create(fileMetadata)
                            .setFields("id")
                            .execute()
                            .id
                    } else {
                        files[0].id
                    }
                } catch (e: IOException) {
                    null
                }
            }
        }
        return null
    }

    @Suppress("ktlint:standard:max-line-length")
    suspend fun getOrCreateEvidenceFolder(caseName: String): String? {
        val drive = getDriveService() ?: return null
        val caseFolderId = getOrCreateCaseFolder(caseName) ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val folderName = "Evidence"
                val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false and '$caseFolderId' in parents"
                val files =
                    drive
                        .files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .execute()
                        .files
                if (files.isNullOrEmpty()) {
                    val fileMetadata =
                        File().apply {
                            name = folderName
                            mimeType = "application/vnd.google-apps.folder"
                            parents = listOf(caseFolderId)
                        }
                    drive
                        .files()
                        .create(fileMetadata)
                        .setFields("id")
                        .execute()
                        .id
                } else {
                    files[0].id
                }
            } catch (e: IOException) {
                null
            }
        }
    }

    suspend fun addCaseToRegistry(
        registryId: String,
        case: Case,
    ): Boolean {
        val sheets = getSheetsService() ?: return false
        android.util.Log.d("GoogleApiService", "addCaseToRegistry called for case: ${case.name}")
        return withContext(Dispatchers.IO) {
            try {
                val values =
                    listOf(
                        listOf(
                            case.id.toString(),
                            case.name,
                            case.spreadsheetId,
                            case.scriptId ?: "",
                            case.generatedPdfId ?: "",
                            case.sourceHtmlSnapshotId ?: "",
                            case.originalMasterHtmlTemplateId ?: "",
                            case.folderId ?: "",
                            case.plaintiffs ?: "",
                            case.defendants ?: "",
                            case.court ?: "",
                            case.isArchived.toString(),
                            case.lastModifiedTime?.toString() ?: "",
                        ),
                    )
                val body = ValueRange().setValues(values)
                sheets
                    .spreadsheets()
                    .values()
                    .append(registryId, "Sheet1", body)
                    .setValueInputOption("RAW")
                    .execute()
                android.util.Log.d("GoogleApiService", "addCaseToRegistry successful")
                true
            } catch (e: IOException) {
                android.util.Log.e("GoogleApiService", "IOException in addCaseToRegistry: $e")
                false
            }
        }
    }

    suspend fun getAllCasesFromRegistry(registryId: String): List<Case> {
        val sheets = getSheetsService() ?: return emptyList()
        android.util.Log.d("GoogleApiService", "getAllCasesFromRegistry called")
        return withContext(Dispatchers.IO) {
            try {
                val range = "Sheet1!A:M"
                val response =
                    sheets
                        .spreadsheets()
                        .values()
                        .get(registryId, range)
                        .execute()
                val values = response.getValues()
                if (values.isNullOrEmpty()) {
                    android.util.Log.d("GoogleApiService", "Registry is empty")
                    emptyList()
                } else {
                    android.util.Log.d("GoogleApiService", "Found ${values.size} rows in registry")
                    values.mapNotNull { row ->
                        if (row.size >= 3) {
                            try {
                                Case(
                                    id = row[0].toString().toInt(),
                                    name = row[1].toString(),
                                    spreadsheetId = row[2].toString(),
                                    scriptId = row.getOrNull(3)?.toString(),
                                    generatedPdfId = row.getOrNull(4)?.toString(),
                                    sourceHtmlSnapshotId = row.getOrNull(5)?.toString(),
                                    originalMasterHtmlTemplateId = row.getOrNull(6)?.toString(),
                                    folderId = row.getOrNull(7)?.toString(),
                                    plaintiffs = row.getOrNull(8)?.toString(),
                                    defendants = row.getOrNull(9)?.toString(),
                                    court = row.getOrNull(10)?.toString(),
                                    isArchived = row.getOrNull(11)?.toString()?.toBoolean() ?: false,
                                    lastModifiedTime = row.getOrNull(12)?.toString()?.toLongOrNull(),
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("GoogleApiService", "Error parsing row: $row", e)
                                null // Skip row if parsing fails
                            }
                        } else {
                            android.util.Log.w("GoogleApiService", "Skipping row with not enough columns: $row")
                            null // Skip row if not enough columns
                        }
                    }
                }
            } catch (e: IOException) {
                android.util.Log.e("GoogleApiService", "IOException in getAllCasesFromRegistry: $e")
                emptyList()
            }
        }
    }

    suspend fun updateCaseInRegistry(case: Case): Boolean {
        val sheets = getSheetsService() ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val registryIdResult = getOrCreateAppRootFolder()
                if (registryIdResult is Result.Success) {
                    val registryId = getOrCreateCaseRegistrySpreadsheetId(registryIdResult.data)
                    val range = "Sheet1!A:A" // Assuming IDs are in column A
                    val response =
                        sheets
                            .spreadsheets()
                            .values()
                            .get(registryId, range)
                            .execute()
                    val values = response.getValues()
                    if (values.isNullOrEmpty()) return@withContext false

                    val rowIndex = values.indexOfFirst { it.isNotEmpty() && it[0].toString() == case.id.toString() }
                    if (rowIndex == -1) return@withContext false

                    val rowData =
                        listOf(
                            case.id.toString(),
                            case.name,
                            case.spreadsheetId,
                            case.scriptId ?: "",
                            case.generatedPdfId ?: "",
                            case.sourceHtmlSnapshotId ?: "",
                            case.originalMasterHtmlTemplateId ?: "",
                            case.folderId ?: "",
                            case.plaintiffs ?: "",
                            case.defendants ?: "",
                            case.court ?: "",
                            case.isArchived.toString(),
                            System.currentTimeMillis().toString(),
                        )
                    val valueRange = ValueRange().setValues(listOf(rowData))
                    val updateRange = "Sheet1!A${rowIndex + 1}"
                    sheets
                        .spreadsheets()
                        .values()
                        .update(registryId, updateRange, valueRange)
                        .setValueInputOption("RAW")
                        .execute()
                    true
                } else {
                    false
                }
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun deleteCaseFromRegistry(case: Case): Boolean {
        val sheets = getSheetsService() ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val registryIdResult = getOrCreateAppRootFolder()
                if (registryIdResult is Result.Success) {
                    val registryId = getOrCreateCaseRegistrySpreadsheetId(registryIdResult.data)
                    val range = "Sheet1!A:A" // Assuming IDs are in column A
                    val response =
                        sheets
                            .spreadsheets()
                            .values()
                            .get(registryId, range)
                            .execute()
                    val values = response.getValues()
                    if (values.isNullOrEmpty()) return@withContext false

                    val rowIndex = values.indexOfFirst { it.isNotEmpty() && it[0].toString() == case.id.toString() }
                    if (rowIndex == -1) return@withContext false

                    val sheetId =
                        sheets
                            .spreadsheets()
                            .get(registryId)
                            .execute()
                            .sheets
                            .firstOrNull { it.properties.title == "Sheet1" }
                            ?.properties
                            ?.sheetId ?: 0

                    val request =
                        Request().setDeleteDimension(
                            DeleteDimensionRequest()
                                .setRange(
                                    DimensionRange()
                                        .setSheetId(sheetId)
                                        .setDimension("ROWS")
                                        .setStartIndex(rowIndex)
                                        .setEndIndex(rowIndex + 1),
                                ),
                        )

                    val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
                    sheets.spreadsheets().batchUpdate(registryId, batchUpdateRequest).execute()
                    true
                } else {
                    false
                }
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun deleteFolder(folderId: String): Boolean =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext false
            try {
                drive.files().delete(folderId).execute()
                true
            } catch (e: IOException) {
                false
            }
        }

    suspend fun uploadFile(
        file: java.io.File,
        folderId: String,
        mimeType: String,
    ): Result<File?> =
        withContext(Dispatchers.IO) {
            val drive = getDriveService() ?: return@withContext Result.Error(IOException("Credential not available for Drive service"))
            try {
                val fileMetadata =
                    File().apply {
                        name = file.name
                        parents = listOf(folderId)
                    }
                val mediaContent = FileContent(mimeType, file)
                val uploadedFile =
                    drive
                        .files()
                        .create(fileMetadata, mediaContent)
                        .setFields("id, name, webViewLink")
                        .execute()
                Result.Success(uploadedFile)
            } catch (e: IOException) {
                Result.Error(e)
            }
        }

    suspend fun attachScript(
        spreadsheetId: String,
        scriptContent: String,
        scriptId: String,
    ): String? {
        // This is a complex operation that requires the Script API.
        // For simplicity, this is not implemented.
        return null
    }

    suspend fun readSpreadsheet(spreadsheetId: String): Map<String, List<List<Any>>>? =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext null
            try {
                val spreadsheet =
                    sheets
                        .spreadsheets()
                        .get(spreadsheetId)
                        .setIncludeGridData(false)
                        .execute()
                val sheetData = mutableMapOf<String, List<List<Any>>>()
                spreadsheet.sheets.forEach { sheet ->
                    val range = "${sheet.properties.title}!A1:Z"
                    val response =
                        sheets
                            .spreadsheets()
                            .values()
                            .get(spreadsheetId, range)
                            .execute()
                    val values = response.getValues()
                    if (!values.isNullOrEmpty()) {
                        sheetData[sheet.properties.title] = values
                    }
                }
                sheetData
            } catch (e: IOException) {
                null
            }
        }

    suspend fun addSheet(
        spreadsheetId: String,
        sheetName: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext false
            try {
                val requests =
                    listOf(
                        Request().setAddSheet(
                            AddSheetRequest().setProperties(
                                SheetProperties().setTitle(sheetName),
                            ),
                        ),
                    )
                val body = BatchUpdateSpreadsheetRequest().setRequests(requests)
                sheets.spreadsheets().batchUpdate(spreadsheetId, body).execute()
                true
            } catch (e: IOException) {
                false
            }
        }

    suspend fun appendData(
        spreadsheetId: String,
        range: String,
        values: List<List<Any>>,
    ): AppendValuesResponse? =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext null
            try {
                val body = ValueRange().setValues(values)
                sheets
                    .spreadsheets()
                    .values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute()
            } catch (e: IOException) {
                null
            }
        }

    suspend fun getAllegationsForCase(
        spreadsheetId: String,
        caseId: Int, // caseId is not used to fetch sheets or drive
    ): List<com.hereliesaz.lexorcist.data.Allegation> =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext emptyList()
            try {
                val range = "Allegations!A:C" // Assuming allegations are in a sheet named "Allegations"
                val response =
                    sheets
                        .spreadsheets()
                        .values()
                        .get(spreadsheetId, range)
                        .execute()
                val values = response.getValues()
                if (values.isNullOrEmpty()) {
                    emptyList()
                } else {
                    values.mapNotNull { row ->
                        try {
                            com.hereliesaz.lexorcist.data.Allegation(
                                id = row[0].toString().toInt(),
                                spreadsheetId = spreadsheetId,
                                text = row[2].toString(),
                            )
                        } catch (e: Exception) {
                            null // Skip row if parsing fails
                        }
                    }
                }
            } catch (e: IOException) {
                emptyList()
            }
        }

    suspend fun getEvidenceForCase(
        spreadsheetId: String,
        caseId: Long, // caseId is used in Evidence constructor
    ): List<com.hereliesaz.lexorcist.data.Evidence> =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext emptyList()
            try {
                val range = "Evidence!A:Z" // Assuming evidence is in a sheet named "Evidence"
                val response =
                    sheets
                        .spreadsheets()
                        .values()
                        .get(spreadsheetId, range)
                        .execute()
                val values = response.getValues()
                if (values.isNullOrEmpty()) {
                    emptyList()
                } else {
                    values.mapNotNull { row ->
                        try {
                            com.hereliesaz.lexorcist.data.Evidence(
                                id = row[0].toString().toInt(),
                                caseId = caseId,
                                spreadsheetId = spreadsheetId,
                                type = row[1].toString(),
                                content = row[2].toString(),
                                timestamp = row[3].toString().toLong(),
                                sourceDocument = row[4].toString(),
                                documentDate = row[5].toString().toLong(),
                                allegationId = row.getOrNull(6)?.toString()?.toIntOrNull(),
                                category = row.getOrNull(7)?.toString() ?: "",
                                tags = row.getOrNull(8)?.toString()?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                                commentary = row.getOrNull(9)?.toString(),
                                linkedEvidenceIds = row.getOrNull(10)?.toString()?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList(),
                                parentVideoId = row.getOrNull(11)?.toString(),
                                entities =
                                    row
                                        .getOrNull(12)
                                        ?.toString()
                                        ?.let { Gson().fromJson(it, object : TypeToken<Map<String, List<String>>>() {}.type) }
                                        ?: emptyMap(),
                                isSelected = row.getOrNull(13)?.toString()?.toBoolean() ?: false,
                                formattedContent = row.getOrNull(14)?.toString(),
                                mediaUri = row.getOrNull(15)?.toString(),
                            )
                        } catch (e: Exception) {
                            null // Skip row if parsing fails
                        }
                    }
                }
            } catch (e: IOException) {
                emptyList()
            }
        }

    suspend fun addEvidenceToCase(evidence: com.hereliesaz.lexorcist.data.Evidence): AppendValuesResponse? =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext null
            try {
                val values =
                    listOf(
                        listOf(
                            evidence.id.toString(),
                            evidence.type,
                            evidence.content,
                            evidence.timestamp.toString(),
                            evidence.sourceDocument,
                            evidence.documentDate.toString(),
                            evidence.allegationId?.toString() ?: "",
                            evidence.category,
                            evidence.tags.joinToString(","),
                            evidence.commentary ?: "",
                            evidence.linkedEvidenceIds.joinToString(","),
                            evidence.parentVideoId ?: "",
                            Gson().toJson(evidence.entities),
                            evidence.isSelected.toString(),
                            evidence.formattedContent,
                            evidence.mediaUri,
                        ),
                    )
                val body = ValueRange().setValues(values)
                sheets
                    .spreadsheets()
                    .values()
                    .append(evidence.spreadsheetId, "Evidence!A1", body)
                    .setValueInputOption("RAW")
                    .execute()
            } catch (e: IOException) {
                null
            }
        }

    suspend fun addAllegationToCase(
        spreadsheetId: String,
        allegationText: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext false
            try {
                val values =
                    listOf(
                        listOf(
                            // This is not ideal, we should get the last id and increment it
                            (System.currentTimeMillis() / 1000).toInt(),
                            spreadsheetId,
                            allegationText,
                        ),
                    )
                val body = ValueRange().setValues(values)
                sheets
                    .spreadsheets()
                    .values()
                    .append(spreadsheetId, "Allegations!A1", body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }

    suspend fun updateEvidenceInSheet(evidence: com.hereliesaz.lexorcist.data.Evidence): Boolean {
        val sheets = getSheetsService() ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val range = "Evidence!A:A" // Assuming IDs are in column A
                val response =
                    sheets
                        .spreadsheets()
                        .values()
                        .get(evidence.spreadsheetId, range)
                        .execute()
                val values = response.getValues()
                if (values.isNullOrEmpty()) return@withContext false

                val rowIndex = values.indexOfFirst { it.isNotEmpty() && it[0].toString() == evidence.id.toString() }
                if (rowIndex == -1) return@withContext false

                val rowData =
                    listOf(
                        evidence.id.toString(),
                        evidence.type,
                        evidence.content,
                        evidence.timestamp.toString(),
                        evidence.sourceDocument,
                        evidence.documentDate.toString(),
                        evidence.allegationId?.toString() ?: "",
                        evidence.category,
                        evidence.tags.joinToString(","),
                        evidence.commentary ?: "",
                        evidence.linkedEvidenceIds.joinToString(","),
                        evidence.parentVideoId ?: "",
                        Gson().toJson(evidence.entities),
                        evidence.isSelected.toString(),
                        evidence.formattedContent,
                        evidence.mediaUri,
                    )
                val valueRange = ValueRange().setValues(listOf(rowData))
                val updateRange = "Evidence!A${rowIndex + 1}"
                sheets
                    .spreadsheets()
                    .values()
                    .update(evidence.spreadsheetId, updateRange, valueRange)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun deleteEvidenceFromSheet(evidence: com.hereliesaz.lexorcist.data.Evidence): Boolean {
        val sheets = getSheetsService() ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val range = "Evidence!A:A" // Assuming IDs are in column A
                val response =
                    sheets
                        .spreadsheets()
                        .values()
                        .get(evidence.spreadsheetId, range)
                        .execute()
                val values = response.getValues()
                if (values.isNullOrEmpty()) return@withContext false

                val rowIndex = values.indexOfFirst { it.isNotEmpty() && it[0].toString() == evidence.id.toString() }
                if (rowIndex == -1) return@withContext false

                val sheetId =
                    sheets
                        .spreadsheets()
                        .get(evidence.spreadsheetId)
                        .execute()
                        .sheets
                        .firstOrNull { it.properties.title == "Evidence" }
                        ?.properties
                        ?.sheetId ?: 0

                val request =
                    Request().setDeleteDimension(
                        DeleteDimensionRequest()
                            .setRange(
                                DimensionRange()
                                    .setSheetId(sheetId)
                                    .setDimension("ROWS")
                                    .setStartIndex(rowIndex)
                                    .setEndIndex(rowIndex + 1),
                            ),
                    )

                val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
                sheets.spreadsheets().batchUpdate(evidence.spreadsheetId, batchUpdateRequest).execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun listHtmlTemplatesInAppRootFolder(): List<File> {
        val drive = getDriveService() ?: return emptyList()
        val appRootFolderIdResult = getOrCreateAppRootFolder()
        if (appRootFolderIdResult is Result.Success) {
            val appRootFolderId = appRootFolderIdResult.data
            return withContext(Dispatchers.IO) {
                try {
                    val query = "mimeType='text/html' and trashed=false and '$appRootFolderId' in parents"
                    drive
                        .files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .execute()
                        .files ?: emptyList()
                } catch (e: IOException) {
                    emptyList()
                }
            }
        }
        return emptyList()
    }

    suspend fun getSharedScripts(): List<Script> {
        val sheets = getSheetsService() ?: return emptyList()
        val spreadsheetId = "18hB2Kx5Le1uaF2pImeITgWntcBB-JfYxvpU2aqTzRr8" // This should probably be a constant or configurable
        val sheetData = readSpreadsheet(spreadsheetId) // This uses sheets internally
        val scriptSheet = sheetData?.get("Scripts") ?: return emptyList()

        return scriptSheet.mapNotNull { row ->
            if (row.size >= 7) {
                Script(
                    id = row[0].toString(),
                    name = row[1].toString(),
                    description = row[2].toString(),
                    content = row[3].toString(),
                    author = row[4].toString(),
                    rating = row[5].toString().toDoubleOrNull() ?: 0.0,
                    numRatings = row[6].toString().toIntOrNull() ?: 0,
                )
            } else {
                null
            }
        }
    }

    suspend fun getSharedTemplates(): List<Template> {
        val sheets = getSheetsService() ?: return emptyList()
        val spreadsheetId = "18hB2Kx5Le1uaF2pImeITgWntcBB-JfYxvpU2aqTzRr8" // This should probably be a constant or configurable
        val sheetData = readSpreadsheet(spreadsheetId) // This uses sheets internally
        val templateSheet = sheetData?.get("Templates") ?: return emptyList()

        return templateSheet.mapNotNull { row ->
            if (row.size >= 7) {
                Template(
                    id = row[0].toString(),
                    name = row[1].toString(),
                    description = row[2].toString(),
                    content = row[3].toString(),
                    author = row[4].toString(),
                    rating = row[5].toString().toDoubleOrNull() ?: 0.0,
                    numRatings = row[6].toString().toIntOrNull() ?: 0,
                )
            } else {
                null
            }
        }
    }

    suspend fun shareAddon(
        name: String,
        description: String,
        content: String,
        type: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext false
            try {
                val spreadsheetId = "18hB2Kx5Le1uaF2pImeITgWntcBB-JfYxvpU2aqTzRr8" // Constant
                val sheetName = if (type == "Script") "Scripts" else "Templates"
                val range = "$sheetName!A:A"
                val response =
                    sheets
                        .spreadsheets()
                        .values()
                        .get(spreadsheetId, range)
                        .execute()
                val lastRow = response.getValues()?.size ?: 0
                val newId = "$sheetName-${lastRow + 1}"

                val values =
                    listOf(
                        listOf(
                            newId,
                            name,
                            description,
                            content,
                            "self", // Author
                            0.0, // Rating
                            0, // NumRatings
                        ),
                    )
                val body = ValueRange().setValues(values)
                sheets
                    .spreadsheets()
                    .values()
                    .append(spreadsheetId, sheetName, body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }

    suspend fun rateAddon(
        id: String,
        rating: Int,
        type: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            // TODO: Implement actual logic to rate an addon
            // This might involve updating metadata for the shared addon file or a registry.
            false
        }

    suspend fun getSelectedAllegations(spreadsheetId: String): List<String> =
        withContext(Dispatchers.IO) {
            val sheets = getSheetsService() ?: return@withContext emptyList()
            try {
                val range = "SelectedAllegations!A:A"
                val response =
                    sheets
                        .spreadsheets()
                        .values()
                        .get(spreadsheetId, range)
                        .execute()
                val values = response.getValues()
                if (values.isNullOrEmpty()) {
                    emptyList()
                } else {
                    values.map { it[0].toString() }
                }
            } catch (e: IOException) {
                emptyList()
            }
        }

    suspend fun updateSelectedAllegations(
        spreadsheetId: String,
        allegations: List<String>,
    ) {
        val sheets = getSheetsService() ?: return // Or throw, or return Result
        withContext(Dispatchers.IO) {
            try {
                val sheetData = readSpreadsheet(spreadsheetId) // Uses sheets internally
                if (sheetData?.get("SelectedAllegations") == null) {
                    addSheet(spreadsheetId, "SelectedAllegations") // Uses sheets internally
                } else {
                    clearSheet(spreadsheetId, "SelectedAllegations") // Uses sheets internally
                }
                val values = allegations.map { listOf(it) }
                writeData(spreadsheetId, "SelectedAllegations!A1", values) // Uses sheets internally
            } catch (e: IOException) {
                // Handle error
            }
        }
    }
}
