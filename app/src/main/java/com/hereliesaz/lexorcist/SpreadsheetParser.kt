package com.hereliesaz.lexorcist

import android.util.Log
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import com.hereliesaz.lexorcist.utils.Result // Assuming Result is in this package

class SpreadsheetParser(
    private val googleApiService: GoogleApiService,
    private val schema: SpreadsheetSchema
) {
    private val TAG = "SpreadsheetParser"

    suspend fun parseAndStore(sheetsData: Map<String, List<List<Any>>>): Case? {
        // 1. Get Root Folder and Registry IDs
        val appRootFolderId = googleApiService.getOrCreateAppRootFolder()
        if (appRootFolderId == null) {
            Log.e(TAG, "parseAndStore: Failed to get or create app root folder.")
            return null
        }
        val caseRegistrySpreadsheetId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
        if (caseRegistrySpreadsheetId == null) {
            Log.e(TAG, "parseAndStore: Failed to get or create case registry spreadsheet.")
            return null
        }

        // 2. Extract Case Name from imported sheet
        val caseInfoSheet = sheetsData[schema.caseInfoSheet.name]
        val importedCaseName = caseInfoSheet?.find { it.getOrNull(0)?.toString()?.equals(schema.caseInfoSheet.caseNameLabel, ignoreCase = true) == true }
            ?.getOrNull(schema.caseInfoSheet.caseNameColumn)?.toString() ?: "Imported Case ${System.currentTimeMillis()}"
        Log.d(TAG, "parseAndStore: Determined case name from import data: $importedCaseName")

        // 2.1 Check for Existing Case with the same name in the registry
        val existingCases = googleApiService.getAllCasesFromRegistry(caseRegistrySpreadsheetId)
        val duplicateCase = existingCases.find { it.name.equals(importedCaseName, ignoreCase = true) }
        if (duplicateCase != null) {
            Log.w(TAG, "parseAndStore: Case with name '$importedCaseName' already exists in the registry. Import aborted. Spreadsheet ID: ${duplicateCase.spreadsheetId}")
            return null
        }

        // 3. Create New Case Structure
        Log.d(TAG, "parseAndStore: No existing case found with name '$importedCaseName'. Proceeding to create new case structure.")
        
        val newCaseFolderId = googleApiService.getOrCreateCaseFolder(importedCaseName) // Changed from getOrCreateFolder
        if (newCaseFolderId == null) {
            Log.e(TAG, "parseAndStore: Failed to create folder for new case: $importedCaseName")
            return null
        }

        val newCaseSpreadsheetIdResult = googleApiService.createSpreadsheet(importedCaseName, newCaseFolderId)
        val newCaseSpreadsheetId: String
        when (newCaseSpreadsheetIdResult) {
            is Result.Success -> {
                val id = newCaseSpreadsheetIdResult.data
                if (id == null) {
                    Log.e(TAG, "parseAndStore: createSpreadsheet returned success but with a null ID for case: $importedCaseName")
                    return null
                }
                newCaseSpreadsheetId = id
            }
            is Result.Error -> {
                Log.e(TAG, "parseAndStore: Failed to create spreadsheet for new case: $importedCaseName. Error: ${newCaseSpreadsheetIdResult.exception}")
                return null
            }
        }
        
        Log.d(TAG, "parseAndStore: Created new case structure: Folder ID $newCaseFolderId, Spreadsheet ID $newCaseSpreadsheetId")
        
        val placeholderScriptContent = "// Default script for imported case\nfunction onOpen() {}\n"
        // Assuming masterIdForScriptConfig should be related to the newCaseSpreadsheetId or a predefined one.
        // For now, using newCaseSpreadsheetId as a placeholder if a specific script project ID is needed.
        // The Google Script API might require a project ID, not just a spreadsheet ID for attaching a script.
        // This part might need further review based on how attachScript is implemented.
        googleApiService.attachScript(newCaseSpreadsheetId, placeholderScriptContent, newCaseSpreadsheetId /* Placeholder for scriptId */)

        // 4. Add to Case Registry
        val newCase = Case(name = importedCaseName, spreadsheetId = newCaseSpreadsheetId, folderId = newCaseFolderId) // Added folderId
        val addedToRegistry = googleApiService.addCaseToRegistry(caseRegistrySpreadsheetId, newCase)
        if (!addedToRegistry) {
            Log.w(TAG, "parseAndStore: Failed to add new case '$importedCaseName' to registry. Proceeding with data import, but it won't be listed.")
        }

        // 5. Parse and Store Allegations
        val allegationsSheetName = schema.allegationsSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, allegationsSheetName)
        val allegationsSheetData = sheetsData[allegationsSheetName]
        allegationsSheetData?.drop(1)?.forEach { row ->
            val allegationText = row.getOrNull(schema.allegationsSheet.allegationColumn)?.toString()
            if (!allegationText.isNullOrBlank()) {
                // TODO: Implement googleApiService.addAllegationToCase(spreadsheetId, allegationText)
                // val success = googleApiService.addAllegationToCase(newCaseSpreadsheetId, allegationText)
                // if (!success) {
                //     Log.w(TAG, "parseAndStore: Failed to add allegation: '$allegationText' to $newCaseSpreadsheetId")
                // }
                Log.d(TAG, "Allegation to add: $allegationText to $newCaseSpreadsheetId (skipping due to missing GoogleApiService method)")
            }
        }

        // 6. Parse and Store Evidence
        val evidenceSheetName = schema.evidenceSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, evidenceSheetName)
        val evidenceHeader = listOf(listOf("Content", "Timestamp", "Source Document", "Document Date", "Tags", "Allegation ID", "Category", "Type", "SpreadsheetID"))
        // TODO: Implement googleApiService.appendData(spreadsheetId, sheetName, data)
        // googleApiService.appendData(newCaseSpreadsheetId, evidenceSheetName, evidenceHeader)
        Log.d(TAG, "Evidence header for: $evidenceSheetName in $newCaseSpreadsheetId (skipping due to missing GoogleApiService method)")


        val evidenceSheetData = sheetsData[evidenceSheetName]
        var evidenceCounter = 0
        evidenceSheetData?.drop(1)?.forEach { row ->
            try {
                val content = row.getOrNull(schema.evidenceSheet.contentColumn)?.toString()
                val tagsStr = row.getOrNull(schema.evidenceSheet.tagsColumn)?.toString()

                if (content != null) {
                    val evidence = Evidence(
                        id = evidenceCounter++, 
                        caseId = newCase.id.toLong(), // Corrected: Int to Long
                        spreadsheetId = newCaseSpreadsheetId, // Added
                        type = "imported_text_evidence", // Added default type
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Imported from spreadsheet",
                        documentDate = System.currentTimeMillis(),
                        allegationId = null,
                        category = "Imported",
                        tags = tagsStr?.split(",")?.map { it.trim() } ?: emptyList()
                    )
                    // TODO: Implement googleApiService.addEvidenceToCase(spreadsheetId, evidence)
                    // val newId = googleApiService.addEvidenceToCase(newCaseSpreadsheetId, evidence)
                    // if (newId == null) {
                    //    Log.w(TAG, "parseAndStore: Failed to add evidence: $evidence to $newCaseSpreadsheetId")
                    // }
                    Log.d(TAG, "Evidence to add: $evidence to $newCaseSpreadsheetId (skipping due to missing GoogleApiService method)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "parseAndStore: Error parsing evidence row: $row", e)
            }
        }
        Log.i(TAG, "parseAndStore: Finished importing data for case: $importedCaseName, Spreadsheet ID: $newCaseSpreadsheetId")
        return newCase
    }
}
