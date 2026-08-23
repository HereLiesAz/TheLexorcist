package com.hereliesaz.lexorcist.service

import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.javascript.Context
import org.mozilla.javascript.RhinoException
import com.hereliesaz.lexorcist.utils.parseCsvLine
import com.hereliesaz.lexorcist.utils.unescapeCsvField
import java.io.File

/**
 * Every script seeded into a fresh install must at least parse.
 *
 * `app/src/main/assets/default_scripts.csv` ships with the app and its contents
 * are offered to users as working examples. Two classes of defect had reached
 * it unnoticed, because nothing ever executed or even parsed these scripts:
 *
 *  * 13 scripts used a bare `case.evidence`. `case` is a reserved word in
 *    JavaScript, so those scripts were syntax errors -- they could never run,
 *    on any input, regardless of what the runtime injected.
 *  * 2 scripts called `lex.ui.addOrUpdate(...)`, a namespace `ScriptRunner`
 *    never registered, so they threw `TypeError` on every execution.
 *
 * Both failures surfaced as a `Result.Error` that the caller discarded, so a
 * user saw scripts that appeared to run and silently tagged nothing.
 *
 * This test parses (does not execute) each script body. It catches syntax
 * errors -- the reserved-word class of bug -- cheaply and without needing the
 * Android runtime.
 */
class SeedScriptsParseTest {

    private fun seedFile(): File {
        // Resolve relative to the module regardless of the working directory
        // the test runner chooses.
        val candidates = listOf(
            File("src/main/assets/default_scripts.csv"),
            File("app/src/main/assets/default_scripts.csv"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("default_scripts.csv not found; looked in ${candidates.map { it.absolutePath }}")
    }

    /**
     * Extracts the script bodies exactly the way the production loaders do.
     *
     * `ScriptRepository.loadDefaultScripts` and
     * `DefaultExtrasSeeder.loadDefaultScriptsFromCsv` both read the file line by
     * line, pull quoted fields with `"(.*?)"`, and take index 4 as the script
     * content. Replicating that (rather than parsing the CSV "properly") is the
     * point: this test must see precisely the strings the app will execute.
     */
    private fun scriptBodies(): List<String> {
        return seedFile().readLines()
            .drop(1) // header
            .mapNotNull { line ->
                val tokens = parseCsvLine(line)
                if (tokens.size >= 5) tokens[4].unescapeCsvField() else null
            }
            .filter { it.isNotBlank() }
    }

    @Test
    fun `the seed file is present and holds scripts`() {
        val bodies = scriptBodies()
        assertTrue("expected seed scripts, found ${bodies.size}", bodies.size >= 20)
    }

    @Test
    fun `every seeded script parses as JavaScript`() {
        val rhino = Context.enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1
        val failures = mutableListOf<String>()
        try {
            scriptBodies().forEachIndexed { index, body ->
                try {
                    rhino.compileString(body, "seed[$index]", 1, null)
                } catch (e: RhinoException) {
                    failures += "seed[$index]: ${e.details()} -- ${body.take(90)}"
                }
            }
        } finally {
            Context.exit()
        }
        assertTrue(
            "seeded scripts that do not parse:\n" + failures.joinToString("\n"),
            failures.isEmpty(),
        )
    }

    @Test
    fun `a bare case identifier is caught by the parse check`() {
        // The historical regression was 13 seeded scripts using `case.evidence`
        // rather than `lex.case.evidence`. `case` is reserved, so that is a
        // syntax error and `every seeded script parses as JavaScript` above is
        // what catches it. This pins that the parse check really does reject
        // the shape, so the guarantee cannot quietly weaken.
        //
        // A regex over the raw bodies is deliberately NOT used: the word
        // "case." occurs in English inside comments and string literals in
        // several scripts, and matching that produced only false positives.
        val rhino = Context.enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1
        try {
            var rejected = false
            try {
                rhino.compileString("var x = case.evidence;", "bad", 1, null)
            } catch (e: RhinoException) {
                rejected = true
            }
            assertTrue("a bare `case` identifier must not compile", rejected)

            // ...and the corrected form must.
            rhino.compileString("var x = lex.case.evidence;", "good", 1, null)
        } finally {
            Context.exit()
        }
    }
}
