package com.hereliesaz.lexorcist.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.repository.LegalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AllegationsRepositoryImpl @Inject constructor(
    private val legalRepository: LegalRepository,
    private val gson: Gson
) : AllegationsRepository {
    override suspend fun getAllegations(caseId: String): List<Allegation> {
        val masterAllegations = legalRepository.getMasterAllegations().first()
        return masterAllegations.mapNotNull { master ->
            val elements = try {
                if (master.relevantEvidence.isNullOrBlank() || master.relevantEvidence == "{}") {
                    emptyList()
                } else {
                    val type = object : TypeToken<Map<String, List<String>>>() {}.type
                    val evidenceMap: Map<String, List<String>> = gson.fromJson(master.relevantEvidence, type)
                    evidenceMap.map { (elementName, elementDescriptions) ->
                        AllegationElement(name = elementName, description = elementDescriptions.joinToString(", "))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

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