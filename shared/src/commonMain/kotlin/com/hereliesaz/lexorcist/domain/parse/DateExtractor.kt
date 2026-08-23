package com.hereliesaz.lexorcist.domain.parse

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

/**
 * Extracts dates from free text (OCR output, transcripts, message bodies).
 *
 * ## Why this was rewritten
 *
 * The Android predecessor (`utils/DataParser.kt`) declared each pattern as a
 * Kotlin raw string that *itself began and ended with a quote character*:
 *
 * ```
 * """"\b\d{4}-\d{2}-\d{2}\b"""".toRegex()
 * ```
 *
 * The outer `"""` pairs are the raw-string delimiters, so the compiled regex
 * was `"\b\d{4}-\d{2}-\d{2}\b"` -- it required a literal `"` immediately
 * before and after the date. Real scanned or OCR'd text does not wrap dates in
 * quotation marks, so every pattern matched nothing, on every input, always.
 *
 * That mattered far beyond this file. `OcrProcessingService` resolved a
 * document's date as `exifDate ?: parseDates(text).firstOrNull() ?: now()`.
 * Screenshots -- the app's headline input -- carry no EXIF `DateTimeOriginal`,
 * so the first term was null; this parser guaranteed the second was null too;
 * and the result was that every screenshot was stamped with the moment it
 * happened to be scanned. `TimelineScreen` then sorted by that field and
 * rendered it to the minute, presenting ingest order as the chronology of
 * events with nothing to indicate the date was invented.
 *
 * Two patterns were additionally broken a second way: written as raw strings
 * containing `\\b` and `\\s`, which in a raw string is a literal backslash
 * followed by `b`/`s`, not a word-boundary or whitespace escape.
 *
 * ## Behaviour
 *
 * [extract] returns every date it can find, in the order they appear, each
 * paired with the text that produced it so a caller can show its work. It
 * returns an empty list when it finds nothing -- callers must treat that as
 * "unknown" and must not substitute the current time.
 */
object DateExtractor {

    data class Match(
        val instant: Instant,
        /** The exact substring that was matched. */
        val text: String,
        val startIndex: Int,
    )

    private val monthsShort = listOf(
        "jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec",
    )

    private val monthsLong = listOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
    )

    // Note the absence of surrounding literal quotes, and the use of single
    // backslashes -- inside a raw string `\b` is already the two characters a
    // regex engine reads as a word boundary.
    private val isoDateTime = Regex("""\b(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?Z?\b""")
    private val isoDate = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
    private val numericMdY4 = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{4})\b""")
    private val numericMdY2 = Regex("""\b(\d{1,2})[-/](\d{1,2})[-/](\d{2})\b""")
    private val monthNameFirst =
        Regex("""\b([A-Za-z]{3,9})\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})\b""")
    private val dayFirstPattern =
        Regex("""\b(\d{1,2})(?:st|nd|rd|th)?\s+([A-Za-z]{3,9})\.?,?\s+(\d{4})\b""")

    /**
     * Finds every date in [text].
     *
     * @param timeZone zone used to anchor date-only matches to an instant.
     *   Date-only text carries no zone, so one has to be chosen; the caller's
     *   local zone is the sane default.
     * @param assumeDayFirst when true, ambiguous all-numeric dates like
     *   `03/04/2024` are read day-first (UK/EU) rather than month-first (US).
     */
    fun extract(
        text: String,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        assumeDayFirst: Boolean = false,
    ): List<Match> {
        if (text.isBlank()) return emptyList()

        val found = mutableListOf<Match>()
        val consumed = mutableListOf<IntRange>()

        fun claim(range: IntRange): Boolean {
            if (consumed.any { it.first <= range.last && range.first <= it.last }) return false
            consumed += range
            return true
        }

        fun add(m: MatchResult, instant: Instant?) {
            if (instant != null && claim(m.range)) {
                found += Match(instant, m.value, m.range.first)
            }
        }

        // Most specific first, so a full timestamp is not shadowed by the
        // date-only pattern nested inside it.
        for (m in isoDateTime.findAll(text)) {
            val g = m.groupValues
            add(m, instantOf(g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4].toInt(), g[5].toInt(), g[6].toInt(), timeZone))
        }
        for (m in isoDate.findAll(text)) {
            val g = m.groupValues
            add(m, dateOf(g[1].toInt(), g[2].toInt(), g[3].toInt(), timeZone))
        }
        for (m in monthNameFirst.findAll(text)) {
            val g = m.groupValues
            val month = monthNumber(g[1]) ?: continue
            add(m, dateOf(g[3].toInt(), month, g[2].toInt(), timeZone))
        }
        for (m in dayFirstPattern.findAll(text)) {
            val g = m.groupValues
            val month = monthNumber(g[2]) ?: continue
            add(m, dateOf(g[3].toInt(), month, g[1].toInt(), timeZone))
        }
        for (m in numericMdY4.findAll(text)) {
            val g = m.groupValues
            val (month, day) = disambiguate(g[1].toInt(), g[2].toInt(), assumeDayFirst) ?: continue
            add(m, dateOf(g[3].toInt(), month, day, timeZone))
        }
        for (m in numericMdY2.findAll(text)) {
            val g = m.groupValues
            val (month, day) = disambiguate(g[1].toInt(), g[2].toInt(), assumeDayFirst) ?: continue
            add(m, dateOf(expandTwoDigitYear(g[3].toInt()), month, day, timeZone))
        }

        return found.sortedBy { it.startIndex }
    }

    /** The first date in [text], or null when there is none. Never guesses. */
    fun extractFirst(
        text: String,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        assumeDayFirst: Boolean = false,
    ): Instant? = extract(text, timeZone, assumeDayFirst).firstOrNull()?.instant

    private fun disambiguate(a: Int, b: Int, dayFirst: Boolean): Pair<Int, Int>? {
        // If one component cannot be a month, the ordering is unambiguous
        // regardless of locale convention.
        return when {
            a > 12 && b in 1..12 -> b to a
            b > 12 && a in 1..12 -> a to b
            a in 1..12 && b in 1..31 -> if (dayFirst) b to a else a to b
            else -> null
        }
    }

    /**
     * Two-digit years are read into a 100-year window ending 20 years from
     * the 2000s baseline: 00-49 -> 2000s, 50-99 -> 1900s.
     */
    private fun expandTwoDigitYear(y: Int): Int = if (y < 50) 2000 + y else 1900 + y

    private fun monthNumber(name: String): Int? {
        val n = name.lowercase().trimEnd('.')
        monthsLong.indexOf(n).let { if (it >= 0) return it + 1 }
        monthsShort.indexOf(n.take(3)).let { if (it >= 0 && n.length <= 4) return it + 1 }
        // Long-form prefixes such as "Sept"
        monthsLong.indexOfFirst { it.startsWith(n) && n.length >= 3 }.let { if (it >= 0) return it + 1 }
        return null
    }

    private fun dateOf(year: Int, month: Int, day: Int, tz: TimeZone): Instant? =
        runCatching { LocalDate(year, month, day).atStartOfDayIn(tz) }.getOrNull()

    private fun instantOf(
        year: Int, month: Int, day: Int,
        hour: Int, minute: Int, second: Int,
        tz: TimeZone,
    ): Instant? = runCatching {
        LocalDateTime(year, month, day, hour, minute, second).toInstant(tz)
    }.getOrNull()
}
