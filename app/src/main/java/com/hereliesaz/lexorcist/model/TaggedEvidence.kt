package com.hereliesaz.lexorcist.model

/**
 * Represents a piece of evidence that has been tagged and annotated.
 *
 * This data class combines an [Evidence] object with additional metadata, such as tags,
 * relevance score, and notes.
 *
 * @property id The underlying [Evidence] object.
 * @property tags A list of tags associated with the evidence.
 * @property content The textual content of the evidence. This may be redundant if
 * the [Evidence] object also contains the content.
 * @property relevance A score indicating the relevance of the evidence.
 * @property notes Additional notes or comments about the evidence.
 */
data class TaggedEvidence(
    val id: Evidence,
    val tags: List<String>,
    val content: String,
    val relevance: Int = 0,
    val notes: String = ""
)
