package com.hereliesaz.lexorcist.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Field-level checks over every seed CSV the app loads from `assets`.
 *
 * Five loaders each reimplemented CSV parsing as the regex `"(.*?)"`. That is
 * not a CSV parser: the files follow RFC 4180 and write an embedded quote as
 * `""`, so a non-greedy match ends the field at the first one.
 *
 * For `default_scripts.csv` this was catastrophic and silent -- every script
 * body was truncated at its first string literal. For the other four it
 * happened to be harmless, because the fields those loaders read contain no
 * quotes or commas. "Happened to be harmless" is not a property worth relying
 * on, so they all use [parseCsvLine] now, and these tests pin the shape of the
 * data that makes it safe.
 */
class SeedCsvParsingTest {

    private fun asset(name: String): File =
        listOf(File("src/main/assets/$name"), File("app/src/main/assets/$name"))
            .firstOrNull { it.exists() }
            ?: error("asset $name not found")

    private fun rows(name: String): List<String> =
        asset(name).readLines().drop(1).filter { it.isNotBlank() }

    // Each seed file, and the number of columns its loader indexes into.
    private val files = listOf(
        "default_scripts.csv" to 5,
        "default_templates.csv" to 7,
        "allegations.csv" to 7,
        "exhibits.csv" to 4,
        "jurisdictions.csv" to 2,
    )

    @Test
    fun `every seed file has at least the columns its loader reads`() {
        files.forEach { (name, columns) ->
            rows(name).forEachIndexed { i, line ->
                val fields = parseCsvLine(line)
                assertTrue(
                    "$name row ${i + 2} parsed to ${fields.size} fields, loader needs $columns",
                    fields.size >= columns,
                )
            }
        }
    }

    @Test
    fun `parseCsvLine keeps embedded quotes instead of ending the field`() {
        val fields = parseCsvLine("""	"a","say ""hi"" now","c"	""".trim())
        assertEquals(listOf("a", """say "hi" now""", "c"), fields)
    }

    @Test
    fun `parseCsvLine keeps commas inside a quoted field`() {
        assertEquals(listOf("a", "one, two", "c"), parseCsvLine(""""a","one, two","c""""))
    }

    @Test
    fun `parseCsvLine handles empty and unquoted fields`() {
        assertEquals(listOf("a", "", "c"), parseCsvLine(""""a","","c""""))
        assertEquals(listOf("a", "b"), parseCsvLine("a,b"))
    }

    @Test
    fun `the old regex would have truncated the script bodies`() {
        // Pins the defect this replaced, so the reason for parseCsvLine cannot
        // be lost. The scripts file is the one where the two disagree.
        val legacy = Regex("\"(.*?)\"")
        val disagreements = rows("default_scripts.csv").count { line ->
            legacy.findAll(line).count() != parseCsvLine(line).size
        }
        assertTrue(
            "expected the legacy regex to disagree with a real parse on the scripts file",
            disagreements > 0,
        )
    }

    @Test
    fun `unescapeCsvField resolves newlines without eating regex escapes`() {
        assertEquals("a\nb", """a\nb""".unescapeCsvField())
        assertEquals("a\tb", """a\tb""".unescapeCsvField())
        // A single backslash before a letter we do not own is a regex escape
        // and must survive untouched.
        assertEquals("""\b\d{4}""", """\b\d{4}""".unescapeCsvField())
        // An escaped backslash yields one backslash, and must not then be
        // re-read as the start of a newline escape.
        assertEquals("""\n""", """\\n""".unescapeCsvField())
    }

    @Test
    fun `unescapeCsvField leaves text with no backslashes alone`() {
        val plain = "nothing to do here"
        assertEquals(plain, plain.unescapeCsvField())
    }
}
