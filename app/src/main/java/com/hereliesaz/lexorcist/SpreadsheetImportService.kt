package com.hereliesaz.lexorcist

import android.util.Log
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import java.io.IOException

class SpreadsheetImportService(
    private val googleApiService: GoogleApiService,
    private val schema: SpreadsheetSchema,
) {
    private val tag = "SpreadsheetImportSvc"

    suspend fun importAndSetupNewCaseFromData(sheetsData: Map<String, List<List<Any>>>): Case? {
        // 1. Get Root Folder and Registry IDs
        val appRootFolderIdResult = googleApiService.getOrCreateAppRootFolder()
        val appRootFolderId: String? = when (val result = appRootFolderIdResult) {
            is Result.Success -> result.data
            is Result.Error -> {
                Log.e(tag, "importAndSetup: Failed to get or create app root folder. Error: ${result.exception}")
                null
            }
            is Result.UserRecoverableError -> {
                Log.e(tag, "importAndSetup: User recoverable error getting or creating app root folder. Error: ${result.exception}")
                null
            }
        }

        if (appRootFolderId == null) {
            // Log message already handled in the when block above
            return null
        }

        val caseRegistrySpreadsheetId: String? =
            try {
                googleApiService.getOrCreateCaseRegistrySpreadsheetId(appRootFolderId)
            } catch (e: IOException) {
                Log.e(tag, "importAndSetup: Failed to get or create case registry spreadsheet for folder $appRootFolderId.", e)
                null
            }

        if (caseRegistrySpreadsheetId == null) {
            // Log message already handled in catch
            return null
        }

        // 2. Extract Case Name from imported sheet
        val caseInfoSheet = sheetsData[schema.caseInfoSheet.name]
        val importedCaseName =
            caseInfoSheet
                ?.find {
                    it.getOrNull(0)?.toString()?.equals(schema.caseInfoSheet.caseNameLabel, ignoreCase = true) ?: false
                }?.getOrNull(schema.caseInfoSheet.caseNameColumn)
                ?.toString() ?: "Imported Case ${System.currentTimeMillis()}"
        Log.d(tag, "importAndSetup: Determined case name from import data: $importedCaseName")

        // 2.1 Check for Existing Case with the same name in the registry
        val existingCases = googleApiService.getAllCasesFromRegistry(caseRegistrySpreadsheetId)
        val duplicateCase = existingCases.find { it.name.equals(importedCaseName, ignoreCase = true) }
        if (duplicateCase != null) {
            Log.w(
                tag,
                "importAndSetup: Case with name '$importedCaseName' already exists. Import aborted. Spreadsheet ID: ${duplicateCase.spreadsheetId}",
            )
            return null // Do not proceed if case with same name exists
        }

        // 3. Create New Case Structure
        Log.d(tag, "importAndSetup: No existing case found with name '$importedCaseName'. Proceeding to create new case structure.")

        val newCaseFolderId: String? = googleApiService.getOrCreateCaseFolder(importedCaseName)

        if (newCaseFolderId == null) {
            Log.e(tag, "importAndSetup: Failed to create or retrieve folder for new case: $importedCaseName. getOrCreateCaseFolder returned null.")
            return null
        }

        val newCaseSpreadsheetIdResult = googleApiService.createSpreadsheet(importedCaseName, newCaseFolderId)
        val newCaseSpreadsheetId: String = when (val result = newCaseSpreadsheetIdResult) {
            is Result.Success -> {
                val id = result.data
                if (id == null) {
                    Log.e(tag, "importAndSetup: createSpreadsheet returned success but with a null ID for case: $importedCaseName")
                    return null
                }
                id
            }
            is Result.Error -> {
                Log.e(
                    tag,
                    "importAndSetup: Failed to create spreadsheet for new case: $importedCaseName. Error: ${result.exception}",
                )
                return null
            }
            is Result.UserRecoverableError -> {
                Log.e(
                    tag,
                    "importAndSetup: User recoverable error creating spreadsheet for new case: $importedCaseName. Error: ${result.exception}",
                )
                return null
            }
        }

        Log.d(tag, "importAndSetup: Created new case structure: Folder ID $newCaseFolderId, Spreadsheet ID $newCaseSpreadsheetId")

        val placeholderScriptContent = "// Default script for imported case\nfunction onOpen() {}\n"
        googleApiService.attachScript(newCaseSpreadsheetId, placeholderScriptContent, "ImportedCaseScript")

        val newCaseForRegistry = Case(id = 0, name = importedCaseName, spreadsheetId = newCaseSpreadsheetId, folderId = newCaseFolderId)
        val addedToRegistry = googleApiService.addCaseToRegistry(caseRegistrySpreadsheetId, newCaseForRegistry)
        if (!addedToRegistry) {
            Log.w(
                tag,
                "importAndSetup: Failed to add new case '$importedCaseName' to registry. Data import will proceed, but case won't be auto-listed.",
            )
        }

        val allegationsSheetName = schema.allegationsSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, allegationsSheetName)
        val allegationsDataToAppend =
            sheetsData[allegationsSheetName]?.drop(1)?.mapNotNull { row ->
                val allegationText = row.getOrNull(schema.allegationsSheet.allegationColumn)?.toString()
                if (!allegationText.isNullOrBlank()) listOf(allegationText) else null
            } ?: emptyList()

        if (allegationsDataToAppend.isNotEmpty()) {
            Log.d(
                tag,
                "Attempting to append ${allegationsDataToAppend.size} allegations to $allegationsSheetName in $newCaseSpreadsheetId",
            )
            // googleApiService.appendData(newCaseSpreadsheetId, allegationsSheetName + "!A1", allegationsDataToAppend)
        }

        val evidenceSheetName = schema.evidenceSheet.name
        googleApiService.addSheet(newCaseSpreadsheetId, evidenceSheetName)
        // val evidenceHeader = listOf("Content", "Timestamp", "Source Document", "Document Date", "Tags", "Allegation ID", "Category", "Type", "SpreadsheetID")
        // googleApiService.appendData(newCaseSpreadsheetId, evidenceSheetName + "!A1", listOf(evidenceHeader))

        val evidenceDataToAppend =
            sheetsData[evidenceSheetName]?.drop(1)?.mapNotNull { row ->
                val content = row.getOrNull(schema.evidenceSheet.contentColumn)?.toString()
                if (content.isNullOrBlank()) {
                    null
                } else {
                    // val tagsStr = row.getOrNull(schema.evidenceSheet.tagsColumn)?.toString()
                    listOf(
                        content,
                        System.currentTimeMillis().toString(),
                        "Imported from spreadsheet",
                        System.currentTimeMillis().toString(),
                        row.getOrNull(schema.evidenceSheet.tagsColumn)?.toString() ?: "",
                        "",
                        "Imported",
                        "imported_text_evidence",
                        newCaseSpreadsheetId,
                    )
                }
            } ?: emptyList()

        if (evidenceDataToAppend.isNotEmpty()) {
            Log.d(tag, "Attempting to append ${evidenceDataToAppend.size} evidence items to $evidenceSheetName in $newCaseSpreadsheetId")
            // googleApiService.appendData(newCaseSpreadsheetId, evidenceSheetName + "!A2", evidenceDataToAppend)
        }

        Log.i(
            tag,
            "importAndSetup: Finished importing and setting up data for case: $importedCaseName, New Spreadsheet ID: $newCaseSpreadsheetId",
        )
        return Case(name = importedCaseName, spreadsheetId = newCaseSpreadsheetId, folderId = newCaseFolderId, id = newCaseForRegistry.id)
    }
}
