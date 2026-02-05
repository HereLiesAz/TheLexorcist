package com.hereliesaz.lexorcist.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.repository.LegalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Implementation of [AllegationsRepository].
 *
 * This repository acts as an adapter, transforming the low-level [MasterAllegation] data entities
 * fetched from the [LegalRepository] into the domain-layer [Allegation] objects used by the UI.
 *
 * It handles JSON parsing of the `relevantEvidence` field, which stores allegation elements in a serialized format.
 */
class AllegationsRepositoryImpl @Inject constructor(
    private val legalRepository: LegalRepository,
    private val gson: Gson
) : AllegationsRepository {

    /**
     * Retrieves the list of allegations for a specific case.
     *
     * @param caseId The identifier of the case (currently used as spreadsheetId context, though locally logic maps from master data).
     * @return A list of [Allegation] objects with parsed elements.
     */
    override suspend fun getAllegations(caseId: String): List<Allegation> {
        // Fetch the raw master allegations from the local database.
        val masterAllegations = legalRepository.getMasterAllegations().first()

        return masterAllegations.mapNotNull { master ->
            // Parse the 'relevantEvidence' JSON string into a structured list of AllegationElements.
            val elements = try {
                if (master.relevantEvidence.isNullOrBlank() || master.relevantEvidence == "{}") {
                    emptyList()
                } else {
                    // JSON format expected: {"ElementName": ["Description1", "Description2"], ...}
                    val type = object : TypeToken<Map<String, List<String>>>() {}.type
                    val evidenceMap: Map<String, List<String>> = gson.fromJson(master.relevantEvidence, type)

                    evidenceMap.map { (elementName, elementDescriptions) ->
                        AllegationElement(name = elementName, description = elementDescriptions.joinToString(", "))
                    }
                }
            } catch (e: Exception) {
                // Fail gracefully on malformed JSON, returning an empty list of elements.
                e.printStackTrace()
                emptyList()
            }

            // Map valid master allegations to domain objects.
            master.id?.let {
                Allegation(
                    id = it.toIntOrNull() ?: 0,
                    spreadsheetId = caseId,
                    name = master.name,
                    elements = elements
                )
            }
        }
    }
}
