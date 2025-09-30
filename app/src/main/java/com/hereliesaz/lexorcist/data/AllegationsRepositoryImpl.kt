package com.hereliesaz.lexorcist.data

import android.util.Log
// import com.hereliesaz.lexorcist.service.GoogleApiService // Not used in this file
import javax.inject.Inject

class AllegationsRepositoryImpl
@Inject
constructor(
    private val allegationProvider: AllegationProvider
) : AllegationsRepository {
    override suspend fun getAllegations(caseId: String): List<Allegation> {
        Log.d("AllegationsRepositoryImpl", "Fetching allegations for caseId: $caseId using AllegationProvider instance: $allegationProvider")
        val catalogEntries = allegationProvider.getAllLoadedCatalogEntries()
        Log.d("AllegationsRepositoryImpl", "Loaded ${catalogEntries.size} catalog entries from provider.")
        return catalogEntries.map { catalogEntry ->
            val elements = catalogEntry.relevantEvidence.map { (elementName, elementDescriptions) ->
                AllegationElement(name = elementName, description = elementDescriptions.joinToString(", "))
            }
            Allegation(
                id = catalogEntry.id.toIntOrNull() ?: 0, // Or handle parse error more gracefully
                spreadsheetId = caseId, // Use the passed caseId for context
                name = catalogEntry.allegationName, // Changed from catalogEntry.name
                elements = elements
            )
        }
    }
}