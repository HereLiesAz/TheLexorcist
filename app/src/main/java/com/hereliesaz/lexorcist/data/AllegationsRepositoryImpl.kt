package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.hereliesaz.lexorcist.service.GoogleApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AllegationsRepositoryImpl @Inject constructor(
    private val googleApiService: GoogleApiService,
    private val allegationProvider: AllegationProvider,
    @ApplicationContext private val context: Context
) : AllegationsRepository {
    override suspend fun getAllegations(caseId: String): List<Allegation> {
        val staticAllegations = allegationProvider.getAllAllegations(context)
        val dynamicAllegations = mutableListOf<Allegation>()

        val spreadsheetId = caseId
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
                Allegation(
                    id = "sheet_${spreadsheetId}_${index}",
                    text = row[2].toString(),
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