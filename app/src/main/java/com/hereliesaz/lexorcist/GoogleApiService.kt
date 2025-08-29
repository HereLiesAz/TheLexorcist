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
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.model.Evidence // Import FinancialEntry data class
import com.hereliesaz.lexorcist.utils.FolderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader // Added for reading InputStream
import java.io.IOException
import java.io.InputStreamReader // Added for reading InputStream
import android.util.Log // Added for logging

import com.hereliesaz.lexorcist.data.Schema

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
            Log.e("GoogleApiService", "Error in getOrCreateFolder for $folderName", e)
            null
        }
    }

    suspend fun getOrCreateAppRootFolder(): String? {
        return getOrCreateFolder(Schema.APP_ROOT_FOLDER_NAME)
    }

    suspend fun getOrCreateCaseFolder(caseName: String): String? {
        val rootFolderId = getOrCreateAppRootFolder()
        return getOrCreateFolder(caseName, rootFolderId)
    }

    suspend fun getOrCreateEvidenceFolder(caseName: String): String? {
        val caseFolderId = getOrCreateCaseFolder(caseName)
        return getOrCreateFolder(Schema.Evidence.SHEET_NAME, caseFolderId)
    }

    suspend fun getOrCreateCaseRegistrySpreadsheetId(appRootFolderId: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "'${appRootFolderId}' in parents and mimeType='application/vnd.google-apps.spreadsheet' and name='${Schema.CASE_REGISTRY_SPREADSHEET_NAME}' and trashed=false"
            val fileListResult = driveService.files().list().setQ(query).setSpaces("drive").execute()
            val existingFiles = fileListResult?.files
            var registrySpreadsheetId: String?
            val newHeader = listOf(Schema.CaseRegistry.HEADER)

            if (existingFiles != null && existingFiles.isNotEmpty()) {
                registrySpreadsheetId = existingFiles[0].id
            } else {
                registrySpreadsheetId = createSpreadsheet(Schema.CASE_REGISTRY_SPREADSHEET_NAME, appRootFolderId)
                if (registrySpreadsheetId == null) return@withContext null
                addSheet(registrySpreadsheetId, Schema.CaseRegistry.SHEET_NAME)
                appendData(registrySpreadsheetId, Schema.CaseRegistry.SHEET_NAME, newHeader)
            }
            if (registrySpreadsheetId != null) {
                addSheet(registrySpreadsheetId, Schema.CaseRegistry.SHEET_NAME) // Ensure sheet exists
                val range = "${Schema.CaseRegistry.SHEET_NAME}!A1:E1" // Updated range for 5 columns
                val currentHeaderResponse = sheetsService.spreadsheets().values().get(registrySpreadsheetId, range).execute()
                val currentHeader = currentHeaderResponse.getValues()

                if (currentHeader == null || currentHeader.isEmpty() || currentHeader[0].size < 5) {
                    if (currentHeader == null || currentHeader.isEmpty()){
                         appendData(registrySpreadsheetId, Schema.CaseRegistry.SHEET_NAME, newHeader)
                    } else {
                        Log.w("GoogleApiService", "Case Registry header might be outdated or incomplete. Found: ${currentHeader[0]}. Expected: ${newHeader[0]}")
                        // Potentially add logic here to update the header if it's old but exists
                    }
                }
            }
            registrySpreadsheetId
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in getOrCreateCaseRegistrySpreadsheetId", e)
            null
        }
    }

    suspend fun getAllCasesFromRegistry(registrySpreadsheetId: String): List<Case> = withContext(Dispatchers.IO) {
        val cases = mutableListOf<Case>()
        try {
            val allSheetData = readSpreadsheet(registrySpreadsheetId)
            val caseSheetValues = allSheetData?.get(Schema.CaseRegistry.SHEET_NAME)
            
            caseSheetValues?.let {
                val header = it.firstOrNull() ?: return@withContext emptyList()
                val nameIndex = header.indexOf(Schema.CaseRegistry.COL_CASE_NAME)
                val spreadsheetIdIndex = header.indexOf(Schema.CaseRegistry.COL_SPREADSHEET_ID)
                val pdfIdIndex = header.indexOf(Schema.CaseRegistry.COL_GENERATED_PDF_ID)
                val snapshotIdIndex = header.indexOf(Schema.CaseRegistry.COL_SOURCE_HTML_SNAPSHOT_ID)
                val templateIdIndex = header.indexOf(Schema.CaseRegistry.COL_ORIGINAL_MASTER_HTML_TEMPLATE_ID)

                it.drop(1).forEach { row ->
                    val name = row.getOrNull(nameIndex)?.toString() ?: ""
                    val spreadsheetId = row.getOrNull(spreadsheetIdIndex)?.toString() ?: ""
                    if (name.isNotBlank() && spreadsheetId.isNotBlank()) {
                        val generatedPdfId = row.getOrNull(pdfIdIndex)?.toString()
                        val sourceHtmlSnapshotId = row.getOrNull(snapshotIdIndex)?.toString()
                        val originalMasterHtmlTemplateId = row.getOrNull(templateIdIndex)?.toString()
                        cases.add(Case(
                            name = name, 
                            spreadsheetId = spreadsheetId, 
                            generatedPdfId = if(generatedPdfId?.isBlank() == true) null else generatedPdfId,
                            sourceHtmlSnapshotId = if(sourceHtmlSnapshotId?.isBlank() == true) null else sourceHtmlSnapshotId,
                            originalMasterHtmlTemplateId = if(originalMasterHtmlTemplateId?.isBlank() == true) null else originalMasterHtmlTemplateId
                        ))
                    }
                }
            }
        } catch (e: Exception) { 
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in getAllCasesFromRegistry", e)
        }
        cases
    }

    suspend fun addCaseToRegistry(registrySpreadsheetId: String, caseData: Case): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = listOf(listOf(
                caseData.name,
                caseData.spreadsheetId,
                caseData.generatedPdfId ?: "",
                caseData.sourceHtmlSnapshotId ?: "",
                caseData.originalMasterHtmlTemplateId ?: ""
            ))
            appendData(registrySpreadsheetId, Schema.CaseRegistry.SHEET_NAME, values) != null
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in addCaseToRegistry for ${caseData.name}", e)
            false
        }
    }

    suspend fun deleteEvidenceFromCase(spreadsheetId: String, evidenceId: Int): Boolean = withContext(Dispatchers.IO) {
        if (spreadsheetId.isBlank()) return@withContext false
        try {
            val sheetId = getSheetId(spreadsheetId, Schema.Evidence.SHEET_NAME) ?: return@withContext false
            val request = Request().setDeleteDimension(
                DeleteDimensionRequest()
                    .setRange(
                        DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("ROWS")
                            .setStartIndex(evidenceId + 1) // +1 because rows are 1-indexed and we skip the header
                            .setEndIndex(evidenceId + 2)
                    )
            )
            val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
            sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in deleteEvidenceFromCase for $spreadsheetId", e)
            false
        }
    }

    // createMasterTemplate function removed

    suspend fun listHtmlTemplatesInAppRootFolder(): List<DriveFile> = withContext(Dispatchers.IO) {
        try {
            val appRootFolderId = getOrCreateAppRootFolder()
            if (appRootFolderId == null) {
                Log.e("GoogleApiService", "Failed to get app root folder ID. Cannot list HTML templates.")
                return@withContext emptyList()
            }

            val query = "'${appRootFolderId}' in parents and mimeType='text/html' and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, modifiedTime)") // Include fields you might need for display/sorting
                .execute()
            
            result.files ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error listing HTML templates in app root folder", e)
            emptyList()
        }
    }
    
    suspend fun getAllegationsForCase(caseSpreadsheetId: String, caseIdForAssociation: Int): List<Allegation> = withContext(Dispatchers.IO) {
        val allegations = mutableListOf<Allegation>()
        if (caseSpreadsheetId.isBlank()) return@withContext allegations
        try {
            val allSheetData = readSpreadsheet(caseSpreadsheetId)
            val allegationSheetValues = allSheetData?.get(Schema.Allegations.SHEET_NAME)
            allegationSheetValues?.let {
                val header = it.firstOrNull() ?: return@withContext emptyList()
                val textIndex = header.indexOf(Schema.Allegations.COL_ALLEGATION_TEXT)
                it.drop(1).forEachIndexed { index, row ->
                    val text = row.getOrNull(textIndex)?.toString() ?: ""
                    if (text.isNotBlank()) {
                        allegations.add(Allegation(id = index, caseId = caseIdForAssociation, text = text))
                    }
                }
            }
        } catch (e: Exception) { 
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in getAllegationsForCase for $caseSpreadsheetId", e)
        }
        allegations
    }

    suspend fun addAllegationToCase(caseSpreadsheetId: String, allegationText: String): Boolean = withContext(Dispatchers.IO) {
        if (caseSpreadsheetId.isBlank() || allegationText.isBlank()) return@withContext false
        try {
            addSheet(caseSpreadsheetId, Schema.Allegations.SHEET_NAME) // Ensure sheet exists
            val values = listOf(listOf(allegationText))
            appendData(caseSpreadsheetId, Schema.Allegations.SHEET_NAME, values) != null
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in addAllegationToCase for $caseSpreadsheetId", e)
            false
        }
    }

    suspend fun getEvidenceForCase(caseSpreadsheetId: String, caseIdForAssociation: Int): List<Evidence> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<Evidence>()
        if (caseSpreadsheetId.isBlank()) return@withContext entries
        try {
            val allSheetData = readSpreadsheet(caseSpreadsheetId)
            val sheetValues = allSheetData?.get(Schema.Evidence.SHEET_NAME)
            sheetValues?.let {
                val header = it.firstOrNull() ?: return@withContext emptyList()
                val contentIndex = header.indexOf(Schema.Evidence.COL_CONTENT)
                val timestampIndex = header.indexOf(Schema.Evidence.COL_TIMESTAMP)
                val sourceDocumentIndex = header.indexOf(Schema.Evidence.COL_SOURCE_DOCUMENT)
                val documentDateIndex = header.indexOf(Schema.Evidence.COL_DOCUMENT_DATE)
                val tagsIndex = header.indexOf(Schema.Evidence.COL_TAGS)
                val allegationIdIndex = header.indexOf(Schema.Evidence.COL_ALLEGATION_ID)
                val categoryIndex = header.indexOf(Schema.Evidence.COL_CATEGORY)

                it.drop(1).forEachIndexed { index, row -> // drop(1) to skip header
                    try {
                        val content = row.getOrNull(contentIndex)?.toString() ?: ""
                        val timestamp = row.getOrNull(timestampIndex)?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                        val sourceDocument = row.getOrNull(sourceDocumentIndex)?.toString() ?: ""
                        val documentDate = row.getOrNull(documentDateIndex)?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                        val tags = row.getOrNull(tagsIndex)?.toString()?.split(",")?.map { it.trim() } ?: emptyList()
                        val allegationId = row.getOrNull(allegationIdIndex)?.toString()?.toIntOrNull()
                        val category = row.getOrNull(categoryIndex)?.toString() ?: ""

                        entries.add(Evidence(
                            id = index, // Using row index as a simple ID for now
                            caseId = caseIdForAssociation,
                            allegationId = allegationId,
                            content = content,
                            timestamp = timestamp,
                            sourceDocument = sourceDocument,
                            documentDate = documentDate,
                            tags = tags,
                            category = category
                        ))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("GoogleApiService", "Error parsing row in getEvidenceForCase for $caseSpreadsheetId", e)
                    }
                }
            }
        } catch (e: Exception) { 
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in getEvidenceForCase for $caseSpreadsheetId", e)
        }
        entries
    }

    suspend fun addEvidenceToCase(caseSpreadsheetId: String, entry: Evidence): Boolean = withContext(Dispatchers.IO) {
        if (caseSpreadsheetId.isBlank()) return@withContext false
        try {
            // Ensure sheet exists and add header if it's new
            val sheetExists = sheetsService.spreadsheets().get(caseSpreadsheetId).execute().sheets?.any { it.properties?.title == Schema.Evidence.SHEET_NAME } == true
            if (!sheetExists) {
                addSheet(caseSpreadsheetId, Schema.Evidence.SHEET_NAME)
                val header = listOf(Schema.Evidence.HEADER)
                appendData(caseSpreadsheetId, Schema.Evidence.SHEET_NAME, header)
            }

            val values = listOf(listOf(
                entry.content,
                entry.timestamp.toString(),
                entry.sourceDocument,
                entry.documentDate.toString(),
                entry.tags.joinToString(","),
                entry.allegationId?.toString() ?: "", // Store Allegation ID as string, empty if null
                entry.category
            ))
            appendData(caseSpreadsheetId, Schema.Evidence.SHEET_NAME, values) != null
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in addEvidenceToCase for $caseSpreadsheetId", e)
            false
        }
    }

    suspend fun updateEvidenceInCase(spreadsheetId: String, evidence: Evidence): Boolean = withContext(Dispatchers.IO) {
        if (spreadsheetId.isBlank()) return@withContext false
        try {
            val range = "${Schema.Evidence.SHEET_NAME}!A${evidence.id + 2}" // +2 because sheet is 1-indexed and there's a header
            val values = listOf(listOf(
                evidence.content,
                evidence.timestamp.toString(),
                evidence.sourceDocument,
                evidence.documentDate.toString(),
                evidence.tags.joinToString(","),
                evidence.allegationId?.toString() ?: "",
                evidence.category
            ))
            val body = ValueRange().setValues(values)
            sheetsService.spreadsheets().values().update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in updateEvidenceInCase for $spreadsheetId", e)
            false
        }
    }

    
    suspend fun getSheetId(spreadsheetId: String, sheetName: String): Int? = withContext(Dispatchers.IO) {
        try {
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            val sheet = spreadsheet.sheets?.find { it.properties?.title == sheetName }
            sheet?.properties?.sheetId
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error getting sheet ID for $sheetName in $spreadsheetId", e)
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
            Log.e("GoogleApiService", "Error in readSpreadsheet for $spreadsheetId", e)
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
            Log.e("GoogleApiService", "Error in createSpreadsheet for $title", e)
            null
        }
    }
    
    suspend fun addSheet(spreadsheetId: String, sheetTitle: String): BatchUpdateSpreadsheetResponse? = withContext(Dispatchers.IO) {
        try {
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            if (spreadsheet.sheets?.any { it.properties?.title == sheetTitle } == true) return@withContext null // Sheet already exists
            val addSheetRequest = AddSheetRequest().setProperties(SheetProperties().setTitle(sheetTitle))
            val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))
            sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in addSheet for $sheetTitle in $spreadsheetId", e)
            null 
        }
    }

    suspend fun appendData(spreadsheetId: String, sheetTitle: String, values: List<List<Any>>): AppendValuesResponse? = withContext(Dispatchers.IO) {
        try {
            val body = ValueRange().setValues(values)
            sheetsService.spreadsheets().values().append(spreadsheetId, "$sheetTitle!A1", body) // Appending to A1 of the specified sheet
                .setValueInputOption("USER_ENTERED") 
                .setInsertDataOption("INSERT_ROWS")
                .execute()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in appendData to $sheetTitle in $spreadsheetId", e)
            null
        }
    }

    suspend fun attachScript(spreadsheetId: String, scriptContent: String, masterTemplateId: String) = withContext(Dispatchers.IO) {
        try {
            val createProjectRequest = CreateProjectRequest().setTitle("Case Tools Script").setParentId(spreadsheetId)
            val createdProject = scriptService.projects().create(createProjectRequest).execute()
            val scriptId = createdProject.scriptId ?: return@withContext

            val scriptFile = ScriptPlatformFile().setSource(scriptContent).setName("Code")
            val configContent = if (masterTemplateId.isNotBlank()) {
                "{\"masterTemplateId\": \"$masterTemplateId\"}" // This masterTemplateId is for the script's use, might refer to the generated PDF ID
            } else {
                "{}" 
            }
            val configFile = ScriptPlatformFile().setSource(configContent).setName("config.json")

            val scriptAPIContent = ScriptContent().setFiles(listOf(scriptFile, configFile)).setScriptId(scriptId)
            scriptService.projects().updateContent(scriptId, scriptAPIContent).execute()
        } catch (e: Exception) { 
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in attachScript for $spreadsheetId", e)
        }
    }
    
    suspend fun uploadFile(file: java.io.File, folderId: String, mimeType: String): DriveFile? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = DriveFile().apply { 
                name = file.name
                parents = listOf(folderId) 
            }
            val mediaContent = GoogleFileContent(mimeType, file)
            driveService.files().create(fileMetadata, mediaContent).setFields("id, name, webViewLink, webContentLink").execute()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in uploadFile ${file.name}", e)
            null
        }
    }

    suspend fun uploadStringAsFile(content: String, mimeType: String, fileName: String, folderId: String): DriveFile? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
                this.mimeType = mimeType 
            }
            val mediaContent = ByteArrayContent.fromString(mimeType, content)
            driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink, webContentLink") 
                .execute()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Failed to upload string as file '$fileName'", e)
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
            Log.e("GoogleApiService", "Error in copyFile $fileId to $newName", e)
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
            if (requests.isNotEmpty()) { 
                val batchUpdateRequest = BatchUpdateDocumentRequest().setRequests(requests)
                docsService.documents().batchUpdate(docId, batchUpdateRequest).execute()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in replacePlaceholdersInDoc for $docId", e)
        }
    }

    suspend fun getScript(scriptId: String): com.google.api.services.script.model.Content? = withContext(Dispatchers.IO) {
        try {
            scriptService.projects().getContent(scriptId).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in getScript $scriptId", e)
            null
        }
    }

    suspend fun updateScript(scriptId: String, scriptContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingContent = scriptService.projects().getContent(scriptId).execute()
            val existingFiles = existingContent?.files ?: mutableListOf()

            val updatedFiles = existingFiles.map { 
                if (it.name == "Code" && it.type == "SERVER_JS") { 
                    it.setSource(scriptContent) 
                } else {
                    it
                }
            }.toMutableList()

            if (updatedFiles.none { (it.name == "Code" || it.name == "Code.gs" || it.name == "Code.js") && it.type == "SERVER_JS"}) {
                 updatedFiles.add(com.google.api.services.script.model.File().setName("Code").setType("SERVER_JS").setSource(scriptContent))
            }
            
            val newContent = com.google.api.services.script.model.Content().setFiles(updatedFiles)
            scriptService.projects().updateContent(scriptId, newContent).execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in updateScript $scriptId", e)
            false
        }
    }

    suspend fun downloadFileAsString(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            driveService.files().get(fileId).executeMediaAsInputStream().use { inputStream ->
                if (inputStream == null) {
                    Log.e("GoogleApiService", "Failed to get input stream for file ID: $fileId")
                    return@withContext null
                }
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GoogleApiService", "Error in downloadFileAsString for $fileId", e)
            null
        }
    }
}
