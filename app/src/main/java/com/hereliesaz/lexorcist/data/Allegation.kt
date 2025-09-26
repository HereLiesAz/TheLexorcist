package com.hereliesaz.lexorcist.data

data class Allegation(
    val id: Int = 0,
    val spreadsheetId: String,
    val text: String,
    val allegationElementName: String,
    val elements: List<AllegationElement> = emptyList()
)
