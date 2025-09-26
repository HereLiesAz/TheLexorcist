package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import javax.inject.Inject

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CaseSheetParser
    @Inject
    constructor(
        private val schema: SpreadsheetSchema,
        private val gson: Gson,
    ) {
        private val tag = "CaseSheetParser"

        // Renamed from parseAndStore to parseCaseFromData
        suspend fun parseCaseFromData(
            spreadsheetId: String,
            sheetData: Map<String, List<List<Any>>>,
        ): Pair<Case, List<Evidence>>? {
            val caseDetailsSheetName = schema.caseInfoSheet.name
            // val evidenceSheetName = schema.evidenceSheet.name // For future use

            val caseDetailsData = sheetData[caseDetailsSheetName]

            if (caseDetailsData == null) {
                Log.e(tag, "Case Details sheet ('$caseDetailsSheetName') not found in the imported data for $spreadsheetId")
                return null
            }

            val caseName =
                extractStringValue(caseDetailsData, schema.caseInfoSheet.caseNameColumn)
                    ?: "Imported Case - $spreadsheetId" // Default name if not found

            val newCase =
                Case(
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
                    lastModifiedTime = System.currentTimeMillis(),
                )

            val evidenceSheetName = schema.evidenceSheet.name
            val evidenceSheetData = sheetData[evidenceSheetName]
            val evidenceList = mutableListOf<Evidence>()

            if (evidenceSheetData != null) {
                for (i in 1 until evidenceSheetData.size) { // Skip header row
                    val row = evidenceSheetData[i]
                    val evidence = Evidence(
                        id = (row.getOrNull(0) as? Number)?.toInt() ?: 0,
                        caseId = (row.getOrNull(1) as? Number)?.toLong() ?: 0L,
                        spreadsheetId = spreadsheetId,
                        type = row.getOrNull(2)?.toString() ?: "",
                        content = row.getOrNull(3)?.toString() ?: "",
                        formattedContent = row.getOrNull(4)?.toString(),
                        mediaUri = row.getOrNull(5)?.toString(),
                        timestamp = (row.getOrNull(6) as? Number)?.toLong() ?: 0L,
                        sourceDocument = row.getOrNull(7)?.toString() ?: "",
                        documentDate = (row.getOrNull(8) as? Number)?.toLong() ?: 0L,
                        allegationId = (row.getOrNull(9) as? Number)?.toInt()?.toString(), // Changed to String?
                        allegationElementName = null, // Added parameter
                        category = row.getOrNull(10)?.toString() ?: "",
                        tags = (row.getOrNull(11)?.toString() ?: "").split(",").filter { it.isNotBlank() },
                        commentary = row.getOrNull(12)?.toString(),
                        linkedEvidenceIds = (row.getOrNull(13)?.toString() ?: "").split(",").mapNotNull { it.toIntOrNull() },
                        parentVideoId = row.getOrNull(14)?.toString(),
                        entities = gson.fromJson(row.getOrNull(15)?.toString() ?: "{}", object : TypeToken<Map<String, List<String>>>() {}.type),
                    )
                    evidenceList.add(evidence)
                }
            }

            // The evidence list is not stored in the Case object directly.
            // This parser should probably return the case and the evidence list separately.
            // For now, I will just return the case. The evidence will be parsed and stored
            // by another service that uses this parser.

            return Pair(newCase, evidenceList)
        }

        private fun extractStringValue(
            sheetPageData: List<List<Any>>?,
            columnIndex: Int,
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