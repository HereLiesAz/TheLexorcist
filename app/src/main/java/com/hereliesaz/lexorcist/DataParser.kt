package com.hereliesaz.lexorcist

import java.text.SimpleDateFormat
import java.util.*

object DataParser {

    private val timePatterns: Map<Regex, SimpleDateFormat> = mapOf(
        Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)\b""", RegexOption.IGNORE_CASE) to SimpleDateFormat("h:mm a", Locale.US),
        Regex("""\b\d{1,2}:\d{2}:\d{2}\b""") to SimpleDateFormat("HH:mm:ss", Locale.US),
        Regex("""\b\d{1,2}:\d{2}\b""") to SimpleDateFormat("HH:mm", Locale.US)
    )

    fun parseTime(text: String): Date? {
        for ((pattern, format) in timePatterns) {
            pattern.findAll(text).forEach { matchResult ->
                try {
                    format.isLenient = false
                    return format.parse(matchResult.value)
                } catch (e: Exception) {
                    // Ignore and try next pattern
                }
            }
        }
        return null
    }

    fun tagData(text: String): Map<String, List<String>> {
        val taggedData = mutableMapOf<String, MutableList<String>>()
        val lines = text.lines()

        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                taggedData.getOrPut(key) { mutableListOf() }.add(value)
            }
        }
        return taggedData.mapValues { it.value.toList() }
    }
}
