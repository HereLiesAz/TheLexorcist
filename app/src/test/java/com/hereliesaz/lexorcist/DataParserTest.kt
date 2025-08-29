package com.hereliesaz.lexorcist

import org.junit.Assert.assertEquals
import org.junit.Test

class DataParserTest {

    @Test
    fun `test parseDates with yyyy-MM-dd format`() {
        val text = "Date: 2023-01-15"
        val dates = DataParser.parseDates(text)
        assertEquals(1, dates.size)
    }

    @Test
    fun `test parseDates with MM-dd-yyyy format`() {
        val text = "Date: 01-15-2023"
        val dates = DataParser.parseDates(text)
        assertEquals(1, dates.size)
    }

    @Test
    fun `test parseDates with MMM d, yyyy format`() {
        val text = "Date: Jan 15, 2023"
        val dates = DataParser.parseDates(text)
        assertEquals(1, dates.size)
    }

    @Test
    fun `test parseNames`() {
        val text = "John Doe and Jane Smith are here."
        val names = DataParser.parseNames(text)
        assertEquals(listOf("John Doe", "Jane Smith"), names)
    }

    @Test
    fun `test parseAddresses`() {
        val text = "123 Main St, Anytown, CA 12345"
        val addresses = DataParser.parseAddresses(text)
        assertEquals(listOf("123 Main St, Anytown, CA 12345"), addresses)
    }

    @Test
    fun `test tagData`() {
        val text = "John Doe lives at 123 Main St, Anytown, CA 12345. His birthday is 1990-01-01."
        val taggedData = DataParser.tagData(text)
        val expectedDate = "1990-01-01"
        val actualDate = taggedData["dates"]?.firstOrNull()
        assertEquals(expectedDate, actualDate)
        assertEquals(listOf("John Doe"), taggedData["names"])
        assertEquals(listOf("123 Main St, Anytown, CA 12345"), taggedData["addresses"])
    }

    @Test
    fun `test extractEvidence`() {
        val text = "The first piece of evidence.\nThe second piece of evidence."
        val evidence = DataParser.extractEvidence(1, text, emptyList())
        assertEquals(2, evidence.size)
        assertEquals("The first piece of evidence.", evidence[0].content)
        assertEquals("The second piece of evidence.", evidence[1].content)
    }

    @Test
    fun `test parseDates with no dates`() {
        val text = "This is a string with no dates."
        val dates = DataParser.parseDates(text)
        assertEquals(0, dates.size)
    }

    @Test
    fun `test parseNames with no names`() {
        val text = "This is a string with no names."
        val names = DataParser.parseNames(text)
        assertEquals(0, names.size)
    }

    @Test
    fun `test parseAddresses with no addresses`() {
        val text = "This is a string with no addresses."
        val addresses = DataParser.parseAddresses(text)
        assertEquals(0, addresses.size)
    }

    @Test
    fun `test extractEvidence with category`() {
        val text = "This is a piece of evidence. Category: Medical"
        val evidence = DataParser.extractEvidence(1, text, emptyList())
        assertEquals(1, evidence.size)
        assertEquals("Medical", evidence[0].category)
    }
}
