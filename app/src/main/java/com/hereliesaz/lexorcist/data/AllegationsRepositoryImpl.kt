package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.service.GoogleApiService // Added import
import javax.inject.Inject
// Removed: import com.hereliesaz.lexorcist.auth.CredentialHolder

class AllegationsRepositoryImpl
    @Inject
    constructor(
        private val googleApiService: GoogleApiService, // Changed to inject GoogleApiService directly
    ) : AllegationsRepository {
        override suspend fun getAllegations(caseId: String): List<Allegation> {
            // Use the injected googleApiService directly
            // The null check for googleApiService is removed as it's now directly injected.
            // If GoogleApiService cannot operate (e.g., no credentials), its methods should handle that.

            // TODO: The spreadsheetId should not be hardcoded. It should likely come from the caseId or a Case object.
            val spreadsheetId = "1TN9MLnzpCJjcO9bwEhTeOjon3mRunYs5_tSxII6LizA" // Example, ensure this is correct
            Log.d("AllegationsRepository", "Fetching allegations from spreadsheet: $spreadsheetId for case: $caseId")

            val sheetData = googleApiService.readSpreadsheet(spreadsheetId) // Explicit type argument removed
            if (sheetData.isNullOrEmpty()) {
                Log.w("AllegationsRepository", "Sheet data is null or empty for spreadsheet: $spreadsheetId")
                return emptyList()
            }
            // Assuming allegations are in the first sheet, or a specifically named sheet.
            // This logic might need to be more robust based on actual spreadsheet structure.
            val allegationsSheet = sheetData["Allegations"] // Or the correct sheet name
            if (allegationsSheet.isNullOrEmpty()) {
                 Log.w("AllegationsRepository", "'Allegations' sheet is null or empty, or not found in spreadsheet: $spreadsheetId")
                 // Fallback to trying the first sheet if "Allegations" is not found, for compatibility with previous logic
                 val firstSheet = sheetData.values.firstOrNull()
                 if (firstSheet.isNullOrEmpty()) {
                    Log.w("AllegationsRepository", "No sheets with data found in spreadsheet: $spreadsheetId")
                    return emptyList()
                 }
                return firstSheet.mapIndexedNotNull { index, row ->
                    if (row.size >= 3) { // Assuming text is in the 3rd column (index 2)
                        Allegation(
                            id = index, // This might need a more robust ID generation
                            spreadsheetId = spreadsheetId,
                            text = row[2].toString(),
                        )
                    } else {
                        Log.w("AllegationsRepository", "Skipping row due to insufficient columns: $row")
                        null
                    }
                }
            }

            return allegationsSheet.mapIndexedNotNull { index, row ->
                if (row.size >= 3) { // Assuming text is in the 3rd column (index 2)
                    Allegation(
                        id = index, // This might need a more robust ID generation
                        spreadsheetId = spreadsheetId,
                        text = row[2].toString(),
                    )
                } else {
                    Log.w("AllegationsRepository", "Skipping row in 'Allegations' sheet due to insufficient columns: $row")
                    null
                }
            }
        }
    }
