package com.hereliesaz.lexorcist.data

import android.util.Log

object CaseSheetParser {

    private const val TAG = "CaseSheetParser"

    fun parseCaseFromData(spreadsheetId: String, sheetData: Map<String, List<List<Any>>>): Pair<Case, List<Evidence>>? {
        try {
            // Assume the first sheet contains case details in a key-value format
            val caseDetailsSheet = sheetData["Case Details"] ?: sheetData.values.firstOrNull() ?: run {
                Log.w(TAG, "No sheets found in spreadsheet $spreadsheetId")
                return null
            }

            val detailsMap = caseDetailsSheet.associate { row ->
                if (row.size >= 2) row[0].toString().trim() to row[1].toString().trim() else "" to ""
            }.filterKeys { it.isNotEmpty() }

            val caseName = detailsMap["Case Name"] ?: "Unnamed Case from $spreadsheetId"
            val plaintiffs = detailsMap["Plaintiffs"]
            val defendants = detailsMap["Defendants"]
            val court = detailsMap["Court"]
            val caseNumber = detailsMap["Case Number"]
            val caseSection = detailsMap["Case Section"]

            val newCase = Case(
                name = caseName,
                spreadsheetId = spreadsheetId,
                plaintiffs = plaintiffs,
                defendants = defendants,
                court = court,
                caseNumber = caseNumber,
                caseSection = caseSection,
                lastModifiedTime = System.currentTimeMillis()
            )

            // Assume a sheet named "Evidence" for the evidence list
            val evidenceSheet = sheetData["Evidence"]
            val evidenceList = if (evidenceSheet != null) {
                // Assuming first row is header, so we drop it
                evidenceSheet.drop(1).mapNotNull { row ->
                    try {
                        if (row.isEmpty() || row[0].toString().isBlank()) return@mapNotNull null
                        Evidence(
                            id = row.getOrNull(0)?.toString() ?: return@mapNotNull null,
                            caseId = 0L, // This can be updated after the case is created
                            spreadsheetId = spreadsheetId,
                            type = row.getOrNull(1)?.toString() ?: "text",
                            content = row.getOrNull(2)?.toString() ?: "",
                            timestamp = row.getOrNull(3)?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
                            sourceDocument = row.getOrNull(4)?.toString(),
                            documentDate = row.getOrNull(5)?.toString()?.toLongOrNull(),
                            allegationId = row.getOrNull(6)?.toString(),
                            allegationElementName = row.getOrNull(7)?.toString()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing evidence row: $row", e)
                        null
                    }
                }
            } else {
                emptyList()
            }

            return Pair(newCase, evidenceList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse case from sheet data for spreadsheet: $spreadsheetId", e)
            return null
        }
    }
}