package com.hereliesaz.lexorcist.data // Assuming this is the correct package

import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.model.Case // Added import
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import javax.inject.Inject

class SpreadsheetParser @Inject constructor(
    private val googleApiService: GoogleApiService,
    private val schema: SpreadsheetSchema,
    private val caseDao: CaseDao
) {

    suspend fun parseAndStore(sheetData: Map<String, List<List<Any>>>): Case? {
        val caseDetailsSheet = schema.sheets.find { it.name == "Case Details" }
        val evidenceSheet = schema.sheets.find { it.name == "Evidence Log" }

        if (caseDetailsSheet == null || evidenceSheet == null) {
            // Log error: Schema definition missing for required sheets
            return null
        }

        val caseDetailsData = sheetData[caseDetailsSheet.name]
        val evidenceLogData = sheetData[evidenceSheet.name]

        if (caseDetailsData == null || evidenceLogData == null) {
            // Log error: Required sheets not found in the imported data
            return null
        }

        val caseName = extractStringValue(caseDetailsData, caseDetailsSheet, "Case Name") ?: "Imported Case"
        // Placeholder for the ID of the spreadsheet being parsed. This is a complex topic
        // as the map itself doesn't directly provide it. For now, empty.
        val sourceSpreadsheetId = "" 

        val newCase = Case(
            name = caseName,
            spreadsheetId = sourceSpreadsheetId 
            // Initialize other fields as needed based on your schema and extraction logic
        )

        // Save the case to get an ID from the database
        val caseId = caseDao.insert(newCase) // Assuming insert returns the new row ID (Long)

        // TODO: Implement parsing and storing of evidence items from evidenceLogData.
        // This will involve iterating through rows of evidenceLogData, creating Evidence objects,
        // linking them to caseId, and saving them via evidenceDao.

        // Return the case, potentially after re-fetching from DB to get all updated fields
        return caseDao.getCaseById(caseId.toInt())
    }

    private fun extractStringValue(
        sheetData: List<List<Any>>,
        sheetSchema: SpreadsheetSchema.SheetDefinition,
        columnName: String
    ): String? {
        val columnIndex = sheetSchema.columns.indexOfFirst { it.name == columnName }
        if (columnIndex == -1) return null
        
        // This assumes actual data starts from the second row (index 1) of the sheetData list,
        // and that case details are typically single values in a specific row (e.g., the first data row).
        // Adjust indices if your sheet structure is different (e.g., headers are not in sheetData).
        return sheetData.getOrNull(1)?.getOrNull(columnIndex)?.toString()
    }

    // You might need other helper functions like extractIntValue, extractDateValue, etc.
}
