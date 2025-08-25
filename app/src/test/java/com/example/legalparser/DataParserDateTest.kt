package com.example.legalparser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.*

class DataParserDateTest {

    private fun getTimestamp(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(year, month - 1, day, 0, 0, 0) // Month is 0-based in Calendar
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun `test with yyyy-mm-dd format`() {
        val text = "Date: 2024-01-15"
        val expected = getTimestamp(2024, 1, 15)
        val dates = DataParser.parseDates(text)
        assertEquals(listOf(expected), dates)
    }

    @Test
    fun `test with mm-dd-yyyy format`() {
        val text = "Date: 02-28-2023"
        val expected = getTimestamp(2023, 2, 28)
        val dates = DataParser.parseDates(text)
        assertEquals(listOf(expected), dates)
    }

    @Test
    fun `test with month dd, yyyy format`() {
        val text = "Dated this Aug 1, 2022"
        val expected = getTimestamp(2022, 8, 1)
        val dates = DataParser.parseDates(text)
        assertEquals(listOf(expected), dates)
    }

    @Test
    fun `test with multiple dates`() {
        val text = "Start 01/01/2022, end 12/31/2022"
        val expected = listOf(
            getTimestamp(2022, 1, 1),
            getTimestamp(2022, 12, 31)
        )
        val dates = DataParser.parseDates(text)
        assertEquals(expected, dates)
    }

    @Test
    fun `test with no dates`() {
        val text = "This is a regular sentence."
        val dates = DataParser.parseDates(text)
        assertEquals(emptyList<Long>(), dates)
    }

    @Test
    fun `test with yyyy-slash-mm-dd format`() {
        val text = "Date: 2024/03/20"
        val expected = getTimestamp(2024, 3, 20)
        val dates = DataParser.parseDates(text)
        assertEquals(listOf(expected), dates)
    }
}
