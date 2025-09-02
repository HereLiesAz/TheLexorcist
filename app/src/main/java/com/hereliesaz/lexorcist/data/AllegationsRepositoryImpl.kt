package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import javax.inject.Inject

class AllegationsRepositoryImpl
    @Inject
    constructor(
        private val googleApiService: GoogleApiService?,
    ) : AllegationsRepository {
        override suspend fun getAllegations(caseId: String): List<Allegation> {
            val spreadsheetId = "1TN9MLnzpCJjcO9bwEhTeOjon3mRunYs5_tSxII6LizA"
            val sheetData = googleApiService?.readSpreadsheet(spreadsheetId)
            if (sheetData.isNullOrEmpty()) {
                return emptyList()
            }

            val sheet = sheetData.values.firstOrNull() ?: return emptyList()

            return sheet.mapIndexedNotNull { index, row ->
                if (row.size >= 3) {
                    Allegation(
                        id = index,
                        spreadsheetId = spreadsheetId,
                        text = row[2].toString()
                    )
                } else {
                    null
                }
            }
        }
    }
