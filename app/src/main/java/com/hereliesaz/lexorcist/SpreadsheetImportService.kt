package com.hereliesaz.lexorcist

import android.util.Log
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import com.hereliesaz.lexorcist.utils.Result // Assuming Result is in this package

class SpreadsheetImportService(
    private val googleApiService: GoogleApiService,
    private val schema: SpreadsheetSchema
) {
    private val TAG = "SpreadsheetImportSvc"

    // Renamed from parseAndStore to importAndSetupNewCaseFromData
    suspend fun importAndSetupNewCaseFromData(sheetsData: Map<String, List<List<Any>>>): Case? {
        // 1. Get Root Folder and Registry IDs
        val appRootFolderId = googleApiService.getOrCreateAppRootFolder()
        if (appRootFolderId == null) {
            Log.e(TAG, "importAndSetup: Failed to get or create app root folder.")
            return null
        }
        val caseRegistrySpreadsheetId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
        if (caseRegistrySpreadsheetId == null) {
            Log.e(TAG, "importAndSetup: Failed to get or create case registry spreadsheet.")
            return null
        }

        // 2. Extract Case Name from imported sheet
        val caseInfoSheet = sheetsData[schema.caseInfoSheet.name]
        val importedCaseName = caseInfoSheet?.find { it.getOrNull(0)?.toString()?.equals(schema.caseInfoSheet.caseNameLabel, ignoreCase = true) == true }
            ?.getOrNull(schema.caseInfoSheet.caseNameColumn)?.toString() ?: "Imported Case ${System.currentTimeMillis()}"
        Log.d(TAG, "importAndSetup: Determined case name from import data: $importedCaseName")

        // 2.1 Check for Existing Case with the same name in the registry
        val existingCases = googleApiService.getAllCasesFromRegistry(caseRegistrySpreadsheetId)
        val duplicateCase = existingCases.find { it.name.equals(importedCaseName, ignoreCase = true) }
        if (duplicateCase != null) {
            Log.w(TAG, "importAndSetup: Case with name '$importedCaseName' already exists. Import aborted. Spreadsheet ID: ${duplicateCase.spreadsheetId}")
            return null // Do not proceed if case with same name exists
        }

        // 3. Create New Case Structure
        Log.d(TAG, "importAndSetup: No existing case found with name '$importedCaseName'. Proceeding to create new case structure.")
        
        val newCaseFolderId = googleApiService.getOrCreateCaseFolder(importedCaseName)
        if (newCaseFolderId == null) {
            Log.e(TAG, "importAndSetup: Failed to create folder for new case: $importedCaseName")
            return null
        }

        val newCaseSpreadsheetIdResult = googleApiService.createSpreadsheet(importedCaseName, newCaseFolderId)
        val newCaseSpreadsheetId: String
        when (newCaseSpreadsheetIdResult) {
            is Result.Success -> {
                val id = newCaseSpreadsheetIdResult.data
                if (id == null) {
                    Log.e(TAG, "importAndSetup: createSpreadsheet returned success but with a null ID for case: $importedCaseName")
                    return null
                }
                newCaseSpreadsheetId = id
            }
            is Result.Error -> {
                Log.e(TAG, "importAndSetup: Failed to create spreadsheet for new case: $importedCaseName. Error: ${newCaseSpreadsheetIdResult.exception}")
                return null
            }
        }
        
        Log.d(TAG, "importAndSetup: Created new case structure: Folder ID $newCaseFolderId, Spreadsheet ID $newCaseSpreadsheetId")
        
        val placeholderScriptContent = "// Default script for imported case\nfunction onOpen() {}\n"
        googleApiService.attachScript(newCaseSpreadsheetId, placeholderScriptContent, "ImportedCaseScript") // Provide a default script name/ID

        // 4. Add to Case Registry
        // Create a basic Case object for registry. ID will be assigned by registry logic or is row-based.
        val newCaseForRegistry = Case(id = 0, name = importedCaseName, spreadsheetId = newCaseSpreadsheetId, folderId = newCaseFolderId)
        val addedToRegistry = googleApiService.addCaseToRegistry(caseRegistrySpreadsheetId, newCaseForRegistry)
        if (!addedToRegistry) {
            Log.w(TAG, "importAndSetup: Failed to add new case '$importedCaseName' to registry. Data import will proceed, but case won't be auto-listed.")
        }

        // 5. Parse and Store Allegations from the imported data into the new spreadsheet
        val allegationsSheetName = schema.allegationsSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, allegationsSheetName) // Ensure sheet exists
        val allegationsDataToAppend = sheetsData[allegationsSheetName]?.drop(1)?.mapNotNull { row ->
            val allegationText = row.getOrNull(schema.allegationsSheet.allegationColumn)?.toString()
            if (!allegationText.isNullOrBlank()) listOf(allegationText) else null
        } ?: emptyList()

        if (allegationsDataToAppend.isNotEmpty()) {
             Log.d(TAG, "Attempting to append ${allegationsDataToAppend.size} allegations to $allegationsSheetName in $newCaseSpreadsheetId")
            // TODO: Ensure GoogleApiService.appendData is suitable or if a batch update is needed
            // googleApiService.appendData(newCaseSpreadsheetId, allegationsSheetName + "!A1", allegationsDataToAppend)
        }

        // 6. Parse and Store Evidence from the imported data into the new spreadsheet
        val evidenceSheetName = schema.evidenceSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, evidenceSheetName) // Ensure sheet exists
        val evidenceHeader = listOf("Content", "Timestamp", "Source Document", "Document Date", "Tags", "Allegation ID", "Category", "Type", "SpreadsheetID")
        // TODO: appendData for header
        // googleApiService.appendData(newCaseSpreadsheetId, evidenceSheetName + "!A1", listOf(evidenceHeader))

        val evidenceDataToAppend = sheetsData[evidenceSheetName]?.drop(1)?.mapNotNull { row ->
            val content = row.getOrNull(schema.evidenceSheet.contentColumn)?.toString()
            if (content.isNullOrBlank()) null
            else {
                val tagsStr = row.getOrNull(schema.evidenceSheet.tagsColumn)?.toString()
                // Construct the row for the new sheet
                listOf(
                    content,
                    System.currentTimeMillis().toString(), // Timestamp
                    "Imported from spreadsheet", // Source Document
                    System.currentTimeMillis().toString(), // Document Date (or parse if available)
                    tagsStr ?: "", // Tags
                    "", // Allegation ID (needs linking logic)
                    "Imported", // Category
                    "imported_text_evidence", // Type
                    newCaseSpreadsheetId // SpreadsheetID (redundant but for schema consistency)
                )
            }
        } ?: emptyList()

        if (evidenceDataToAppend.isNotEmpty()) {
            Log.d(TAG, "Attempting to append ${evidenceDataToAppend.size} evidence items to $evidenceSheetName in $newCaseSpreadsheetId")
            // TODO: appendData for evidence rows
            // googleApiService.appendData(newCaseSpreadsheetId, evidenceSheetName + "!A2", evidenceDataToAppend) // A2 if header is in A1
        }

        Log.i(TAG, "importAndSetup: Finished importing and setting up data for case: $importedCaseName, New Spreadsheet ID: $newCaseSpreadsheetId")
        // The returned Case object should reflect the newly created and registered case
        // It might be beneficial to re-fetch the case from registry to get any server-assigned ID or confirm details
        return Case(name = importedCaseName, spreadsheetId = newCaseSpreadsheetId, folderId = newCaseFolderId, id = newCaseForRegistry.id) 
    }
}
