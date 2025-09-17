package com.hereliesaz.lexorcist.ui

import com.hereliesaz.lexorcist.model.Template
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplatesScreenTest {

    private val templates = listOf(
        Template("1", "California Pleading", "", "", "", "California"),
        Template("2", "Federal Pleading", "", "", "", "Federal"),
        Template("3", "Louisiana Pleading", "", "", "", "Louisiana"),
        Template("4", "Generic Cover Sheet", "", "", "", "Generic"),
        Template("5", "Generic Custody Log", "", "", "", "Generic")
    )

    @Test
    fun `filterTemplates with blank court returns all templates`() {
        val filtered = filterTemplates(templates, "")
        assertEquals(5, filtered.size)
    }

    @Test
    fun `filterTemplates with specific court returns correct templates`() {
        val filtered = filterTemplates(templates, "California")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "California Pleading" })
        assert(filtered.any { it.name == "Generic Cover Sheet" })
        assert(filtered.any { it.name == "Generic Custody Log" })
    }

    @Test
    fun `filterTemplates with specific court is case-insensitive`() {
        val filtered = filterTemplates(templates, "california")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "California Pleading" })
    }

    @Test
    fun `filterTemplates with specific court returns correct templates 2`() {
        val filtered = filterTemplates(templates, "Federal")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "Federal Pleading" })
    }

    @Test
    fun `filterTemplates with unknown court returns only generic templates`() {
        val filtered = filterTemplates(templates, "Texas")
        assertEquals(2, filtered.size)
    }
}
