package com.example.legalparser

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DataParser {
    fun parseAmounts(text: String): List<String> {
        val amountRegex = """\$(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)""".toRegex()
        val matches = amountRegex.findAll(text)
        return matches.map { it.value }.toList()
    }

    fun parseDates(text: String): List<Long> {
        val dateRegex = """(?i)(?:\d{1,4}[/-]\d{1,2}[/-]\d{2,4})|(?:\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b\s\d{1,2},?\s\d{4})""".toRegex()
        val matches = dateRegex.findAll(text)
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("MM-dd-yyyy", Locale.US),
            SimpleDateFormat("MM/dd/yy", Locale.US),
            SimpleDateFormat("MM-dd-yy", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("MMM d, yyyy", Locale.US)
        ).onEach {
            it.timeZone = TimeZone.getTimeZone("UTC")
            it.isLenient = false
        }

        return matches.mapNotNull { matchResult ->
            for (format in dateFormats) {
                try {
                    return@mapNotNull format.parse(matchResult.value)?.time
                } catch (e: Exception) {
                    // Ignore and try next format
                }
            }
            null
        }.toList()
    }
}
