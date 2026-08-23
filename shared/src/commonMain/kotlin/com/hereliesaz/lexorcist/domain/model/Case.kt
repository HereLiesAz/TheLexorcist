package com.hereliesaz.lexorcist.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Case(
    val id: CaseId,
    val name: String,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val plaintiffs: String = "",
    val defendants: String = "",
    val court: String = "",
    val caseNumber: String = "",
    val jurisdiction: String = "",
    val isArchived: Boolean = false,
)

@Serializable
data class Exhibit(
    val id: ExhibitId,
    val caseId: CaseId,
    val name: String,
    val description: String = "",
    val evidenceIds: List<EvidenceId> = emptyList(),
    val orderIndex: Int = 0,
)

@Serializable
@JvmInline
value class ExhibitId(val value: String)

@Serializable
data class Allegation(
    val id: AllegationId,
    val caseId: CaseId,
    val name: String,
    val description: String = "",
    val category: String = "",
    val elements: List<LegalElement> = emptyList(),
)

/**
 * A required element of a legal claim -- the thing a given exhibit is meant to
 * prove. The Android predecessor declared this type and then backed it with a
 * repository whose only implementation returned `emptyList()`, so no screen
 * ever had elements to show.
 */
@Serializable
data class LegalElement(
    val name: String,
    val description: String = "",
    /** Evidence the user has nominated as proving this element. */
    val supportingEvidenceIds: List<EvidenceId> = emptyList(),
)

@Serializable
data class Script(
    val id: ScriptId,
    val name: String,
    val description: String = "",
    val source: String,
    val authorName: String = "",
    val authorEmail: String = "",
    /**
     * SHA-256 of [source]. Recorded on every tag this script applies so a
     * tagging decision stays attributable after the script is edited.
     */
    val contentHash: String,
    val isEnabled: Boolean = false,
)

@Serializable
@JvmInline
value class ScriptId(val value: String)

@Serializable
data class DocumentTemplate(
    val id: TemplateId,
    val name: String,
    val description: String = "",
    val body: String,
    val format: Format = Format.Html,
) {
    @Serializable
    enum class Format { Html, Markdown }
}

@Serializable
@JvmInline
value class TemplateId(val value: String)

enum class SortOrder {
    NameAsc,
    NameDesc,
    DateCreatedAsc,
    DateCreatedDesc,
    DateModifiedAsc,
    DateModifiedDesc,
}

/**
 * Sorts cases. The Android predecessor's `DATE_ASC`/`DATE_DESC` branches
 * sorted by `Case.id`, which on the live read path was
 * `spreadsheetId.hashCode()` -- a stable but date-unrelated scramble.
 */
fun List<Case>.sortedBy(order: SortOrder): List<Case> = when (order) {
    SortOrder.NameAsc -> sortedBy { it.name.lowercase() }
    SortOrder.NameDesc -> sortedByDescending { it.name.lowercase() }
    SortOrder.DateCreatedAsc -> sortedBy { it.createdAt }
    SortOrder.DateCreatedDesc -> sortedByDescending { it.createdAt }
    SortOrder.DateModifiedAsc -> sortedBy { it.lastModifiedAt }
    SortOrder.DateModifiedDesc -> sortedByDescending { it.lastModifiedAt }
}
