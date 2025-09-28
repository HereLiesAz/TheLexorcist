package com.hereliesaz.lexorcist.data

data class Allegation(
    val id: Int = 0,
    val spreadsheetId: String,
    val text: String,
    val elements: List<AllegationElement> = emptyList()
)

data class AllegationElement(
    val name: String,
    val description: String
)

// Represents a single entry in the allegations_catalog.json
data class AllegationCatalogEntry(
    val id: String,
    val allegationName: String,
    val relevant_evidence: Map<String, List<String>>
)
