package com.hereliesaz.lexorcist.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object EvidenceRepository {
    private val _taggedEvidenceList = MutableStateFlow<List<TaggedEvidence>>(emptyList())
    val taggedEvidenceList: StateFlow<List<TaggedEvidence>> = _taggedEvidenceList

    fun setTaggedEvidence(list: List<TaggedEvidence>) {
        _taggedEvidenceList.value = list
    }
}
