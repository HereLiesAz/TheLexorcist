package com.hereliesaz.lexorcist

import com.hereliesaz.lexorcist.db.Allegation
import com.hereliesaz.lexorcist.model.Evidence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DataParser {
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
            SimpleDateFormat("MMM d, yyyy", Locale.US),
            SimpleDateFormat("MMMM d, yyyy", Locale.US)
        ).onEach {
            it.timeZone = TimeZone.getTimeZone("UTC")
            it.isLenient = true // Set to true to allow for more flexible parsing
        }

        return matches.mapNotNull { matchResult ->
            for (format in dateFormats) {
                try {
                    return@mapNotNull format.parse(matchResult.value.trim())?.time
                } catch (e: Exception) {
                    // Ignore and try next format
                }
            }
            null
        }.toList()
    }

    fun parseNames(text: String): List<String> {
        val nameRegex = """\b[A-Z][a-z]+ [A-Z][a-z]+\b""".toRegex()
        return nameRegex.findAll(text).map { it.value }.toList()
    }

    fun parseAddresses(text: String): List<String> {
        val addressRegex = """\d+\s+([a-zA-Z]+\s+)+[a-zA-Z]+,\s+[A-Z]{2}\s+\d{5}""".toRegex()
        return addressRegex.findAll(text).map { it.value }.toList()
    }

    fun tagData(text: String): Map<String, List<String>> {
        val dates = parseDates(text).map { Date(it).toString() }
        val names = parseNames(text)
        val addresses = parseAddresses(text)

        return mapOf(
            "dates" to dates,
            "names" to names,
            "addresses" to addresses
        )
    }

    fun parseTextForCase(caseId: Int, text: String): CaseData {
        val allegations = extractAllegations(caseId, text)
        val evidence = extractEvidence(caseId, text, allegations)
        // ... extract other data types ...

        return CaseData(allegations, evidence)
    }

    private fun extractAllegations(caseId: Int, text: String): List<Allegation> {
        val allegationRegex = """(?i)\b(alleges|claims|argues that)\b.*""".toRegex()
        return allegationRegex.findAll(text).mapIndexed { index, matchResult ->
            Allegation(id = index, caseId = caseId, text = matchResult.value)
        }.toList()
    }

    private fun extractEvidence(caseId: Int, text: String, allegations: List<Allegation>): List<Evidence> {
        val dateRegex = """(?i)(?:\d{1,4}[/-]\d{1,2}[/-]\d{2,4})|(?:\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b\s\d{1,2},?\s\d{4})""".toRegex()
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("MM-dd-yyyy", Locale.US),
            SimpleDateFormat("MM/dd/yy", Locale.US),
            SimpleDateFormat("MM-dd-yy", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("MMM d, yyyy", Locale.US),
            SimpleDateFormat("MMMM d, yyyy", Locale.US)
        ).onEach {
            it.timeZone = TimeZone.getTimeZone("UTC")
            it.isLenient = true
        }

        val entries = mutableListOf<Evidence>()
        val sentences = text.split("\n")

        for (sentence in sentences) {
            val dateMatch = dateRegex.find(sentence)
            val date = dateMatch?.let { matchResult ->
                dateFormats.firstNotNullOfOrNull { format ->
                    try {
                        format.parse(matchResult.value.trim())?.time
                    } catch (e: Exception) {
                        null
                    }
                }
            } ?: System.currentTimeMillis()

            val linkedAllegation = allegations.find { sentence.contains(it.text, ignoreCase = true) }

            entries.add(
                Evidence(
                    allegationId = linkedAllegation?.id,
                    content = sentence,
                    timestamp = System.currentTimeMillis(),
                    sourceDocument = "Parsed from text",
                    documentDate = date,
                    tags = emptyList()
                )
            )
        }
        return entries
    }

    data class CaseData(
        val allegations: List<Allegation>,
        val evidence: List<Evidence>
    )
}