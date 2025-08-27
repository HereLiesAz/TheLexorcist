package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.TaggedEvidence
import kotlinx.coroutines.flow.StateFlow

class DataReviewViewModel : ViewModel() {

    val taggedEvidenceList: StateFlow<List<TaggedEvidence>> = EvidenceRepository.taggedEvidenceList

    fun updateRelevance(evidence: TaggedEvidence, relevance: Int) {
        val newList = taggedEvidenceList.value.map {
            if (it == evidence) {
                it.copy(relevance = relevance)
            } else {
                it
            }
        }
        EvidenceRepository.setTaggedEvidence(newList)
    }

    fun updateNotes(evidence: TaggedEvidence, notes: String) {
        val newList = taggedEvidenceList.value.map {
            if (it == evidence) {
                it.copy(notes = notes)
            } else {
                it
            }
        }
        EvidenceRepository.setTaggedEvidence(newList)
    }
}
