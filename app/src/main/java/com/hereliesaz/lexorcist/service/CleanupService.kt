package com.hereliesaz.lexorcist.service

import androidx.tracing.trace
import com.hereliesaz.lexorcist.data.Evidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupService @Inject constructor(private val semanticService: SemanticService) {

    suspend fun findSimilarTextEvidence(evidenceList: List<Evidence>): List<List<Evidence>> = withContext(Dispatchers.Default) {
        trace("findSimilarTextEvidence") {
            val embeddings = evidenceList.map { evidence ->
                semanticService.getEmbedding(evidence.content)
            }

            val groups = mutableListOf<MutableList<Evidence>>()
            val processedEvidence = mutableSetOf<Int>()

            for (i in evidenceList.indices) {
                if (processedEvidence.contains(evidenceList[i].id) || embeddings[i] == null) continue

                val group = mutableListOf(evidenceList[i])
                processedEvidence.add(evidenceList[i].id)

                for (j in i + 1 until evidenceList.size) {
                    if (processedEvidence.contains(evidenceList[j].id) || embeddings[j] == null) continue

                    val similarity = com.google.mediapipe.tasks.text.textembedder.TextEmbedder.cosineSimilarity(embeddings[i]!!, embeddings[j]!!)

                    if (similarity > 0.95) {
                        group.add(evidenceList[j])
                        processedEvidence.add(evidenceList[j].id)
                    }
                }
                if (group.size > 1) {
                    groups.add(group)
                }
            }
            groups
        }
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