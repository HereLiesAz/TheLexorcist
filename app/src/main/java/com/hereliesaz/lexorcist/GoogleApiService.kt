package com.hereliesaz.lexorcist

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent as GoogleFileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory // Changed from JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.docs.v1.Docs
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest
import com.google.api.services.docs.v1.model.SubstringMatchCriteria
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.script.Script
import com.google.api.services.script.model.Content as ScriptContent
import com.google.api.services.script.model.CreateProjectRequest
import com.google.api.services.script.model.File as ScriptPlatformFile
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.hereliesaz.lexorcist.db.Allegation
import com.hereliesaz.lexorcist.db.Case
import com.hereliesaz.lexorcist.model.Evidence // Import FinancialEntry data class
import com.hereliesaz.lexorcist.utils.FolderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val APP_ROOT_FOLDER_NAME = "The Lexorcist"
private const val CASE_REGISTRY_SPREADSHEET_NAME = "Lexorcist_Case_Registry"
private const val CASE_REGISTRY_SHEET_NAME = "Cases"
private const val ALLEGATIONS_SHEET_NAME = "Allegations"
private const val EVIDENCE_SHEET_NAME = "Evidence"

class GoogleApiService(
    private val credential: GoogleAccountCredential,
    applicationName: String
) {

    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance() // Changed from JacksonFactory

    val sheetsService: Sheets = Sheets.Builder(transport, jsonFactory, credential)
        .setApplicationName(applicationName)
        .build()

    val driveService: Drive = Drive.Builder(transport, jsonFactory, credential)
        .setApplicationName(applicationName)
        .build()

    val scriptService: Script = Script.Builder(transport, jsonFactory, credential)
        .setApplicationName(applicationName)
        .build()

    val docsService: Docs = Docs.Builder(transport, jsonFactory, credential)
        .setApplicationName(applicationName)
        .build()

    private val folderManager = FolderManager(driveService)

    suspend fun getOrCreateFolder(folderName: String, parentId: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            folderManager.getOrCreateFolder(folderName, parentId)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getOrCreateAppRootFolder(): String? {
        return getOrCreateFolder(APP_ROOT_FOLDER_NAME)
    }

    suspend fun getOrCreateCaseFolder(caseName: String): String? {
        val rootFolderId = getOrCreateAppRootFolder()
        return getOrCreateFolder(caseName, rootFolderId)
    }

    suspend fun getOrCreateEvidenceFolder(caseName: String): String? {
        val caseFolderId = getOrCreateCaseFolder(caseName)
        return getOrCreateFolder("Evidence", caseFolderId)
    }

    suspend fun getOrCreateCaseRegistrySpreadsheetId(appRootFolderId: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "'${appRootFolderId}' in parents and mimeType='application/vnd.google-apps.spreadsheet' and name='$CASE_REGISTRY_SPREADSHEET_NAME' and trashed=false"
            val fileListResult = driveService.files().list().setQ(query).setSpaces("drive").execute()
            val existingFiles = fileListResult?.files
            var registrySpreadsheetId: String?
            if (existingFiles != null && existingFiles.isNotEmpty()) {
                registrySpreadsheetId = existingFiles[0].id
            } else {
                registrySpreadsheetId = createSpreadsheet(CASE_REGISTRY_SPREADSHEET_NAME, appRootFolderId)
                if (registrySpreadsheetId == null) return@withContext null
                addSheet(registrySpreadsheetId, CASE_REGISTRY_SHEET_NAME)
                val header = listOf(listOf("Case Name", "Spreadsheet ID", "Master Template ID"))
                appendData(registrySpreadsheetId, CASE_REGISTRY_SHEET_NAME, header)
            }
            if (registrySpreadsheetId != null) {
                addSheet(registrySpreadsheetId, CASE_REGISTRY_SHEET_NAME) // Ensure sheet exists
                val range = "$CASE_REGISTRY_SHEET_NAME!A1:C1"
                val currentHeader = sheetsService.spreadsheets().values().get(registrySpreadsheetId, range).execute()
                if (currentHeader.getValues() == null || currentHeader.getValues().isEmpty()) {
                    val header = listOf(listOf("Case Name", "Spreadsheet ID", "Master Template ID"))
                    appendData(registrySpreadsheetId, CASE_REGISTRY_SHEET_NAME, header)
                }
            }
            registrySpreadsheetId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAllCasesFromRegistry(registrySpreadsheetId: String): List<Case> = withContext(Dispatchers.IO) {
        val cases = mutableListOf<Case>()
        try {
            val allSheetData = readSpreadsheet(registrySpreadsheetId)
            val caseSheetValues = allSheetData?.get(CASE_REGISTRY_SHEET_NAME)
            caseSheetValues?.drop(1)?.forEach { row ->
                if (row.size >= 3) {
                    val name = row.getOrNull(0)?.toString() ?: ""
                    val spreadsheetId = row.getOrNull(1)?.toString() ?: ""
                    val masterTemplateId = row.getOrNull(2)?.toString() ?: ""
                    if (name.isNotBlank() && spreadsheetId.isNotBlank()) {
                        cases.add(Case(name = name, spreadsheetId = spreadsheetId, masterTemplateId = masterTemplateId))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        cases
    }

    suspend fun addCaseToRegistry(registrySpreadsheetId: String, caseData: Case): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = listOf(listOf(caseData.name, caseData.spreadsheetId, caseData.masterTemplateId))
            appendData(registrySpreadsheetId, CASE_REGISTRY_SHEET_NAME, values) != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllegationsForCase(caseSpreadsheetId: String, caseIdForAssociation: Int): List<Allegation> = withContext(Dispatchers.IO) {
        val allegations = mutableListOf<Allegation>()
        if (caseSpreadsheetId.isBlank()) return@withContext allegations
        try {
            val allSheetData = readSpreadsheet(caseSpreadsheetId)
            val allegationSheetValues = allSheetData?.get(ALLEGATIONS_SHEET_NAME)
            allegationSheetValues?.forEachIndexed { index, row ->
                val text = row.getOrNull(0)?.toString() ?: ""
                if (text.isNotBlank()) {
                    allegations.add(Allegation(id = index, caseId = caseIdForAssociation, text = text))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        allegations
    }

    suspend fun addAllegationToCase(caseSpreadsheetId: String, allegationText: String): Boolean = withContext(Dispatchers.IO) {
        if (caseSpreadsheetId.isBlank() || allegationText.isBlank()) return@withContext false
        try {
            addSheet(caseSpreadsheetId, ALLEGATIONS_SHEET_NAME) // Ensure sheet exists
            val values = listOf(listOf(allegationText))
            appendData(caseSpreadsheetId, ALLEGATIONS_SHEET_NAME, values) != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getEvidenceForCase(caseSpreadsheetId: String, caseIdForAssociation: Int): List<Evidence> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<Evidence>()
        if (caseSpreadsheetId.isBlank()) return@withContext entries
        try {
            val allSheetData = readSpreadsheet(caseSpreadsheetId)
            val sheetValues = allSheetData?.get(EVIDENCE_SHEET_NAME)
            sheetValues?.drop(1)?.forEachIndexed { index, row -> // drop(1) to skip header
                try {
                    val content = row.getOrNull(0)?.toString() ?: ""
                    val timestamp = row.getOrNull(1)?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                    val sourceDocument = row.getOrNull(2)?.toString() ?: ""
                    val documentDate = row.getOrNull(3)?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                    val tags = row.getOrNull(4)?.toString()?.split(",")?.map { it.trim() } ?: emptyList()
                    val allegationId = row.getOrNull(5)?.toString()?.toIntOrNull()

                    entries.add(Evidence(
                        id = index, // Using row index as a simple ID for now
                        caseId = caseIdForAssociation,
                        allegationId = allegationId,
                        content = content,
                        timestamp = timestamp,
                        sourceDocument = sourceDocument,
                        documentDate = documentDate,
                        tags = tags
                    ))
                } catch (e: Exception) {
                    e.printStackTrace() // Log error during row parsing
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        entries
    }

    suspend fun addEvidenceToCase(caseSpreadsheetId: String, entry: Evidence): Boolean = withContext(Dispatchers.IO) {
        if (caseSpreadsheetId.isBlank()) return@withContext false
        try {
            // Ensure sheet exists and add header if it's new
            val sheetExists = sheetsService.spreadsheets().get(caseSpreadsheetId).execute().sheets?.any { it.properties?.title == EVIDENCE_SHEET_NAME } == true
            if (!sheetExists) {
                addSheet(caseSpreadsheetId, EVIDENCE_SHEET_NAME)
                val header = listOf(listOf("Content", "Timestamp", "Source Document", "Document Date", "Tags", "Allegation ID"))
                appendData(caseSpreadsheetId, EVIDENCE_SHEET_NAME, header)
            }

            val values = listOf(listOf(
                entry.content,
                entry.timestamp.toString(),
                entry.sourceDocument,
                entry.documentDate.toString(),
                entry.tags.joinToString(","),
                entry.allegationId?.toString() ?: "" // Store Allegation ID as string, empty if null
            ))
            appendData(caseSpreadsheetId, EVIDENCE_SHEET_NAME, values) != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createMasterTemplate(parentId: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = DriveFile().setName("Lexorcist Master Template").setMimeType("application/vnd.google-apps.document")
            if (parentId != null) fileMetadata.parents = listOf(parentId)
            val content = ByteArrayContent.fromString("text/plain", "Placeholder for master template content.")
            driveService.files().create(fileMetadata, content).setFields("id").execute()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun readSpreadsheet(spreadsheetId: String): Map<String, List<List<Any>>>? = withContext(Dispatchers.IO) {
        try {
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).setIncludeGridData(false).execute()
            val sheetData = mutableMapOf<String, List<List<Any>>>()
            spreadsheet.sheets?.forEach { sheet ->
                val range = sheet.properties?.title
                if (range != null) {
                    val response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute()
                    sheetData[range] = response.getValues() ?: emptyList()
                }
            }
            sheetData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createSpreadsheet(title: String, parentId: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = DriveFile().setName(title).setMimeType("application/vnd.google-apps.spreadsheet")
            if (parentId != null) fileMetadata.parents = listOf(parentId)
            driveService.files().create(fileMetadata).setFields("id").execute()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun addSheet(spreadsheetId: String, sheetTitle: String): BatchUpdateSpreadsheetResponse? = withContext(Dispatchers.IO) {
        try {
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            if (spreadsheet.sheets?.any { it.properties?.title == sheetTitle } == true) return@withContext null
            val addSheetRequest = AddSheetRequest().setProperties(SheetProperties().setTitle(sheetTitle))
            val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))
            sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            null 
        }
    }

    suspend fun appendData(spreadsheetId: String, sheetTitle: String, values: List<List<Any>>): AppendValuesResponse? = withContext(Dispatchers.IO) {
        try {
            val body = ValueRange().setValues(values)
            sheetsService.spreadsheets().values().append(spreadsheetId, "$sheetTitle!A1", body)
                .setValueInputOption("USER_ENTERED") 
                .setInsertDataOption("INSERT_ROWS")
                .execute()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun attachScript(spreadsheetId: String, scriptContent: String, masterTemplateId: String) = withContext(Dispatchers.IO) {
        try {
            val createProjectRequest = CreateProjectRequest().setTitle("Case Tools Script").setParentId(spreadsheetId)
            val createdProject = scriptService.projects().create(createProjectRequest).execute()
            val scriptId = createdProject.scriptId ?: return@withContext

            val scriptFile = ScriptPlatformFile().setSource(scriptContent).setName("Code")
            val configFile = ScriptPlatformFile().setSource("{\"masterTemplateId\": \"$masterTemplateId\"}").setName("config.json")

            val scriptAPIContent = ScriptContent().setFiles(listOf(scriptFile, configFile)).setScriptId(scriptId)
            scriptService.projects().updateContent(scriptId, scriptAPIContent).execute()
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    suspend fun uploadFile(file: java.io.File, folderId: String, mimeType: String): DriveFile? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = DriveFile().apply { name = file.name; parents = listOf(folderId) }
            val mediaContent = GoogleFileContent(mimeType, file)
            driveService.files().create(fileMetadata, mediaContent).setFields("id, name, webViewLink, webContentLink").execute()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun copyFile(fileId: String, newName: String, parentFolderId: String?): DriveFile? = withContext(Dispatchers.IO) {
        try {
            val newFile = DriveFile().setName(newName)
            if (parentFolderId != null) {
                newFile.parents = listOf(parentFolderId)
            }
            driveService.files().copy(fileId, newFile).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun replacePlaceholdersInDoc(docId: String, replacements: Map<String, String>) = withContext(Dispatchers.IO) {
        try {
            val requests = replacements.map { (placeholder, value) ->
                com.google.api.services.docs.v1.model.Request().setReplaceAllText(
                    ReplaceAllTextRequest()
                        .setContainsText(SubstringMatchCriteria().setText("{{$placeholder}}"))
                        .setReplaceText(value)
                )
            }
            val batchUpdateRequest = BatchUpdateDocumentRequest().setRequests(requests)
            docsService.documents().batchUpdate(docId, batchUpdateRequest).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
