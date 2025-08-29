package com.hereliesaz.lexorcist

import android.util.Log
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.model.Evidence
// DAOs are no longer used

class SpreadsheetParser(
    private val googleApiService: GoogleApiService
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
        val caseInfoSheet = sheetsData["Case Info"]
        val importedCaseName = caseInfoSheet?.find { it.getOrNull(0)?.toString()?.equals("Case Name", ignoreCase = true) == true }
            ?.getOrNull(1)?.toString() ?: "Imported Case ${System.currentTimeMillis()}"
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
        
        // val masterTemplateId = googleApiService.createMasterTemplate(appRootFolderId) // Removed call
        val originalMasterHtmlTemplateIdForCaseRecord: String? = null // This will be null for imported cases now

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
        
        // Corrected attachScript call for imported cases
        // For now, using placeholder script content. Ideally, this would load a suitable script template.
        val placeholderScriptContent = "// Default script for imported case\nfunction onOpen() {}\n"
        val masterIdForScriptConfig = "" // No specific PDF/Doc template for imported cases initially
        googleApiService.attachScript(newCaseSpreadsheetId, placeholderScriptContent, masterIdForScriptConfig)
        
        Log.d(TAG, "parseAndStore: Created new case structure: Folder ID $newCaseFolderId, Spreadsheet ID $newCaseSpreadsheetId")

        // 4. Add to Case Registry
        // originalMasterHtmlTemplateId will be null for this newly imported case
        val newCase = Case(name = importedCaseName, spreadsheetId = newCaseSpreadsheetId, originalMasterHtmlTemplateId = originalMasterHtmlTemplateIdForCaseRecord)
        val addedToRegistry = googleApiService.addCaseToRegistry(caseRegistrySpreadsheetId, newCase)
        if (!addedToRegistry) {
            Log.w(TAG, "parseAndStore: Failed to add new case '$importedCaseName' to registry. Proceeding with data import to its sheet, but it won't be listed until registry issue is fixed.")
            // This is a problematic state. The case files are created but not registered.
            // For now, we continue to populate, but this might need better error handling or cleanup.
        }

        // 5. Parse and Store Allegations into the new case's spreadsheet
        val allegationsSheet = sheetsData["Exhibit Matrix - Allegations"]
        allegationsSheet?.drop(1)?.forEach { row -> // Assuming first row is header
            val allegationText = row.getOrNull(3)?.toString()
            if (!allegationText.isNullOrBlank()) {
                val success = googleApiService.addAllegationToCase(newCaseSpreadsheetId, allegationText)
                if (!success) {
                    Log.w(TAG, "parseAndStore: Failed to add allegation: '$allegationText' to $newCaseSpreadsheetId")
                }
            }
        }

        // 6. Parse and Store Evidence into the new case's spreadsheet
        val evidenceSheet = sheetsData["Evidence"]
        evidenceSheet?.drop(1)?.forEach { row -> // Assuming first row is header
            try {
                val content = row.getOrNull(0)?.toString()
                val tagsStr = row.getOrNull(1)?.toString()

                if (content != null) {
                    val evidence = Evidence(
                        caseId = newCase.id, // This id is local and not the primary key in Sheets
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Imported - Evidence Sheet",
                        documentDate = System.currentTimeMillis(),
                        tags = tagsStr?.split(",")?.map { it.trim() } ?: emptyList(),
                        allegationId = null
                    )
                    val success = googleApiService.addEvidenceToCase(newCaseSpreadsheetId, evidence)
                    if (!success) {
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
