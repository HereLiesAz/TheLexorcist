package com.hereliesaz.lexorcist.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * A piece of evidence in a case.
 *
 * Three things changed relative to the Android-only `data.Evidence` this
 * replaces, all of them driven by defects found in that model:
 *
 *  1. `spreadsheetId` is gone. Storage identity does not belong in a domain
 *     entity; a case is addressed by [caseId].
 *  2. `isSelected` is gone. That was UI state living in the persisted model.
 *  3. Provenance is explicit and first-class ([provenance]) instead of a
 *     nullable `fileHash` that only two of the seven ingest paths ever set.
 *
 * [documentDate] is nullable on purpose. The predecessor defaulted it to
 * `System.currentTimeMillis()` whenever EXIF and text extraction both came up
 * empty, so the timeline silently rendered ingest time as occurrence time with
 * no way to tell the difference. A null here means "we do not know when this
 * happened", and the UI is required to say so rather than invent a date.
 */
@Serializable
data class Evidence(
    val id: EvidenceId,
    val caseId: CaseId,
    val kind: EvidenceKind,
    /** Text extracted from the source, if any (OCR output, transcript, message body). */
    val content: String,
    /** Presentation-ready rendering of [content], if the pipeline produced one. */
    val formattedContent: String? = null,
    /** Location of the immutable original, relative to the case's raw store. */
    val sourceRef: String? = null,
    /** When this record was created by the app. Always known. */
    val ingestedAt: Instant,
    /**
     * When the underlying event actually happened, if it could be established.
     * Null means unknown -- never substitute [ingestedAt].
     */
    val documentDate: Instant? = null,
    /** How [documentDate] was established. Recorded so the UI can qualify it. */
    val documentDateSource: DateSource = DateSource.Unknown,
    val sourceDocument: String = "",
    val allegationId: AllegationId? = null,
    val allegationElementName: String? = null,
    val category: String = "",
    val tags: List<Tag> = emptyList(),
    val commentary: String? = null,
    val linkedEvidenceIds: List<EvidenceId> = emptyList(),
    val parentEvidenceId: EvidenceId? = null,
    val entities: Map<String, List<String>> = emptyMap(),
    val transcript: Transcript? = null,
    val durationMillis: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
    val provenance: Provenance,
    val duplicateOf: EvidenceId? = null,
) {
    val isDuplicate: Boolean get() = duplicateOf != null

    /** True when [documentDate] reflects a real, sourced date rather than an absence. */
    val hasReliableDate: Boolean
        get() = documentDate != null && documentDateSource != DateSource.Unknown
}

@Serializable
@JvmInline
value class EvidenceId(val value: String)

@Serializable
@JvmInline
value class CaseId(val value: String)

@Serializable
@JvmInline
value class AllegationId(val value: String)

@Serializable
enum class EvidenceKind {
    Image,
    Audio,
    Video,
    Document,
    TextNote,
    SmsMessage,
    CallLogEntry,
    EmailMessage,
    ChatMessage,
    LocationFix,
}

/**
 * Where a [Evidence.documentDate] came from.
 *
 * This exists because the old pipeline could not distinguish "the photo's EXIF
 * says 3 March" from "we gave up and stamped it with the clock", yet displayed
 * both to the minute in the timeline.
 */
@Serializable
enum class DateSource {
    /** Read from embedded file metadata (EXIF, media container, message header). */
    FileMetadata,

    /** Parsed out of the extracted text. */
    ExtractedText,

    /** Supplied by the source system (SMS/call log/mail timestamps). */
    SourceSystem,

    /** Entered by the user. */
    UserProvided,

    /** Not established. The UI must not present a date. */
    Unknown,
}

/**
 * Tamper-evidence and reproducibility metadata.
 *
 * Every ingest path is required to produce one. [contentHash] is a SHA-256 of
 * the original bytes as stored, taken before any processing; [derivations]
 * records what produced the fields on the [Evidence] so a tagging decision can
 * be re-executed and audited later.
 */
@Serializable
data class Provenance(
    /** SHA-256, lowercase hex, of the original source bytes. */
    val contentHash: String,
    val byteSize: Long,
    val importedAt: Instant,
    /** Free-text description of where this came from, e.g. "Camera", "Google Takeout". */
    val importSource: String,
    val derivations: List<Derivation> = emptyList(),
)

/**
 * A record that some tool produced part of this evidence's data.
 *
 * [toolVersion] and [inputHash] together make a derivation reproducible;
 * without them there is no way to answer "which exact code tagged this?"
 * once a user script has been edited.
 */
@Serializable
data class Derivation(
    val kind: Kind,
    val tool: String,
    val toolVersion: String,
    /** Hash of the exact input this derivation consumed. */
    val inputHash: String,
    val producedAt: Instant,
    val notes: String? = null,
) {
    @Serializable
    enum class Kind { Ocr, Transcription, Tagging, Redaction, Conversion, Enrichment }
}

/** A tag, plus the record of what applied it. */
@Serializable
data class Tag(
    val label: String,
    val appliedBy: AppliedBy = AppliedBy.User,
    /**
     * For [AppliedBy.Script], the SHA-256 of the script source that was
     * actually executed. Scripts are edited in place under a stable id, so the
     * id alone does not identify the code that ran.
     */
    val scriptContentHash: String? = null,
    val appliedAt: Instant? = null,
) {
    @Serializable
    enum class AppliedBy { User, Script, Import }
}

@Serializable
data class Transcript(
    val text: String,
    val language: String? = null,
    val engine: String? = null,
    val edits: List<TranscriptEdit> = emptyList(),
) {
    /** The transcript as first produced, before any user edit. */
    val original: String get() = edits.firstOrNull()?.previousText ?: text
}

@Serializable
data class TranscriptEdit(
    val editedAt: Instant,
    val reason: String,
    val previousText: String,
    val newText: String,
)
