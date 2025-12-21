package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.utils.SpreadsheetUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class FormulaInjectionTest {

    @Test
    fun `test sanitizeForSpreadsheet escapes hazardous characters`() {
        val dangerousInputs = listOf(
            "=1+1",
            "+1+1",
            "-1+1",
            "@SUM(1,1)",
            "=cmd|' /C calc'!A0"
        )

        val safeInputs = listOf(
            "Just normal text",
            "123",
            "email@example.com", // @ is only dangerous at start
            "A=B", // = is only dangerous at start
            null,
            ""
        )

        dangerousInputs.forEach { input ->
            val sanitized = SpreadsheetUtils.sanitizeForSpreadsheet(input)
            assertEquals("'$input", sanitized)
        }

        safeInputs.forEach { input ->
            val sanitized = SpreadsheetUtils.sanitizeForSpreadsheet(input)
            if (input.isNullOrEmpty()) {
                assertEquals("", sanitized)
            } else {
                assertEquals(input, sanitized)
            }
        }
    }

    @Test
    fun `test sanitization of tags`() {
        // This simulates how tags are processed in LocalFileStorageService:
        // evidence.tags.map { SpreadsheetUtils.sanitizeForSpreadsheet(it) }.joinToString(",")

        val tags = listOf("normal", "=dangerous", "@risky", "safe")
        val sanitizedTags = tags.map { SpreadsheetUtils.sanitizeForSpreadsheet(it) }

        assertEquals("normal", sanitizedTags[0])
        assertEquals("'=dangerous", sanitizedTags[1])
        assertEquals("'@risky", sanitizedTags[2])
        assertEquals("safe", sanitizedTags[3])

        val joined = sanitizedTags.joinToString(",")
        assertEquals("normal,'=dangerous,'@risky,safe", joined)
    }
}
