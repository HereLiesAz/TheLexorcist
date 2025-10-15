package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupService @Inject constructor() {

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
