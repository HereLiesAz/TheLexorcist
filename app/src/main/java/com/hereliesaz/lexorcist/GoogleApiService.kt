package com.hereliesaz.lexorcist

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.model.Script // Added import
import com.hereliesaz.lexorcist.model.Template // Added import
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class GoogleApiService(
    private val drive: Drive,
    private val sheets: Sheets
) {

    constructor(credential: GoogleAccountCredential, applicationName: String) : this(
        Drive.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName(applicationName)
            .build(),
        Sheets.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName(applicationName)
            .build()
    )

    suspend fun getOrCreateAppRootFolder(): String = withContext(Dispatchers.IO) {
        try {
            val folderName = "Lexorcist"
            val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false"
            val files = drive.files().list().setQ(query).setSpaces("drive").execute().files
            if (files.isNullOrEmpty()) {
                val fileMetadata = File().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                }
                val createdFile = drive.files().create(fileMetadata).setFields("id").execute()
                createdFile.id
            } else {
                files[0].id
            }
        } catch (e: IOException) {
            throw IOException("Failed to get or create app root folder", e)
        }
    }

    suspend fun getOrCreateCaseRegistrySpreadsheetId(folderId: String): String = withContext(Dispatchers.IO) {
        try {
            val fileName = "CaseRegistry"
            val query = "mimeType='application/vnd.google-apps.spreadsheet' and name='$fileName' and trashed=false and '$folderId' in parents"
            val files = drive.files().list().setQ(query).setSpaces("drive").execute().files
            if (files.isNullOrEmpty()) {
                val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(fileName))
                val createdSheet = sheets.spreadsheets().create(spreadsheet).setFields("spreadsheetId").execute()
                drive.files().update(createdSheet.spreadsheetId, null)
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

    suspend fun createSpreadsheet(title: String, folderId: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(title))
                val createdSheet = sheets.spreadsheets().create(spreadsheet).setFields("spreadsheetId").execute()
                drive.files().update(createdSheet.spreadsheetId, null)
                    .setAddParents(folderId)
                    .execute()
                Result.Success(createdSheet.spreadsheetId)
            } catch (e: IOException) {
                Result.Error(e)
            }
        }
    }

    suspend fun writeData(spreadsheetId: String, sheetName: String, values: List<List<Any>>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val body = ValueRange().setValues(values)
                sheets.spreadsheets().values().update(spreadsheetId, sheetName, body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun clearSheet(spreadsheetId: String, sheetName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = ClearValuesRequest()
                sheets.spreadsheets().values().clear(spreadsheetId, sheetName, request).execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }
    
    suspend fun getOrCreateCaseFolder(caseName: String): String? {
        val appRootFolderId = getOrCreateAppRootFolder() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val query = "mimeType='application/vnd.google-apps.folder' and name='$caseName' and trashed=false and '$appRootFolderId' in parents"
                val files = drive.files().list().setQ(query).setSpaces("drive").execute().files
                if (files.isNullOrEmpty()) {
                    val fileMetadata = File().apply {
                        name = caseName
                        mimeType = "application/vnd.google-apps.folder"
                        parents = listOf(appRootFolderId)
                    }
                    drive.files().create(fileMetadata).setFields("id").execute().id
                } else {
                    files[0].id
                }
            } catch (e: IOException) {
                null
            }
        }
    }

    suspend fun getOrCreateEvidenceFolder(caseName: String): String? {
        val caseFolderId = getOrCreateCaseFolder(caseName) ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val folderName = "Evidence"
                val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false and '$caseFolderId' in parents"
                val files = drive.files().list().setQ(query).setSpaces("drive").execute().files
                if (files.isNullOrEmpty()) {
                    val fileMetadata = File().apply {
                        name = folderName
                        mimeType = "application/vnd.google-apps.folder"
                        parents = listOf(caseFolderId)
                    }
                    drive.files().create(fileMetadata).setFields("id").execute().id
                } else {
                    files[0].id
                }
            } catch (e: IOException) {
                null
            }
        }
    }

    suspend fun addCaseToRegistry(registryId: String, case: Case): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val values = listOf(
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
                        case.lastModifiedTime?.toString() ?: ""
                    )
                )
                val body = ValueRange().setValues(values)
                sheets.spreadsheets().values().append(registryId, "Sheet1", body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun getAllCasesFromRegistry(registryId: String): List<Case> {
        return withContext(Dispatchers.IO) {
            try {
                val range = "Sheet1!A:M"
                val response = sheets.spreadsheets().values().get(registryId, range).execute()
                val values = response.getValues()
                if (values.isNullOrEmpty()) {
                    emptyList()
                } else {
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
                                    lastModifiedTime = row.getOrNull(12)?.toString()?.toLongOrNull()
                                )
                            } catch (e: Exception) {
                                null // Skip row if parsing fails
                            }
                        } else {
                            null // Skip row if not enough columns
                        }
                    }
                }
            } catch (e: IOException) {
                emptyList()
            }
        }
    }
    
    suspend fun updateCaseInRegistry(case: Case): Boolean {
        // This is a complex operation. You need to find the row to update.
        // For simplicity, this is not implemented.
        return false
    }

    suspend fun deleteCaseFromRegistry(case: Case): Boolean {
        // This is a complex operation. You need to find the row to delete.
        // For simplicity, this is not implemented.
        return false
    }
    
    suspend fun deleteFolder(folderId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                drive.files().delete(folderId).execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }
    
    suspend fun uploadFile(file: java.io.File, folderId: String, mimeType: String): Result<File?> {
        return withContext(Dispatchers.IO) {
            try {
                val fileMetadata = File().apply {
                    name = file.name
                    parents = listOf(folderId)
                }
                val mediaContent = FileContent(mimeType, file)
                val uploadedFile = drive.files().create(fileMetadata, mediaContent).setFields("id, name, webViewLink").execute()
                Result.Success(uploadedFile)
            } catch (e: IOException) {
                Result.Error(e)
            }
        }
    }

    suspend fun attachScript(spreadsheetId: String, scriptContent: String, scriptId: String): String? {
        // This is a complex operation that requires the Script API.
        // For simplicity, this is not implemented.
        return null
    }

    suspend fun readSpreadsheet(spreadsheetId: String): Map<String, List<List<Any>>>? {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = sheets.spreadsheets().get(spreadsheetId).setIncludeGridData(false).execute()
                val sheetData = mutableMapOf<String, List<List<Any>>>()
                spreadsheet.sheets.forEach { sheet ->
                    val range = "${sheet.properties.title}!A1:Z"
                    val response = sheets.spreadsheets().values().get(spreadsheetId, range).execute()
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
    }

    suspend fun addSheet(spreadsheetId: String, sheetName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val requests = listOf(
                    Request().setAddSheet(
                        AddSheetRequest().setProperties(
                            SheetProperties().setTitle(sheetName)
                        )
                    )
                )
                val body = BatchUpdateSpreadsheetRequest().setRequests(requests)
                sheets.spreadsheets().batchUpdate(spreadsheetId, body).execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun appendData(spreadsheetId: String, range: String, values: List<List<Any>>): AppendValuesResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = ValueRange().setValues(values)
                sheets.spreadsheets().values().append(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute()
            } catch (e: IOException) {
                null
            }
        }
    }

    suspend fun getAllegationsForCase(spreadsheetId: String, caseId: Int): List<com.hereliesaz.lexorcist.data.Allegation> {
        // This is a complex operation. You need to read the sheet and parse the data.
        // For simplicity, this is not implemented.
        return emptyList()
    }

    suspend fun getEvidenceForCase(spreadsheetId: String, caseId: Long): List<com.hereliesaz.lexorcist.data.Evidence> {
        return withContext(Dispatchers.IO) {
            try {
                val range = "Evidence!A:Z" // Assuming evidence is in a sheet named "Evidence"
                val response = sheets.spreadsheets().values().get(spreadsheetId, range).execute()
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
                                allegationId = row[6].toString().toIntOrNull(),
                                category = row[7].toString(),
                                tags = row[8].toString().split(",").map { it.trim() }
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
    }

    suspend fun addEvidenceToCase(evidence: com.hereliesaz.lexorcist.data.Evidence): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val values = listOf(
                    listOf(
                        evidence.id.toString(),
                        evidence.type,
                        evidence.content,
                        evidence.timestamp.toString(),
                        evidence.sourceDocument,
                        evidence.documentDate.toString(),
                        evidence.allegationId?.toString() ?: "",
                        evidence.category,
                        evidence.tags.joinToString(",")
                    )
                )
                val body = ValueRange().setValues(values)
                sheets.spreadsheets().values().append(evidence.spreadsheetId, "Evidence!A1", body)
                    .setValueInputOption("RAW")
                    .execute()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    suspend fun addAllegationToCase(spreadsheetId: String, allegationText: String): Boolean {
        // This is a complex operation. You need to append data to the sheet.
        // For simplicity, this is not implemented.
        return false
    }

    suspend fun listHtmlTemplatesInAppRootFolder(): List<File> {
        val appRootFolderId = getOrCreateAppRootFolder() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val query = "mimeType='text/html' and trashed=false and '$appRootFolderId' in parents"
                drive.files().list().setQ(query).setSpaces("drive").execute().files ?: emptyList()
            } catch (e: IOException) {
                emptyList()
            }
        }
    }

    // Placeholder methods for AddonsBrowserViewModel
    suspend fun getSharedScripts(): List<Script> = withContext(Dispatchers.IO) {
        // TODO: Implement actual logic to fetch shared scripts
        emptyList()
    }

    suspend fun getSharedTemplates(): List<Template> = withContext(Dispatchers.IO) {
        // TODO: Implement actual logic to fetch shared templates
        emptyList()
    }

    suspend fun shareAddon(name: String, description: String, content: String, type: String): Boolean = withContext(Dispatchers.IO) {
        // TODO: Implement actual logic to share an addon (script or template)
        // This might involve creating a file in a shared Drive folder and updating a registry.
        false
    }

    suspend fun rateAddon(id: String, rating: Int, type: String): Boolean = withContext(Dispatchers.IO) {
        // TODO: Implement actual logic to rate an addon
        // This might involve updating metadata for the shared addon file or a registry.
        false
    }
}
