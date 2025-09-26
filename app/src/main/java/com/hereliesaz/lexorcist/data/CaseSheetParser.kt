package com.hereliesaz.lexorcist.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream

object CaseSheetParser {

    fun parse(inputStream: InputStream): Triple<Map<String, Any>, List<Allegation>, List<Evidence>> {
        // This is a placeholder implementation. A real implementation would parse the sheet.
        val caseDetails = mapOf("Plaintiff(s)" to "", "Defendant(s)" to "", "Court" to "")
        val allegations = listOf(
            Allegation(spreadsheetId = "", text = "Allegation 1", allegationElementName = "Element 1"),
            Allegation(spreadsheetId = "", text = "Allegation 2", allegationElementName = "Element 2")
        )
        val evidence = listOf(
            Evidence(id = "1", caseId = 1, spreadsheetId = "", type = "text", content = "Evidence 1", timestamp = System.currentTimeMillis(), sourceDocument = "doc1.pdf", documentDate = System.currentTimeMillis(), allegationId = "1", allegationElementName = "Element 1")
        )
        return Triple(caseDetails, allegations, evidence)
    }

    fun parseCaseFromData(spreadsheetId: String, sheetData: Map<String, List<List<Any>>>): Pair<Case, List<Evidence>>? {
        val detailsSheet = sheetData["Details"]
        val caseName = detailsSheet?.getOrNull(0)?.getOrNull(1)?.toString() ?: "Imported Case"
        val plaintiffs = detailsSheet?.getOrNull(1)?.getOrNull(1)?.toString() ?: ""
        val defendants = detailsSheet?.getOrNull(2)?.getOrNull(1)?.toString() ?: ""
        val court = detailsSheet?.getOrNull(3)?.getOrNull(1)?.toString() ?: ""

        val newCase = Case(
            name = caseName,
            spreadsheetId = spreadsheetId,
            plaintiffs = plaintiffs,
            defendants = defendants,
            court = court,
            id = 0,
            folderId = "",
            isArchived = false,
            lastModifiedTime = System.currentTimeMillis()
        )

        val evidenceSheet = sheetData["Evidence"]
        val evidenceList = evidenceSheet?.mapNotNull { row ->
            if (row.size >= 5) {
                Evidence(
                    id = row[0].toString(),
                    caseId = 0,
                    spreadsheetId = spreadsheetId,
                    type = row[1].toString(),
                    content = row[2].toString(),
                    timestamp = row[3].toString().toLongOrNull() ?: 0L,
                    sourceDocument = row[4].toString(),
                    documentDate = System.currentTimeMillis(),
                    allegationId = null,
                    allegationElementName = ""
                )
            } else {
                null
            }
        } ?: emptyList()

        return Pair(newCase, evidenceList)
    }
}
