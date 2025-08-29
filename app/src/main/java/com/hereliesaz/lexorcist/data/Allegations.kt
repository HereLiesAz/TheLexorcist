package com.hereliesaz.lexorcist.data

/**
 * Represents a single allegation within a legal case.
 *
 * This data class stores the details of an allegation, including its text and
 * the case it belongs to.
 *
 * @property id The unique identifier for the allegation.
 * @property caseId The ID of the case this allegation belongs to.
 * @property text The text of the allegation.
 */
data class Allegation(
    val id: Int = 0,
    val caseId: Int,
    val text: String
)
