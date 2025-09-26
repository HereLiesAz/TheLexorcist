package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.service.GoogleApiService // Added import
import javax.inject.Inject
// Removed: import com.hereliesaz.lexorcist.auth.CredentialHolder

class AllegationsRepositoryImpl
@Inject
constructor(
    private val googleApiService: GoogleApiService,
    private val allegationProvider: AllegationProvider
) : AllegationsRepository {
    override suspend fun getAllegations(caseId: String): List<Allegation> {
        val staticAllegations = allegationProvider.getAllAllegations()
        val dynamicAllegations = mutableListOf<Allegation>()

        val spreadsheetId = "1TN9MLnzpCJjcO9bwEhTeOjon3mRunYs5_tSxII6LizA"
        Log.d("AllegationsRepository", "Fetching allegations from spreadsheet: $spreadsheetId for case: $caseId")

        val sheetData = googleApiService.readSpreadsheet(spreadsheetId, isPublic = true)
        if (sheetData.isNullOrEmpty()) {
            Log.w("AllegationsRepository", "Sheet data is null or empty for spreadsheet: $spreadsheetId")
            return staticAllegations
        }

        var allegationsSheet = sheetData["Allegations"]
        if (allegationsSheet.isNullOrEmpty()) {
            Log.w("AllegationsRepository", "'Allegations' sheet is null or empty, or not found in spreadsheet: $spreadsheetId. Trying first sheet.")
            allegationsSheet = sheetData.values.firstOrNull()
        }

        if (allegationsSheet.isNullOrEmpty()) {
            Log.w("AllegationsRepository", "No sheets with data found in spreadsheet: $spreadsheetId")
            return staticAllegations
        }

        val mappedAllegations = allegationsSheet.mapIndexedNotNull { index, row ->
            if (row.size >= 3) {
                val allegationText = row[2].toString()
                Allegation(
                    id = "sheet_${spreadsheetId}_${index}",
                    text = allegationText,
                    allegationElementName = allegationText, // Using text as a fallback
                    description = "",
                    elements = emptyList(),
                    evidenceSuggestions = emptyList()
                )
            } else {
                Log.w("AllegationsRepository", "Skipping row due to insufficient columns: $row")
                null
            }
        }
        dynamicAllegations.addAll(mappedAllegations)

        return staticAllegations + dynamicAllegations
    }
}
