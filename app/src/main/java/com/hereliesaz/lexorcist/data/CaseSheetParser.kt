package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import javax.inject.Inject

// Renamed from SpreadsheetParser to CaseSheetParser
class CaseSheetParser @Inject constructor(
    private val schema: SpreadsheetSchema
) {

    private val tag = "CaseSheetParser" // Updated tag

    // Renamed from parseAndStore to parseCaseFromData
    suspend fun parseCaseFromData(spreadsheetId: String, sheetData: Map<String, List<List<Any>>>): Case? {
        val caseDetailsSheetName = schema.caseInfoSheet.name
        // val evidenceSheetName = schema.evidenceSheet.name // For future use

        val caseDetailsData = sheetData[caseDetailsSheetName]

        if (caseDetailsData == null) {
            Log.e(tag, "Case Details sheet ('$caseDetailsSheetName') not found in the imported data for $spreadsheetId")
            return null
        }

        val caseName = extractStringValue(caseDetailsData, schema.caseInfoSheet.caseNameColumn)
            ?: "Imported Case - $spreadsheetId" // Default name if not found

        val newCase = Case(
            id = 0, 
            name = caseName,
            spreadsheetId = spreadsheetId,
            scriptId = null,
            generatedPdfId = null,
            sourceHtmlSnapshotId = null,
            originalMasterHtmlTemplateId = null,
            folderId = null, 
            plaintiffs = null, 
            defendants = null, 
            court = null, 
            isArchived = false,
            lastModifiedTime = System.currentTimeMillis()
        )

        // TODO: Implement parsing of evidence items from sheetData[evidenceSheetName]
        return newCase
    }

    private fun extractStringValue(
        sheetPageData: List<List<Any>>?,
        columnIndex: Int
    ): String? {
        if (sheetPageData.isNullOrEmpty() || sheetPageData.size < 2) { 
            Log.w(tag, "Sheet data is null, empty, or has no data rows for extraction.")
            return null
        }
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
