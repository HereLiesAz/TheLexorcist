package com.hereliesaz.lexorcist

import com.hereliesaz.lexorcist.db.Allegation
import com.hereliesaz.lexorcist.db.AllegationDao
import com.hereliesaz.lexorcist.db.CaseDao
import com.hereliesaz.lexorcist.db.FinancialEntry
import com.hereliesaz.lexorcist.db.FinancialEntryDao
import java.text.SimpleDateFormat
import java.util.*

class SpreadsheetParser(
    private val caseDao: CaseDao,
    private val allegationDao: AllegationDao,
    private val financialEntryDao: FinancialEntryDao
) {
    suspend fun parseAndStore(sheetsData: Map<String, List<List<Any>>>) {
        val caseData = sheetsData["Case Info"]
        val caseName = caseData?.find { it.getOrNull(0) == "Case Name" }?.getOrNull(1)?.toString() ?: "Imported Case"
        val spreadsheetId = caseData?.find { it.getOrNull(0) == "Spreadsheet ID" }?.getOrNull(1)?.toString() ?: ""

        val caseId = caseDao.insert(com.hereliesaz.lexorcist.db.Case(name = caseName, spreadsheetId = spreadsheetId))

        val allegationsSheet = sheetsData["Exhibit Matrix - Allegations"]
        allegationsSheet?.drop(1)?.forEach { row ->
            val allegationText = row.getOrNull(3)?.toString() ?: ""
            if (allegationText.isNotBlank()) {
                val allegationId = allegationDao.insert(Allegation(caseId = caseId.toInt(), text = allegationText))

                // Now you can parse other sheets and link them to this allegationId
            }
        }

        val damagesSheet = sheetsData["Damages Analysis"]
        damagesSheet?.drop(1)?.forEach { row ->
            val plaintiff = row.getOrNull(0)?.toString()
            val category = row.getOrNull(1)?.toString()
            val amount = row.getOrNull(2)?.toString()

            if (plaintiff != null && category != null && amount != null) {
                // Here you would ideally link this to a specific plaintiff entity
                // For now, we'll just create a financial entry
                financialEntryDao.insert(
                    FinancialEntry(
                        caseId = caseId.toInt(),
                        amount = amount,
                        timestamp = System.currentTimeMillis(),
                        sourceDocument = "Damages Analysis Sheet",
                        documentDate = System.currentTimeMillis(),
                        category = category
                    )
                )
            }
        }
    }
}