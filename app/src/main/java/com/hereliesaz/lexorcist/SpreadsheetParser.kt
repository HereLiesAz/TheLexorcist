package com.hereliesaz.lexorcist

import android.content.Context
import android.util.Log
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.R

class SpreadsheetParser(
    private val context: Context,
    private val googleApiService: GoogleApiService
) {
    private val TAG = context.getString(R.string.spreadsheet_parser_tag)

    suspend fun parseAndStore(sheetsData: Map<String, List<List<Any>>>): Case? {
        // 1. Get Root Folder and Registry IDs
        val appRootFolderId = googleApiService.getOrCreateAppRootFolder()
        if (appRootFolderId == null) {
            Log.e(TAG, context.getString(R.string.parse_store_failed_app_root_folder))
            return null
        }
        val caseRegistrySpreadsheetId = googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
        if (caseRegistrySpreadsheetId == null) {
            Log.e(TAG, context.getString(R.string.parse_store_failed_case_registry))
            return null
        }

        // 2. Extract Case Name from imported sheet
        val caseInfoSheet = sheetsData[context.getString(R.string.case_info_sheet_name)]
        val importedCaseName = caseInfoSheet?.find { it.getOrNull(0)?.toString()?.equals(context.getString(R.string.case_name_column), ignoreCase = true) == true }
            ?.getOrNull(1)?.toString() ?: context.getString(R.string.imported_case_default_name, System.currentTimeMillis())
        Log.d(TAG, context.getString(R.string.parse_store_determined_case_name, importedCaseName))

        // 2.1 Check for Existing Case with the same name in the registry
        val existingCases = googleApiService.getAllCasesFromRegistry(caseRegistrySpreadsheetId)
        val duplicateCase = existingCases.find { it.name.equals(importedCaseName, ignoreCase = true) }
        if (duplicateCase != null) {
            Log.w(TAG, context.getString(R.string.parse_store_case_exists, importedCaseName, duplicateCase.spreadsheetId))
            return null // Indicate that import was skipped due to duplicate
        }

        // 3. Create New Case Structure (since no duplicate was found)
        Log.d(TAG, context.getString(R.string.parse_store_no_existing_case, importedCaseName))
        
        // val masterTemplateId = googleApiService.createMasterTemplate(appRootFolderId) // Removed call
        val originalMasterHtmlTemplateIdForCaseRecord: String? = null // This will be null for imported cases now

        val newCaseFolderId = googleApiService.getOrCreateFolder(importedCaseName, appRootFolderId)
        if (newCaseFolderId == null) {
            Log.e(TAG, context.getString(R.string.parse_store_failed_create_folder, importedCaseName))
            return null
        }
        val newCaseSpreadsheetId = googleApiService.createSpreadsheet(importedCaseName, newCaseFolderId)
        if (newCaseSpreadsheetId == null) {
            Log.e(TAG, context.getString(R.string.parse_store_failed_create_spreadsheet, importedCaseName))
            return null
        }
        
        // Corrected attachScript call for imported cases
        // For now, using placeholder script content. Ideally, this would load a suitable script template.
        val placeholderScriptContent = context.getString(R.string.default_imported_case_script)
        val masterIdForScriptConfig = "" // No specific PDF/Doc template for imported cases initially
        googleApiService.attachScript(newCaseSpreadsheetId, placeholderScriptContent, masterIdForScriptConfig)
        
        Log.d(TAG, context.getString(R.string.parse_store_created_new_case, newCaseFolderId, newCaseSpreadsheetId))

        // 4. Add to Case Registry
        // originalMasterHtmlTemplateId will be null for this newly imported case
        val newCase = Case(name = importedCaseName, spreadsheetId = newCaseSpreadsheetId, originalMasterHtmlTemplateId = originalMasterHtmlTemplateIdForCaseRecord)
        val addedToRegistry = googleApiService.addCaseToRegistry(caseRegistrySpreadsheetId, newCase)
        if (!addedToRegistry) {
            Log.w(TAG, context.getString(R.string.parse_store_failed_add_case_to_registry, importedCaseName))
            // This is a problematic state. The case files are created but not registered.
            // For now, we continue to populate, but this might need better error handling or cleanup.
        }

        // 5. Parse and Store Allegations into the new case's spreadsheet
        val allegationsSheet = sheetsData[context.getString(R.string.exhibit_matrix_allegations_sheet_name)]
        allegationsSheet?.drop(1)?.forEach { row -> // Assuming first row is header
            val allegationText = row.getOrNull(3)?.toString()
            if (!allegationText.isNullOrBlank()) {
                val success = googleApiService.addAllegationToCase(newCaseSpreadsheetId, allegationText)
                if (!success) {
                    Log.w(TAG, context.getString(R.string.parse_store_failed_add_allegation, allegationText, newCaseSpreadsheetId))
                }
            }
        }

        // 6. Parse and Store Evidence into the new case's spreadsheet
        val evidenceSheet = sheetsData[context.getString(R.string.evidence_sheet_name)]
        evidenceSheet?.drop(1)?.forEach { row -> // Assuming first row is header
            try {
                val content = row.getOrNull(0)?.toString()
                val tagsStr = row.getOrNull(1)?.toString()

                if (content != null) {
                    val evidence = Evidence(
                        caseId = newCase.id, // This id is local and not the primary key in Sheets
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = context.getString(R.string.imported_evidence_sheet_source),
                        documentDate = System.currentTimeMillis(),
                        tags = tagsStr?.split(context.getString(R.string.comma_separator))?.map { it.trim() } ?: emptyList(),
                        allegationId = null
                    )
                    val success = googleApiService.addEvidenceToCase(newCaseSpreadsheetId, evidence)
                    if (!success) {
                        Log.w(TAG, context.getString(R.string.parse_store_failed_add_evidence, evidence, newCaseSpreadsheetId))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, context.getString(R.string.parse_store_error_parsing_evidence_row, row), e)
            }
        }
        Log.i(TAG, context.getString(R.string.parse_store_finished_import, importedCaseName, newCaseSpreadsheetId))
        return newCase // Return the newly created Case object
    }
}
