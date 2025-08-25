package com.example.legalparser

object DataParser {
    fun parseAmounts(text: String): List<String> {
        val amountRegex = """\$?\s?(\d{1,3}([,.]\d{3})*|\d+)(\.\d{2})?""".toRegex()
        val matches = amountRegex.findAll(text)
        return matches.map { it.value }.toList()
    }
}
