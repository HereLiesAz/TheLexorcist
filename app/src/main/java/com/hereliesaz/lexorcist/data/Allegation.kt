package com.hereliesaz.lexorcist.data

import com.google.gson.annotations.SerializedName

data class Allegation(
    val id: Int = 0,
    val spreadsheetId: String,
    val name: String,
    val elements: List<AllegationElement> = emptyList()
)

// Represents a single entry in the allegations_catalog.json
data class AllegationCatalogEntry(
    val id: String,
    val allegationName: String,
    @SerializedName("relevant_evidence")
    val relevantEvidence: Map<String, List<String>>
)