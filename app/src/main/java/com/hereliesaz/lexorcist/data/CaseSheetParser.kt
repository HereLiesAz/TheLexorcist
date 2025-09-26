package com.hereliesaz.lexorcist.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence

object CaseSheetParser {
    fun parseCaseFromData(spreadsheetId: String, sheetData: Map<String, List<List<Any>>>): Pair<Case, List<Evidence>>? {
        // Placeholder implementation
        val caseDetailsSheet = sheetData["Case Details"] ?: return null
        val evidenceSheet = sheetData["Evidence"] ?: emptyList()

        var plaintiffs = ""
        var defendants = ""
        var court = ""
        var caseName = "Imported Case" // Default name

        for (row in caseDetailsSheet) {
            if (row.isEmpty()) continue
            val key = row[0].toString()
            val value = if (row.size > 1) row[1].toString() else ""
            when (key) {
                "Case Name" -> caseName = value
                "Plaintiff(s)" -> plaintiffs = value
                "Defendant(s)" -> defendants = value
                "Court" -> court = value
            }
        }

        val newCase = Case(
            name = caseName,
            spreadsheetId = spreadsheetId,
            plaintiffs = plaintiffs,
            defendants = defendants,
            court = court,
            id = 0, // Will be replaced by actual ID upon creation
            folderId = "", // Should be set during case creation
            isArchived = false,
            lastModifiedTime = System.currentTimeMillis()
        )

        val evidenceList = evidenceSheet.mapNotNull { row ->
            if (row.size >= 5) {
                Evidence(
                    id = row[0].toString(),
                    caseId = 0, // Placeholder
                    spreadsheetId = spreadsheetId,
                    type = row[1].toString(),
                    content = row[2].toString(),
                    timestamp = row[3].toString().toLongOrNull() ?: System.currentTimeMillis(),
                    sourceDocument = row[4].toString(),
                    documentDate = System.currentTimeMillis(), // Placeholder
                    allegationId = "", // Placeholder
                    allegationElementName = "" // Placeholder
                )
            } else {
                null
            }
        }

        return Pair(newCase, evidenceList)
    }
}
