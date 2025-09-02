package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.TaggedEvidence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A singleton repository for managing tagged evidence in memory.
 *
 * This object provides a centralized place to store and access a list of [TaggedEvidence].
 * It uses a [StateFlow] to allow observers to react to changes in the evidence list.
 */
object TaggedEvidenceRepository {
    private val _taggedEvidenceList = MutableStateFlow<List<TaggedEvidence>>(emptyList())

    /**
     * A [StateFlow] that emits the current list of tagged evidence.
     *
     * Observers can collect this flow to be notified of updates to the evidence list.
     */
    val taggedEvidenceList: StateFlow<List<TaggedEvidence>> = _taggedEvidenceList

    /**
     * Sets the list of tagged evidence, overwriting the existing list.
     *
     * @param list The new list of [TaggedEvidence] to store in the repository.
     */
    fun setTaggedEvidence(list: List<TaggedEvidence>) {
        _taggedEvidenceList.value = list
    }
}
