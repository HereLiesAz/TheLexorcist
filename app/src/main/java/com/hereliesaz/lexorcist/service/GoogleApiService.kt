package com.hereliesaz.lexorcist.service

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
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
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class GoogleApiService(
    private val drive: Drive,
    private val sheets: Sheets,
) {
    constructor(credential: GoogleAccountCredential, applicationName: String) : this(
        Drive
            .Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential) // Changed here
            .setApplicationName(applicationName)
            .build(),
        Sheets
            .Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential) // Changed here
            .setApplicationName(applicationName)
            .build(),
    )

    suspend fun getOrCreateAppRootFolder(): String =
        withContext(Dispatchers.IO) {
            try {
                val folderName = "Lexorcist"
                val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false"
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
                        }
                    val createdFile =
                        drive
                            .files()
                            .create(fileMetadata)
                            .setFields("id")
                            .execute()
                    createdFile.id
                } else {
                    files[0].id
                }
            } catch (e: IOException) {
                throw IOException("Failed to get or create app root folder", e)
            }
        }

    suspend fun getOrCreateCaseRegistrySpreadsheetId(folderId: String): String =
        withContext(Dispatchers.IO) {
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
            try {
                val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(title))
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
                Result.Success(createdSheet.spreadsheetId)
            } catch (e: IOException) {
                Result.Error(e)
            }
        }

    @Suppress("ktlint:standard:max-line-length")
    suspend fun createAllegationsSheet(): String? =
        withContext(Dispatchers.IO) {
            try {
                val folderId = getOrCreateAppRootFolder()
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
        val appRootFolderId = getOrCreateAppRootFolder() ?: return null
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

    @Suppress("ktlint:standard:max-line-length")
    suspend fun getOrCreateEvidenceFolder(caseName: String): String? {
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
        return withContext(Dispatchers.IO) {
            try {
                val registryId = getOrCreateCaseRegistrySpreadsheetId(getOrCreateAppRootFolder())
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
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun deleteCaseFromRegistry(case: Case): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val registryId = getOrCreateCaseRegistrySpreadsheetId(getOrCreateAppRootFolder())
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
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun deleteFolder(folderId: String): Boolean =
        withContext(Dispatchers.IO) {
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
        caseId: Int,
    ): List<com.hereliesaz.lexorcist.data.Allegation> =
        withContext(Dispatchers.IO) {
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
        caseId: Long,
    ): List<com.hereliesaz.lexorcist.data.Evidence> =
        withContext(Dispatchers.IO) {
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
                                category = row.getOrNull(7).toString(),
                                tags =
                                    row
                                        .getOrNull(8)
                                        .toString()
                                        .split(",")
                                        .map { it.trim() },
                                commentary = row.getOrNull(9)?.toString(),
                                linkedEvidenceIds =
                                    row
                                        .getOrNull(10)
                                        .toString()
                                        .split(",")
                                        .mapNotNull { it.trim().toIntOrNull() },
                                parentVideoId = row.getOrNull(11)?.toString(),
                                entities =
                                    row
                                        .getOrNull(12)
                                        ?.toString()
                                        ?.let { Gson().fromJson(it, object : TypeToken<Map<String, List<String>>>() {}.type) }
                                        ?: emptyMap(),
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

    suspend fun addEvidenceToCase(evidence: com.hereliesaz.lexorcist.data.Evidence): Boolean =
        withContext(Dispatchers.IO) {
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
                        ),
                    )
                val body = ValueRange().setValues(values)
                sheets
                    .spreadsheets()
                    .values()
                    .append(evidence.spreadsheetId, "Evidence!A1", body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }

    suspend fun addAllegationToCase(
        spreadsheetId: String,
        allegationText: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
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
        val appRootFolderId = getOrCreateAppRootFolder() ?: return emptyList()
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

    suspend fun getSharedScripts(): List<Script> {
        val spreadsheetId = "18hB2Kx5Le1uaF2pImeITgWntcBB-JfYxvpU2aqTzRr8"
        val sheetData = readSpreadsheet(spreadsheetId)
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
        val spreadsheetId = "18hB2Kx5Le1uaF2pImeITgWntcBB-JfYxvpU2aqTzRr8"
        val sheetData = readSpreadsheet(spreadsheetId)
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
            // TODO: Implement actual logic to share an addon (script or template)
            // This might involve creating a file in a shared Drive folder and updating a registry.
            false
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
}
