package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import javax.inject.Inject

class SpreadsheetParser @Inject constructor(
    private val schema: SpreadsheetSchema
    // private val caseDao: CaseDao // Removed caseDao dependency
) {

    private val tag = "SpreadsheetParser"

    suspend fun parseAndStore(spreadsheetId: String, sheetData: Map<String, List<List<Any>>>): Case? {
        val caseDetailsSheetName = schema.caseInfoSheet.name
        // val evidenceSheetName = schema.evidenceSheet.name // For future use

        val caseDetailsData = sheetData[caseDetailsSheetName]

        if (caseDetailsData == null) {
            Log.e(tag, "Case Details sheet ('$caseDetailsSheetName') not found in the imported data for $spreadsheetId")
            return null
        }

        // Assuming schema.caseInfoSheet.caseNameLabel points to the header, and data is in the next row.
        // And schema.caseInfoSheet.caseNameColumn is the 0-based index of the column for the case name.
        // The extractStringValue helper will assume row 0 is header, row 1 is first data line.
        val caseName = extractStringValue(caseDetailsData, schema.caseInfoSheet.caseNameColumn)
            ?: "Imported Case - $spreadsheetId" // Default name if not found

        // Create a Case object. The ID will be handled by the Google Sheets registry, not here.
        val newCase = Case(
            id = 0, // Placeholder ID; actual ID will be from registry or not used for unsaved parsed object
            name = caseName,
            spreadsheetId = spreadsheetId,
            // Initialize other fields with defaults or parse them if schema supports
            scriptId = null,
            generatedPdfId = null,
            sourceHtmlSnapshotId = null,
            originalMasterHtmlTemplateId = null,
            folderId = null, // This would likely be determined when saving/creating the case folder
            plaintiffs = null, // TODO: Parse if in schema
            defendants = null, // TODO: Parse if in schema
            court = null, // TODO: Parse if in schema
            isArchived = false,
            lastModifiedTime = System.currentTimeMillis() // Or null if not applicable here
        )

        // TODO: Implement parsing of evidence items from sheetData[evidenceSheetName]
        // This will involve iterating through rows of evidenceLogData, creating Evidence objects,
        // and potentially returning them along with the Case object, or a more complex structure.

        return newCase // Return the parsed Case object
    }

    /**
     * Extracts a string value from sheet data.
     * Assumes the first row (index 0) is headers, and actual data starts from the second row (index 1).
     * @param sheetPageData The list of rows for a specific sheet.
     * @param columnIndex The 0-based index of the column to extract data from.
     * @return The string value or null if not found or data is not as expected.
     */
    private fun extractStringValue(
        sheetPageData: List<List<Any>>?,
        columnIndex: Int
    ): String? {
        // Check if sheetData is null or empty, or if columnIndex is out of bounds for data rows
        if (sheetPageData.isNullOrEmpty() || sheetPageData.size < 2) { // Need at least header + 1 data row
            Log.w(tag, "Sheet data is null, empty, or has no data rows for extraction.")
            return null
        }
        // Attempt to get data from the first data row (index 1)
        val dataRow = sheetPageData.getOrNull(1)
        if (dataRow == null) {
            Log.w(tag, "First data row (index 1) is missing.")
            return null
        }
        if (columnIndex < 0 || columnIndex >= dataRow.size) {
            Log.w(tag, "Column index $columnIndex is out of bounds for data row with ${dataRow.size} columns.")
            return null
        }
        return dataRow.getOrNull(columnIndex)?.toString()
    }
}
