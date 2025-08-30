package com.hereliesaz.lexorcist

import android.util.Log
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence // Corrected import
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
// import java.util.Date // Removed import

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
            return null // Indicate that import was skipped due to duplicate
        }

        // 3. Create New Case Structure (since no duplicate was found)
        Log.d(TAG, "parseAndStore: No existing case found with name '$importedCaseName'. Proceeding to create new case structure.")
        
        val newCaseFolderId = googleApiService.getOrCreateFolder(importedCaseName, appRootFolderId)
        if (newCaseFolderId == null) {
            Log.e(TAG, "parseAndStore: Failed to create folder for new case: $importedCaseName")
            return null
        }
        val newCaseSpreadsheetId = googleApiService.createSpreadsheet(importedCaseName, newCaseFolderId)
        if (newCaseSpreadsheetId == null) {
            Log.e(TAG, "parseAndStore: Failed to create spreadsheet for new case: $importedCaseName")
            return null
        }
        
        val placeholderScriptContent = "// Default script for imported case\nfunction onOpen() {}\n"
        val masterIdForScriptConfig = ""
        googleApiService.attachScript(newCaseSpreadsheetId, placeholderScriptContent, masterIdForScriptConfig)
        
        Log.d(TAG, "parseAndStore: Created new case structure: Folder ID $newCaseFolderId, Spreadsheet ID $newCaseSpreadsheetId")

        // 4. Add to Case Registry
        // newCase.id will be 0 by default from the Case data class if not otherwise set
        val newCase = Case(name = importedCaseName, spreadsheetId = newCaseSpreadsheetId)
        val addedToRegistry = googleApiService.addCaseToRegistry(caseRegistrySpreadsheetId, newCase)
        if (!addedToRegistry) {
            Log.w(TAG, "parseAndStore: Failed to add new case '$importedCaseName' to registry. Proceeding with data import to its sheet, but it won't be listed until registry issue is fixed.")
        }

        // 5. Parse and Store Allegations into the new case's spreadsheet
        val allegationsSheetName = schema.allegationsSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, allegationsSheetName) // Ensure sheet exists
        val allegationsSheetData = sheetsData[allegationsSheetName]
        allegationsSheetData?.drop(1)?.forEach { row -> // Assuming first row is header
            val allegationText = row.getOrNull(schema.allegationsSheet.allegationColumn)?.toString()
            if (!allegationText.isNullOrBlank()) {
                val success = googleApiService.addAllegationToCase(newCaseSpreadsheetId, allegationText)
                if (!success) {
                    Log.w(TAG, "parseAndStore: Failed to add allegation: '$allegationText' to $newCaseSpreadsheetId")
                }
            }
        }

        // 6. Parse and Store Evidence into the new case's spreadsheet
        val evidenceSheetName = schema.evidenceSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, evidenceSheetName) // Ensure sheet exists
        val evidenceHeader = listOf(listOf("Content", "Timestamp", "Source Document", "Document Date", "Tags", "Allegation ID", "Category")) // Amount removed from header
        googleApiService.appendData(newCaseSpreadsheetId, evidenceSheetName, evidenceHeader) // Add header row

        val evidenceSheetData = sheetsData[evidenceSheetName]
        var evidenceCounter = 0 // For generating a unique ID for evidence within this import context
        evidenceSheetData?.drop(1)?.forEach { row -> // Assuming first row is header
            try {
                val content = row.getOrNull(schema.evidenceSheet.contentColumn)?.toString()
                val tagsStr = row.getOrNull(schema.evidenceSheet.tagsColumn)?.toString()
                // For other fields like timestamp, documentDate, amount, category, allegationId, sourceDocument:
                // we'll use defaults or parse them if schema includes them. For now, using defaults.

                if (content != null) {
                    val evidence = Evidence(
                        id = evidenceCounter++, 
                        caseId = newCase.id, // newCase.id is Int from Case data class definition
                        content = content,
                        // amount = null, // Removed amount
                        timestamp = System.currentTimeMillis(), // Changed to Long
                        sourceDocument = "Imported from spreadsheet", // Default source
                        documentDate = System.currentTimeMillis(), // Changed to Long
                        allegationId = null, // Defaulting to null (Int?)
                        category = "Imported", // Default category
                        tags = tagsStr?.split(",")?.map { it.trim() } ?: emptyList()
                    )
                    val newId = googleApiService.addEvidenceToCase(newCaseSpreadsheetId, evidence)
                    if (newId == null) {
                        Log.w(TAG, "parseAndStore: Failed to add evidence: $evidence to $newCaseSpreadsheetId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "parseAndStore: Error parsing evidence row: $row", e)
            }
        }
        Log.i(TAG, "parseAndStore: Finished importing data for case: $importedCaseName, Spreadsheet ID: $newCaseSpreadsheetId")
        return newCase // Return the newly created Case object
    }
}
