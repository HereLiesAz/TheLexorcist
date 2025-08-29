package com.hereliesaz.lexorcist

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DataParserTest {

    private fun getExpectedTimestamp(dateString: String, format: String): Long {
        val sdf = SimpleDateFormat(format, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.parse(dateString)!!.time
    }

    @Test
    fun `parseDates should extract dates in yyyy-MM-dd format`() {
        val text = "Some text with a date 2023-01-15 and another 2024-02-20."
        val expected = listOf(
            getExpectedTimestamp("2023-01-15", "yyyy-MM-dd"),
            getExpectedTimestamp("2024-02-20", "yyyy-MM-dd")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should extract dates in MM-dd-yyyy format`() {
        val text = "Some text with a date 01-15-2023 and another 02-20-2024."
        val expected = listOf(
            getExpectedTimestamp("01-15-2023", "MM-dd-yyyy"),
            getExpectedTimestamp("02-20-2024", "MM-dd-yyyy")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should extract dates in MM-dd-yy format`() {
        val text = "Some text with a date 01-15-23 and another 02-20-24."
        val expected = listOf(
            getExpectedTimestamp("01-15-23", "MM-dd-yy"),
            getExpectedTimestamp("02-20-24", "MM-dd-yy")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should extract dates in MM slash dd slash yyyy format`() {
        val text = "Some text with a date 01/15/2023 and another 02/20/2024."
        val expected = listOf(
            getExpectedTimestamp("01/15/2023", "MM/dd/yyyy"),
            getExpectedTimestamp("02/20/2024", "MM/dd/yyyy")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should extract dates in yyyy slash MM slash dd format`() {
        val text = "Some text with a date 2023/01/15 and another 2024/02/20."
        val expected = listOf(
            getExpectedTimestamp("2023/01/15", "yyyy/MM/dd"),
            getExpectedTimestamp("2024/02/20", "yyyy/MM/dd")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should extract dates in MM slash dd slash yy format`() {
        val text = "Some text with a date 01/15/23 and another 02/20/24."
        val expected = listOf(
            getExpectedTimestamp("01/15/23", "MM/dd/yy"),
            getExpectedTimestamp("02/20/24", "MM/dd/yy")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should extract dates in MMM d, yyyy format`() {
        val text = "Some text with a date Jan 15, 2023 and another Feb 20, 2024."
        val expected = listOf(
            getExpectedTimestamp("Jan 15, 2023", "MMM d, yyyy"),
            getExpectedTimestamp("Feb 20, 2024", "MMM d, yyyy")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should extract dates in MMMM d, yyyy format`() {
        val text = "Some text with a date January 15, 2023 and another February 20, 2024."
        val expected = listOf(
            getExpectedTimestamp("January 15, 2023", "MMMM d, yyyy"),
            getExpectedTimestamp("February 20, 2024", "MMMM d, yyyy")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseDates should handle multiple date formats`() {
        val text = "Dates: 2023-01-15, 02/20/2024, and Dec 25, 2022."
        val expected = listOf(
            getExpectedTimestamp("2023-01-15", "yyyy-MM-dd"),
            getExpectedTimestamp("02/20/2024", "MM/dd/yyyy"),
            getExpectedTimestamp("Dec 25, 2022", "MMM d, yyyy")
        )
        val actual = DataParser.parseDates(text)
        assertEquals(expected.sorted(), actual.sorted())
    }

    @Test
    fun `parseDates should return empty list when no dates are found`() {
        val text = "Some text without any dates."
        val expected = emptyList<Long>()
        val actual = DataParser.parseDates(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseNames should extract full names`() {
        val text = "John Doe, Jane Smith, and First Last."
        val expected = listOf("John Doe", "Jane Smith", "First Last")
        val actual = DataParser.parseNames(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseNames should return empty list if no names are found`() {
        val text = "Some text without any names."
        val expected = emptyList<String>()
        val actual = DataParser.parseNames(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseAddresses should extract addresses`() {
        val text = "Address: 123 Main St, Anytown, CA 12345. Another one at 456 Oak Ave, Sometown, NY 54321."
        val expected = listOf("123 Main St, Anytown, CA 12345", "456 Oak Ave, Sometown, NY 54321")
        val actual = DataParser.parseAddresses(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseAddresses should return empty list if no addresses are found`() {
        val text = "Some text without any addresses."
        val expected = emptyList<String>()
        val actual = DataParser.parseAddresses(text)
        assertEquals(expected, actual)
    }

    @Test
    fun `tagData should correctly tag dates, names, and addresses`() {
        val text = "John Doe (from 123 Main St, Anytown, CA 12345) has a meeting on 2024-01-01 with Jane Smith."
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val expectedDate = sdf.format(Date(getExpectedTimestamp("2024-01-01", "yyyy-MM-dd")))

        val expected = mapOf(
            "dates" to listOf(expectedDate),
            "names" to listOf("John Doe", "Jane Smith"),
            "addresses" to listOf("123 Main St, Anytown, CA 12345")
        )
        val actual = DataParser.tagData(text)
        assertEquals(expected, actual)
    }
}
