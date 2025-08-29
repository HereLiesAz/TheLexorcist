package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import com.hereliesaz.lexorcist.model.TaggedEvidence // Added import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update // Added import

class DataReviewViewModel : ViewModel() {
    // Original reviewedText, keeping it as it might be used elsewhere
    private val _reviewedText = MutableStateFlow("")
    val reviewedText: StateFlow<String> = _reviewedText.asStateFlow()

    fun onTextChange(newText: String) {
        _reviewedText.value = newText
    }

    // New properties and methods for DataReviewScreen
    private val _taggedEvidenceList = MutableStateFlow<List<TaggedEvidence>>(emptyList())
    val taggedEvidenceList: StateFlow<List<TaggedEvidence>> = _taggedEvidenceList.asStateFlow()

    // Example function to populate the list (you'll need to adapt this to your data source)
    fun loadSampleTaggedEvidence() {
        // This is just sample data. Replace with your actual data loading logic.
        // import com.hereliesaz.lexorcist.model.Evidence // Ensure Evidence is imported if used directly here
        // val sampleEvidence1 = com.hereliesaz.lexorcist.model.Evidence(content = "Sample Evidence 1 Content", timestamp = System.currentTimeMillis(), sourceDocument = "Doc1", documentDate = System.currentTimeMillis(), tags = listOf("tag1", "sample"), id = 1)
        // val sampleEvidence2 = com.hereliesaz.lexorcist.model.Evidence(content = "Sample Evidence 2 Content", timestamp = System.currentTimeMillis(), sourceDocument = "Doc2", documentDate = System.currentTimeMillis(), tags = listOf("tag2", "sample"), id = 2)
        // _taggedEvidenceList.value = listOf(
        // TaggedEvidence(id = sampleEvidence1, tags = sampleEvidence1.tags, content = sampleEvidence1.content, relevance = 3, notes = "Initial notes for E1"),
        // TaggedEvidence(id = sampleEvidence2, tags = sampleEvidence2.tags, content = sampleEvidence2.content, relevance = 7, notes = "Initial notes for E2")
        // )
    }

    fun updateRelevance(evidenceToUpdate: TaggedEvidence, newRelevance: Int) {
        _taggedEvidenceList.update { currentList ->
            currentList.map { taggedEvidence ->
                // Assuming TaggedEvidence.id is the Evidence object, and Evidence.id is the unique Int identifier
                if (taggedEvidence.id.id == evidenceToUpdate.id.id) {
                    taggedEvidence.copy(relevance = newRelevance)
                } else {
                    taggedEvidence
                }
            }
        }
    }

    fun updateNotes(evidenceToUpdate: TaggedEvidence, newNotes: String) {
        _taggedEvidenceList.update { currentList ->
            currentList.map { taggedEvidence ->
                if (taggedEvidence.id.id == evidenceToUpdate.id.id) {
                    taggedEvidence.copy(notes = newNotes)
                } else {
                    taggedEvidence
                }
            }
        }
    }
}
