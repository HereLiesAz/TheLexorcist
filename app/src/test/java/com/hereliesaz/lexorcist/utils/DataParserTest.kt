package com.hereliesaz.lexorcist.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the entity extraction that fed evidence tagging and
 * document dating.
 *
 * Every regex in this object used to be a raw string wrapped in literal quote
 * characters, so each of these functions returned an empty list for any input
 * that did not happen to have the entity in quotation marks. There were no
 * tests over any of it.
 */
class DataParserTest {

    @Test
    fun `dates are found in unquoted prose`() {
        val dates = DataParser.parseDates("Incident occurred on 2024-01-15 at the residence")
        assertEquals(1, dates.size)
    }

    @Test
    fun `text with no date yields no date rather than the current time`() {
        // This is the assertion that matters. The old pipeline resolved
        // `exif ?: parsed ?: now()`, so an empty result here became a
        // fabricated timestamp that the timeline rendered as fact.
        assertTrue(DataParser.parseDates("no dates in this line").isEmpty())
        assertNull(DataParser.parseFirstDate("no dates in this line"))
    }

    @Test
    fun `names are found in unquoted prose`() {
        val names = DataParser.parseNames("Statement taken from Jane Doe by Officer Ramirez")
        assertTrue("expected Jane Doe in $names", names.any { it.contains("Jane Doe") })
    }

    @Test
    fun `single capitalised words are not treated as names`() {
        assertTrue(DataParser.parseNames("Monday").isEmpty())
    }

    @Test
    fun `addresses are found in unquoted prose`() {
        val found = DataParser.parseAddresses("served at 1234 Elm Street, Springfield, IL 62704 today")
        assertEquals(1, found.size)
        assertTrue(found.first().contains("62704"))
    }

    @Test
    fun `times are normalised to 24 hour form`() {
        assertEquals(listOf("14:05:00"), DataParser.parseTimestamps("called at 2:05 PM"))
        assertEquals(listOf("09:30:00"), DataParser.parseTimestamps("arrived 9:30 AM"))
        assertEquals(listOf("00:15:00"), DataParser.parseTimestamps("at 12:15 AM"))
        assertEquals(listOf("23:59:07"), DataParser.parseTimestamps("logged 23:59:07"))
    }

    @Test
    fun `impossible times are rejected`() {
        assertTrue(DataParser.parseTimestamps("99:99").isEmpty())
    }

    @Test
    fun `tagData returns every entity kind`() {
        val tagged = DataParser.tagData(
            "On 2024-01-15 Jane Doe was at 1234 Elm Street, Springfield, IL 62704 at 2:05 PM",
        )
        assertEquals(listOf("2024-01-15"), tagged["dates"])
        assertTrue(tagged["names"].orEmpty().isNotEmpty())
        assertTrue(tagged["addresses"].orEmpty().isNotEmpty())
        assertEquals(listOf("14:05:00"), tagged["timestamps"])
    }

    @Test
    fun `tagData on entity-free text returns empty lists, not nulls`() {
        val tagged = DataParser.tagData("ok")
        assertNotNull(tagged["dates"])
        assertTrue(tagged.values.all { it.isEmpty() })
    }

    @Test
    fun `extractEvidence splits on real newlines`() {
        // The old implementation split on the two-character string "\\n", so it
        // produced exactly one row no matter how many lines the input had.
        val text = "line one\nline two\nline three"
        val evidence = DataParser.extractEvidence("sheet-1", text, emptyList(), caseId = 7)
        assertEquals(3, evidence.size)
        assertEquals("line one", evidence[0].content)
        assertEquals("line three", evidence[2].content)
    }

    @Test
    fun `extractEvidence leaves undated lines marked as not established`() {
        val evidence = DataParser.extractEvidence("sheet-1", "no date here", emptyList(), caseId = 1)
        assertEquals(1, evidence.size)
        assertEquals(0L, evidence.first().documentDate)
    }

    @Test
    fun `extractEvidence picks up a date when the line has one`() {
        val evidence = DataParser.extractEvidence("sheet-1", "happened 2024-01-15", emptyList(), caseId = 1)
        assertTrue(evidence.first().documentDate > 0L)
    }

    @Test
    fun `allegations are matched without requiring quotation marks`() {
        val data = DataParser.parseTextForCase(
            spreadsheetId = "sheet-1",
            text = "The petitioner alleges that the respondent made repeated threats.",
            caseId = 1,
        )
        assertEquals(1, data.allegations.size)
    }
}
