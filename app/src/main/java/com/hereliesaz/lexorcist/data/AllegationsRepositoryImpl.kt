package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.data.repository.LegalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AllegationsRepositoryImpl
@Inject
constructor(
    private val legalRepository: LegalRepository
) : AllegationsRepository {
    override suspend fun getAllegations(caseId: String): List<Allegation> {
        Log.d("AllegationsRepositoryImpl", "Fetching allegations for caseId: $caseId using LegalRepository")
        val masterAllegations = legalRepository.getMasterAllegations().first()
        Log.d("AllegationsRepositoryImpl", "Loaded ${masterAllegations.size} master allegations from repository.")

        return masterAllegations.map { masterAllegation ->
            val elements = mutableListOf<AllegationElement>()
            masterAllegation.relevantEvidence?.let { ev ->
                ev.physical?.takeIf { it.isNotEmpty() }?.let {
                    elements.add(AllegationElement(name = "Physical", description = it.joinToString(", ")))
                }
                ev.documentary?.takeIf { it.isNotEmpty() }?.let {
                    elements.add(AllegationElement(name = "Documentary", description = it.joinToString(", ")))
                }
                ev.testimonial?.takeIf { it.isNotEmpty() }?.let {
                    elements.add(AllegationElement(name = "Testimonial", description = it.joinToString(", ")))
                }
                ev.digital?.takeIf { it.isNotEmpty() }?.let {
                    elements.add(AllegationElement(name = "Digital", description = it.joinToString(", ")))
                }
                ev.demonstrative?.takeIf { it.isNotEmpty() }?.let {
                    elements.add(AllegationElement(name = "Demonstrative", description = it.joinToString(", ")))
                }
            }

            Allegation(
                id = masterAllegation.id.toIntOrNull() ?: 0,
                spreadsheetId = caseId,
                name = masterAllegation.name,
                elements = elements
            )
        }
    }
}