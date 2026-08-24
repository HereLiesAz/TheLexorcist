package com.hereliesaz.lexorcist.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateFillerTest {

    @Test
    fun `values are substituted`() {
        val result = TemplateFiller.fill(
            "<p>Case No. {{CASE_NUMBER}}, {{PLAINTIFF_NAMES}} v. {{DEFENDANT_NAMES}}</p>",
            mapOf(
                "CASE_NUMBER" to "2:24-cv-01234",
                "PLAINTIFF_NAMES" to "Carlos Fundora",
                "DEFENDANT_NAMES" to "Warlocks, Inc.",
            ),
        )
        assertEquals(
            "<p>Case No. 2:24-cv-01234, Carlos Fundora v. Warlocks, Inc.</p>",
            result.html,
        )
        assertTrue(result.unresolved.isEmpty())
    }

    @Test
    fun `a party name cannot break the markup`() {
        // Names are user input and go into a document someone files.
        val result = TemplateFiller.fill(
            "<p>{{DEFENDANT_NAMES}}</p>",
            mapOf("DEFENDANT_NAMES" to """Smith & Sons <Holdings> "Ltd""""),
        )
        assertEquals(
            "<p>Smith &amp; Sons &lt;Holdings&gt; &quot;Ltd&quot;</p>",
            result.html,
        )
    }

    @Test
    fun `a missing value leaves a visible gap and is reported`() {
        val result = TemplateFiller.fill(
            "<p>{{ATTORNEY_NAME}}, Bar No. {{ATTORNEY_BAR_NUMBER}}</p>",
            mapOf("ATTORNEY_NAME" to "A. Lawyer"),
        )
        assertEquals("<p>A. Lawyer, Bar No. ${TemplateFiller.GAP}</p>", result.html)
        assertEquals(listOf("ATTORNEY_BAR_NUMBER"), result.unresolved)
        // The literal token must never survive into a filed document.
        assertFalse(result.html.contains("{{"))
    }

    @Test
    fun `a blank value counts as missing`() {
        val result = TemplateFiller.fill("{{COUNTY}}", mapOf("COUNTY" to "   "))
        assertEquals(TemplateFiller.GAP, result.html)
        assertEquals(listOf("COUNTY"), result.unresolved)
    }

    @Test
    fun `a placeholder repeated is reported once`() {
        val result = TemplateFiller.fill("{{JUDGE}} {{JUDGE}} {{JUDGE}}", emptyMap())
        assertEquals(listOf("JUDGE"), result.unresolved)
        assertEquals("${TemplateFiller.GAP} ${TemplateFiller.GAP} ${TemplateFiller.GAP}", result.html)
    }

    @Test
    fun `text that resembles a placeholder is left alone`() {
        // Lowercase and mixed case are not placeholders; neither is a lone brace.
        val source = "{{not_a_placeholder}} {single} {{Mixed_Case}}"
        assertEquals(source, TemplateFiller.fill(source, emptyMap()).html)
    }

    @Test
    fun `placeholders can be listed before filling`() {
        assertEquals(
            listOf("CASE_NUMBER", "JUDGE"),
            TemplateFiller.placeholdersIn("{{CASE_NUMBER}} before {{JUDGE}} and {{CASE_NUMBER}} again"),
        )
    }

    @Test
    fun `a substituted value is not itself expanded`() {
        // A case number containing a placeholder-shaped string must not be
        // re-scanned; Regex.replace does not rescan its own output.
        val result = TemplateFiller.fill(
            "{{CASE_NUMBER}}",
            mapOf("CASE_NUMBER" to "{{JUDGE}}", "JUDGE" to "Hon. X"),
        )
        assertEquals("{{JUDGE}}", result.html)
        assertTrue(result.unresolved.isEmpty())
    }
}
