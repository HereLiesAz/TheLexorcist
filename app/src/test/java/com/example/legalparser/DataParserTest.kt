package com.example.legalparser

import org.junit.Assert.assertEquals
import org.junit.Test

class DataParserTest {

    @Test
    fun `test with simple amount`() {
        val text = "The total is $100."
        val amounts = DataParser.parseAmounts(text)
        assertEquals(listOf("$100"), amounts)
    }

    @Test
    fun `test with amount with decimals`() {
        val text = "Price: $19.99"
        val amounts = DataParser.parseAmounts(text)
        assertEquals(listOf("$19.99"), amounts)
    }

    @Test
    fun `test with amount with commas`() {
        val text = "A large amount: $1,234.56"
        val amounts = DataParser.parseAmounts(text)
        assertEquals(listOf("$1,234.56"), amounts)
    }

    @Test
    fun `test with amount without dollar sign`() {
        val text = "The value is 500.00"
        val amounts = DataParser.parseAmounts(text)
        assertEquals(listOf("500.00"), amounts)
    }

    @Test
    fun `test with multiple amounts`() {
        val text = "Invoice with items: $25.50, $10, and 5.00."
        val amounts = DataParser.parseAmounts(text)
        assertEquals(listOf("$25.50", "$10", "5.00"), amounts)
    }

    @Test
    fun `test with no amounts`() {
        val text = "This is a regular sentence with no numbers."
        val amounts = DataParser.parseAmounts(text)
        assertEquals(emptyList<String>(), amounts)
    }

    @Test
    fun `test with amounts and other numbers`() {
        val text = "On 2024-08-25, the charge was $99. Please pay by 09/01/2024."
        val amounts = DataParser.parseAmounts(text)
        assertEquals(listOf("$99"), amounts)
    }

    @Test
    fun `test with complex amounts and text`() {
        val text = "The subtotal is $1,234.56, tax is $86.42, and the total is $1,320.98."
        val amounts = DataParser.parseAmounts(text)
        assertEquals(listOf("$1,234.56", "$86.42", "$1,320.98"), amounts)
    }
}
