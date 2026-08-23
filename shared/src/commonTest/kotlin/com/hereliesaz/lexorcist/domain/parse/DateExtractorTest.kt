package com.hereliesaz.lexorcist.domain.parse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Regression tests for the date-extraction bug that silently falsified every
 * timeline in the Android app.
 *
 * The predecessor's patterns were raw strings that began and ended with a
 * literal quote character, so they only matched dates that appeared inside
 * quotation marks in the source text -- which, in OCR output, never happens.
 * [unquotedDatesAreFound] is the test that would have caught it;
 * [quotedDatesAlsoWork] pins the behaviour the broken version accidentally had,
 * so a future "fix" cannot regress in the other direction.
 */
class DateExtractorTest {

    private val utc = TimeZone.UTC

    private fun ymd(text: String, dayFirst: Boolean = false): Triple<Int, Int, Int>? {
        val i = DateExtractor.extractFirst(text, utc, dayFirst) ?: return null
        val d = i.toLocalDateTime(utc).date
        return Triple(d.year, d.monthNumber, d.day)
    }

    @Test
    fun unquotedDatesAreFound() {
        // The exact shape the old regex could not match.
        assertEquals(Triple(2024, 1, 15), ymd("Incident occurred on 2024-01-15 at the residence"))
    }

    @Test
    fun quotedDatesAlsoWork() {
        assertEquals(Triple(2024, 1, 15), ymd("""the report says "2024-01-15" plainly"""))
    }

    @Test
    fun isoTimestampKeepsTimeOfDay() {
        val i = DateExtractor.extractFirst("logged at 2024-03-09T14:32:07Z", utc)!!
        val dt = i.toLocalDateTime(utc)
        assertEquals(14, dt.hour)
        assertEquals(32, dt.minute)
        assertEquals(7, dt.second)
    }

    @Test
    fun slashDatesDefaultToMonthFirst() {
        assertEquals(Triple(2024, 3, 4), ymd("dated 03/04/2024"))
    }

    @Test
    fun slashDatesHonourDayFirstWhenAsked() {
        assertEquals(Triple(2024, 4, 3), ymd("dated 03/04/2024", dayFirst = true))
    }

    @Test
    fun unambiguousNumericDatesIgnoreTheLocaleHint() {
        // 25 cannot be a month, so ordering is determined regardless of the hint.
        assertEquals(Triple(2024, 6, 25), ymd("25/06/2024"))
        assertEquals(Triple(2024, 6, 25), ymd("25/06/2024", dayFirst = true))
    }

    @Test
    fun shortMonthNames() {
        assertEquals(Triple(2023, 11, 2), ymd("served Nov 2, 2023"))
    }

    @Test
    fun longMonthNames() {
        assertEquals(Triple(2023, 9, 30), ymd("on September 30, 2023 the messages stopped"))
    }

    @Test
    fun dayFirstWrittenDates() {
        assertEquals(Triple(2022, 2, 14), ymd("14 February 2022"))
    }

    @Test
    fun ordinalSuffixesAreTolerated() {
        assertEquals(Triple(2022, 2, 14), ymd("14th February 2022"))
    }

    @Test
    fun twoDigitYearsExpandIntoAWindow() {
        assertEquals(Triple(2004, 5, 6), ymd("05/06/04"))
        assertEquals(Triple(1998, 5, 6), ymd("05/06/98"))
    }

    @Test
    fun impossibleDatesAreRejectedRatherThanClamped() {
        assertNull(DateExtractor.extractFirst("2024-02-31", utc))
        assertNull(DateExtractor.extractFirst("2024-13-01", utc))
    }

    @Test
    fun textWithNoDateYieldsNothing() {
        // Critically: it must return null, not "now". A caller that sees null
        // is required to record DateSource.Unknown.
        assertNull(DateExtractor.extractFirst("no dates here at all", utc))
        assertNull(DateExtractor.extractFirst("", utc))
        assertTrue(DateExtractor.extract("   ", utc).isEmpty())
    }

    @Test
    fun multipleDatesComeBackInDocumentOrder() {
        val matches = DateExtractor.extract(
            "first on 2024-01-15, then again on 2024-02-20, finally 2024-03-25",
            utc,
        )
        assertEquals(3, matches.size)
        assertTrue(matches[0].startIndex < matches[1].startIndex)
        assertTrue(matches[1].startIndex < matches[2].startIndex)
        assertEquals("2024-01-15", matches[0].text)
    }

    @Test
    fun aTimestampIsNotAlsoReportedAsABareDate() {
        // The date-only pattern is nested inside the timestamp pattern; the
        // overlap guard must stop it being counted twice.
        val matches = DateExtractor.extract("at 2024-03-09T14:32:07Z", utc)
        assertEquals(1, matches.size)
    }

    @Test
    fun realisticOcrOutputFromAScreenshot() {
        val ocr = """
            Today 9:41 AM
            You need to stop calling me. I've told you this since 2024-01-15.
            Delivered
        """.trimIndent()
        assertEquals(Triple(2024, 1, 15), ymd(ocr))
    }
}
