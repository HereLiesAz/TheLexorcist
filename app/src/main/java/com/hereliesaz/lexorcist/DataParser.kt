package com.hereliesaz.lexorcist

import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.model.Evidence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DataParser {
    private val datePatterns = mapOf(
        """"\b\d{4}-\d{2}-\d{2}\b"""".toRegex() to SimpleDateFormat("yyyy-MM-dd", Locale.US),
        """"\b\d{2}[-/]\d{2}[-/]\d{4}\b"""".toRegex() to SimpleDateFormat("MM-dd-yyyy", Locale.US),
        """"\b\d{2}[-/]\d{2}[-/]\d{2}\b"""".toRegex() to SimpleDateFormat("MM-dd-yy", Locale.US),
        """"\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s\d{1,2},?\s\d{4}\b"""".toRegex() to SimpleDateFormat("MMM d, yyyy", Locale.US),
        """"\b\d{1,2}\s(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s\d{4}\b"""".toRegex() to SimpleDateFormat("dd MMM yyyy", Locale.US),
        """"\b(?:January|February|March|April|May|June|July|August|September|October|November|December)\s\d{1,2},?\s\d{4}\b"""".toRegex() to SimpleDateFormat("MMMM d, yyyy", Locale.US),
        """"\b\d{1,2}\s(?:January|February|March|April|May|June|July|August|September|October|November|December)\s\d{4}\b"""".toRegex() to SimpleDateFormat("dd MMMM yyyy", Locale.US),
        """"\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z\b"""".toRegex() to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    )

    fun parseDates(text: String): List<Long> {
        val dates = mutableListOf<Long>()
        for ((regex, format) in datePatterns) {
            regex.findAll(text).forEach { matchResult ->
                try {
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.isLenient = false
                    dates.add(format.parse(matchResult.value.trim())!!.time)
                } catch (e: Exception) {
                    // Ignore and continue
                }
            }
        }
        return dates
    }

    fun parseNames(text: String): List<String> {
        val nameRegex = """"\b[A-Z][a-z]+ [A-Z][a-z]+\b"""".toRegex()
        return nameRegex.findAll(text).map { it.value }.toList()
    }

    fun parseAddresses(text: String): List<String> {
        val addressRegex = """"\d+\s+[\w\s,]+[A-Z]{2}\s+\d{5}"""".toRegex()
        return addressRegex.findAll(text).map { it.value }.toList()
    }

    fun tagData(text: String): Map<String, List<String>> {
        val dates = parseDates(text).map {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(it))
        }
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
        val allegationRegex = """"(?i)\b(alleges|claims|argues that)\b.*"""".toRegex()
        var currentId = 0 // Assuming Allegation needs an id and it's generated sequentially here
        return allegationRegex.findAll(text).map {
            Allegation(id = currentId++, caseId = caseId, text = it.value) // Assuming Allegation(id: Int, caseId: Int, text: String)
        }.toList()
    }

    fun extractEvidence(caseId: Int, text: String, allegations: List<Allegation>): List<Evidence> {
        val entries = mutableListOf<Evidence>()
        val sentences = text.split("\n")

        for (sentence in sentences) {
            val date = parseDates(sentence).firstOrNull() ?: System.currentTimeMillis()

            val linkedAllegation = allegations.find { sentence.contains(it.text, ignoreCase = true) }
            val categoryRegex = """"(?i)Category:\s*(\w+)"""".toRegex()
            val categoryMatch = categoryRegex.find(sentence)
            val category = categoryMatch?.groupValues?.get(1) ?: ""

            entries.add(
                Evidence(
                    id = entries.size, 
                    caseId = caseId, 
                    allegationId = linkedAllegation?.id?.toString(),
                    content = sentence,
                    amount = null, 
                    timestamp = Date(System.currentTimeMillis()), 
                    sourceDocument = "Parsed from text",
                    documentDate = Date(date), 
                    category = category,
                    tags = null 
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
