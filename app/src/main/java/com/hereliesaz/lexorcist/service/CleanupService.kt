package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import androidx.tracing.trace
import com.hereliesaz.lexorcist.data.Evidence
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupService @Inject constructor(private val semanticService: SemanticService) {

    suspend fun findSimilarTextEvidence(evidenceList: List<Evidence>): List<List<Evidence>> = trace("findSimilarTextEvidence") {
        val groups = mutableListOf<MutableList<Evidence>>()
        val processedEvidence = mutableSetOf<Int>()

        for (i in evidenceList.indices) {
            if (processedEvidence.contains(evidenceList[i].id)) continue

            val group = mutableListOf(evidenceList[i])
            processedEvidence.add(evidenceList[i].id)

            for (j in i + 1 until evidenceList.size) {
                if (processedEvidence.contains(evidenceList[j].id)) continue

                val similarity = semanticService.calculateSimilarity(evidenceList[i].content, evidenceList[j].content)

                if (similarity > 0.95) {
                    group.add(evidenceList[j])
                    processedEvidence.add(evidenceList[j].id)
                }
            }
            if (group.size > 1) {
                groups.add(group)
            }
        }
        return groups
    }

    fun detectDuplicates(evidenceList: List<Evidence>): List<List<Evidence>> {
        return evidenceList
            .filter { it.fileHash != null }
            .groupBy { it.fileHash }
            .filter { it.value.size > 1 }
            .map { it.value }
    }

    fun detectImageSeries(evidenceList: List<Evidence>): List<List<Evidence>> {
        val imageEvidence = evidenceList.filter { it.type == "image" }
        val seriesCandidates = imageEvidence.groupBy {
            it.sourceDocument.replace(Regex("[_\\d]"), "")
        }.filter { it.value.size > 1 }

        return seriesCandidates.map { it.value }
    }
}
