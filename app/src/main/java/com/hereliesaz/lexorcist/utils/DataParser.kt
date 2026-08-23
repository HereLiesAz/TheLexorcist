package com.hereliesaz.lexorcist.utils

import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.domain.parse.DateExtractor
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Entity extraction over OCR output, transcripts and imported message bodies.
 *
 * ## What was wrong here
 *
 * Every regex in the previous version of this file was a Kotlin raw string
 * that itself began and ended with a quote character:
 *
 * ```
 * """"\b\d{4}-\d{2}-\d{2}\b"""".toRegex()
 * ```
 *
 * The outer `"""` pairs are the raw-string delimiters, so the compiled pattern
 * was `"\b\d{4}-\d{2}-\d{2}\b"` -- requiring a literal `"` immediately before
 * and after the date. Real scanned text does not put dates, names or addresses
 * inside quotation marks, so `parseDates`, `parseNames`, `parseAddresses` and
 * `extractAllegations` returned empty on every realistic input.
 *
 * Several patterns were broken a second way: written as raw strings containing
 * `\\b` and `\\s`, which inside a raw string is a literal backslash followed by
 * `b`/`s`, not an escape. And `extractEvidence` split its input on `"\\n"` --
 * the two-character sequence backslash-n -- rather than a newline, so it
 * treated an entire document as a single sentence.
 *
 * The date failure was the consequential one. `OcrProcessingService` resolved a
 * document's date as `exif ?: parseDates(text).firstOrNull() ?: now()`, and
 * screenshots carry no EXIF `DateTimeOriginal`, so evidence was stamped with
 * the moment it was scanned and the timeline rendered that as the occurrence
 * time.
 *
 * Date handling now delegates to [DateExtractor] in the shared multiplatform
 * module, which is covered by tests. The functions here that can legitimately
 * find nothing return empty lists, and callers must treat that as "unknown"
 * rather than substituting the current time.
 */
object DataParser {

    /**
     * Dates found in [text], as epoch milliseconds, in the order they appear.
     *
     * Empty when the text contains no date. Callers must not fall back to the
     * current time; record the date as unknown instead.
     */
    fun parseDates(text: String): List<Long> =
        DateExtractor.extract(text).map { it.instant.toEpochMilliseconds() }

    /** The first date in [text], or null. */
    fun parseFirstDate(text: String): Long? =
        DateExtractor.extractFirst(text)?.toEpochMilliseconds()

    // Two capitalised words in sequence. A blunt heuristic, but it now runs at
    // all -- and it deliberately excludes single words to keep the false
    // positive rate on OCR noise down.
    private val nameRegex = Regex("""\b\p{Lu}\p{Ll}+(?:\s+\p{Lu}\p{Ll}+)+\b""")

    // Street number, street, then either "CITY, ST 12345" or "ST 12345".
    private val addressRegex =
        Regex("""\b\d{1,6}\s+[\w.'-]+(?:\s+[\w.'-]+){0,5},?\s+(?:[A-Za-z.'-]+,?\s+)?[A-Z]{2}\s+\d{5}(?:-\d{4})?\b""")

    private val timeRegex =
        Regex("""\b(\d{1,2}):(\d{2})(?::(\d{2}))?\s*([AaPp][Mm])?\b""")

    fun parseNames(text: String): List<String> =
        nameRegex.findAll(text).map { it.value }.distinct().toList()

    fun parseAddresses(text: String): List<String> =
        addressRegex.findAll(text).map { it.value.trim() }.distinct().toList()

    /** Times of day found in [text], normalised to `HH:mm:ss`. */
    fun parseTimestamps(text: String): List<String> =
        timeRegex.findAll(text).mapNotNull { m ->
            val hour12Or24 = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val minute = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val second = m.groupValues[3].toIntOrNull() ?: 0
            val meridiem = m.groupValues[4].lowercase()

            if (minute !in 0..59 || second !in 0..59) return@mapNotNull null

            val hour = when {
                meridiem.startsWith("p") && hour12Or24 in 1..11 -> hour12Or24 + 12
                meridiem.startsWith("a") && hour12Or24 == 12 -> 0
                else -> hour12Or24
            }
            if (hour !in 0..23) return@mapNotNull null

            "%02d:%02d:%02d".format(hour, minute, second)
        }.distinct().toList()

    /** Named entities found in [text], keyed by kind. */
    fun tagData(text: String): Map<String, List<String>> {
        val utc = TimeZone.UTC
        val dates = DateExtractor.extract(text).map { it.instant.asIsoDate(utc) }
        return mapOf(
            "dates" to dates,
            "names" to parseNames(text),
            "addresses" to parseAddresses(text),
            "timestamps" to parseTimestamps(text),
        )
    }

    private fun Instant.asIsoDate(tz: TimeZone): String {
        val d = toLocalDateTime(tz).date
        return "%04d-%02d-%02d".format(d.year, d.monthNumber, d.day)
    }

    private val allegationRegex =
        Regex("""(?i)\b(?:alleges|claims|argues|asserts|contends)\s+(?:that\s+)?(.{0,300})""")

    private val categoryRegex = Regex("""(?i)\bCategory:\s*(\w+)""")

    fun parseTextForCase(
        spreadsheetId: String,
        text: String,
        caseId: Int,
    ): CaseData {
        val allegations = extractAllegations(spreadsheetId, text)
        val evidence = extractEvidence(spreadsheetId, text, allegations, caseId)
        return CaseData(allegations, evidence)
    }

    private fun extractAllegations(
        spreadsheetId: String,
        text: String,
    ): List<Allegation> {
        var currentId = 0
        return allegationRegex.findAll(text).map {
            Allegation(id = currentId++, spreadsheetId = spreadsheetId, name = it.value.trim())
        }.toList()
    }

    /**
     * Splits [text] into lines and turns each into an [Evidence] row.
     *
     * `documentDate` is `0L` when the line carries no date. That sentinel means
     * "not established" -- the previous version substituted
     * `System.currentTimeMillis()` here, which is how ingest time ended up
     * presented as occurrence time.
     */
    fun extractEvidence(
        spreadsheetId: String,
        text: String,
        allegations: List<Allegation>,
        caseId: Int,
    ): List<Evidence> {
        val entries = mutableListOf<Evidence>()
        // Splitting on the string "\\n" -- a literal backslash followed by 'n'
        // -- meant this loop only ever saw one giant "sentence".
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

        for (line in lines) {
            val date = parseFirstDate(line) ?: 0L
            val linkedAllegation =
                allegations.find { line.contains(it.name, ignoreCase = true) }
            val category = categoryRegex.find(line)?.groupValues?.get(1).orEmpty()

            entries.add(
                Evidence(
                    id = entries.size,
                    spreadsheetId = spreadsheetId,
                    caseId = caseId.toLong(),
                    allegationId = linkedAllegation?.spreadsheetId,
                    allegationElementName = null,
                    content = line,
                    formattedContent = null,
                    mediaUri = null,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = "Parsed from text",
                    documentDate = date,
                    category = category,
                    tags = emptyList(),
                    type = "text",
                ),
            )
        }
        return entries
    }

    data class CaseData(
        val allegations: List<Allegation>,
        val evidence: List<Evidence>,
    )
}
