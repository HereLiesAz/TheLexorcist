package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Executes real scripts through Rhino against the API surface that
 * `SCRIPT_EXAMPLES.md` documents.
 *
 * None of this was covered. The published examples used `evidence.text`, a
 * `case` global and a `lex.ui` namespace, none of which ScriptRunner ever put
 * on the scope, so the first line of the very first documented example
 * ("Profanity Tagger") threw `TypeError: Cannot call method "toLowerCase" of
 * undefined`. Two of the scripts seeded into every install from
 * `assets/default_scripts.csv` call `lex.ui.addOrUpdate`, and both threw on
 * every run. The resulting `Result.Error` was discarded by the caller, so the
 * failure was invisible.
 *
 * These tests do not touch the network bridges (`lex.ai`, `lex.google`); the
 * services behind them are mocked and never invoked.
 */
class ScriptRunnerApiTest {

    private lateinit var runner: ScriptRunner

    @Before
    fun setUp() {
        runner = ScriptRunner(
            generativeAIService = mock(),
            googleApiService = mock(),
            semanticService = mock(),
        )
    }

    private fun evidence(content: String, tags: List<String> = emptyList()) = Evidence(
        id = 1,
        caseId = 1L,
        spreadsheetId = "sheet-1",
        type = "image",
        content = content,
        formattedContent = null,
        mediaUri = null,
        timestamp = 0L,
        sourceDocument = "test",
        documentDate = 0L,
        allegationId = null,
        allegationElementName = null,
        category = "",
        tags = tags,
    )

    private suspend fun run(script: String, ev: Evidence = evidence("hello"), others: List<Evidence> = emptyList()) =
        runner.runScript(script, ev, others)

    /** Unwraps a success, or fails with the underlying script error rather than a bare `false`. */
    private fun Result<com.hereliesaz.lexorcist.model.ScriptResult>.success() = when (this) {
        is Result.Success -> data
        is Result.Error -> throw AssertionError("script failed: " + generateSequence(exception as Throwable) { it.cause }
            .joinToString(" <- ") { it::class.java.simpleName + ": " + it.message })
        else -> throw AssertionError("unexpected result: $this")
    }

    @Test
    fun `evidence content is readable`() = runTest {
        val r = run("if (evidence.content.indexOf('threat') >= 0) { addTag('threat'); }", evidence("a threat here"))
                assertEquals(listOf("threat"), r.success().tags)
    }

    @Test
    fun `evidence text is aliased for the documented examples`() = runTest {
        // This is example 1 in SCRIPT_EXAMPLES.md, essentially verbatim.
        val r = run(
            "if (evidence.text.toLowerCase().indexOf('damn') >= 0) { addTag('Profanity'); }",
            evidence("well DAMN then"),
        )
                assertEquals(listOf("Profanity"), r.success().tags)
    }

    @Test
    fun `lex case exposes the rest of the case`() = runTest {
        val r = run(
            "if (lex.case.evidence.length === 2) { addTag('two-others'); }",
            others = listOf(evidence("a"), evidence("b")),
        )
        assertEquals(listOf("two-others"), r.success().tags)
    }

    @Test
    fun `caseEvidence is also available as a global`() = runTest {
        val r = run(
            "if (caseEvidence.length === 1) { addTag('one-other'); }",
            others = listOf(evidence("a")),
        )
        assertEquals(listOf("one-other"), r.success().tags)
    }

    @Test
    fun `case evidence is an empty array when none is supplied`() = runTest {
        val r = run("if (lex.case.evidence.length === 0) { addTag('alone'); }")
        assertEquals(listOf("alone"), r.success().tags)
    }

    @Test
    fun `a bare case global would be a JavaScript syntax error`() = runTest {
        // `case` is a reserved word, so SCRIPT_EXAMPLES.md's `case.evidence`
        // cannot ever parse, no matter what the runtime injects. Pinned so
        // nobody "restores" it.
        val r = run("var x = case.evidence;")
        assertTrue(r is Result.Error)
    }

    @Test
    fun `lex ui addOrUpdate records a component`() = runTest {
        val r = run(
            """lex.ui.addOrUpdate('warn', 'Banner', { text: 'Check this', severity: 'high' });""",
        )
                val ui = r.success().uiComponents
        assertEquals(1, ui.size)
        val component = ui["warn"]
        assertNotNull(component)
        assertEquals("Banner", component!!.type)
        assertEquals("Check this", component.properties["text"])
        assertEquals("high", component.properties["severity"])
    }

    @Test
    fun `lex ui renders whole numbers without a decimal point`() = runTest {
        // Rhino models every JS number as a Double, so a naive toString gives "3.0".
        val r = run("""lex.ui.addOrUpdate('c', 'Counter', { count: 3 });""")
        val c = r.success().uiComponents["c"]!!
        assertEquals("3", c.properties["count"])
    }

    @Test
    fun `lex ui remove marks the id for removal`() = runTest {
        val r = run(
            """
            lex.ui.addOrUpdate('a', 'Banner', { text: 'x' });
            lex.ui.remove('a');
            """.trimIndent(),
        )
        val data = r.success()
        assertTrue(data.uiComponents.isEmpty())
        assertTrue(data.removedUiComponentIds.contains("a"))
    }

    @Test
    fun `lex ui clearAll is recorded`() = runTest {
        val r = run(
            """
            lex.ui.addOrUpdate('a', 'Banner', { text: 'x' });
            lex.ui.clearAll();
            """.trimIndent(),
        )
        val data = r.success()
        assertTrue(data.uiComponents.isEmpty())
        assertTrue(data.clearAllUiComponents)
    }

    @Test
    fun `addOrUpdate on an existing id replaces it`() = runTest {
        val r = run(
            """
            lex.ui.addOrUpdate('a', 'Banner', { text: 'first' });
            lex.ui.addOrUpdate('a', 'Banner', { text: 'second' });
            """.trimIndent(),
        )
        val ui = r.success().uiComponents
        assertEquals(1, ui.size)
        assertEquals("second", ui["a"]!!.properties["text"])
    }

    @Test
    fun `the legacy global helpers still work`() = runTest {
        val r = run(
            """
            addTag('t');
            setSeverity('High');
            createNote('a note');
            linkToAllegation('Harassment');
            """.trimIndent(),
        )
        val data = r.success()
        assertEquals(listOf("t"), data.tags)
        assertEquals("High", data.severity)
        assertEquals("a note", data.note)
        assertEquals("Harassment", data.linkedAllegation)
    }

    @Test
    fun `java class access stays blocked`() = runTest {
        val r = run("var f = new java.io.File('/etc/passwd');")
        assertTrue("the sandbox must reject java.* access, got $r", r is Result.Error)
    }

    @Test
    fun `a throwing script is reported as an error rather than silently ignored`() = runTest {
        val r = run("throw new Error('boom');")
        assertTrue(r is Result.Error)
    }

    @Test
    fun `a script that touches an undefined global fails loudly`() = runTest {
        val r = run("noSuchThing.doStuff();")
        assertTrue(r is Result.Error)
    }
}
