package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.auth.CredentialHolder // Added import
import javax.inject.Inject

class AllegationsRepositoryImpl
    @Inject
    constructor(
        private val credentialHolder: CredentialHolder, // Changed from GoogleApiService?
    ) : AllegationsRepository {
        override suspend fun getAllegations(caseId: String): List<Allegation> {
            val googleApiService = credentialHolder.googleApiService // Get from holder
            if (googleApiService == null) {
                Log.e("AllegationsRepository", "GoogleApiService is null in getAllegations. Cannot fetch case allegations.")
                return emptyList()
            }
            val spreadsheetId = "1TN9MLnzpCJjcO9bwEhTeOjon3mRunYs5_tSxII6LizA" // Example, ensure this is correct
            val sheetData = googleApiService.readSpreadsheet(spreadsheetId)
            if (sheetData.isNullOrEmpty()) {
                return emptyList()
            }
            val sheet = sheetData.values.firstOrNull() ?: return emptyList()
            return sheet.mapIndexedNotNull { index, row ->
                if (row.size >= 3) {
                    Allegation(
                        id = index,
                        spreadsheetId = spreadsheetId,
                        text = row[2].toString(),
                    )
                } else {
                    null
                }
            }
        }
    }
