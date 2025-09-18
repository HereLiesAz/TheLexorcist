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
    fun `filterTemplates with Texas court returns correct templates`() {
        val templatesWithTexas = templates + Template("7", "Texas Answer", "", "", "", "Texas")
        val filtered = filterTemplates(templatesWithTexas, "Texas")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "Texas Answer" })
    }

    @Test
    fun `filterTemplates with unknown court returns only generic templates`() {
        val filtered = filterTemplates(templates, "Florida")
        assertEquals(2, filtered.size)
    }

    @Test
    fun `filterTemplates with New York court returns correct templates`() {
        val templatesWithNY = templates + Template("6", "New York Complaint", "", "", "", "New York")
        val filtered = filterTemplates(templatesWithNY, "New York")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "New York Complaint" })
        assert(filtered.any { it.name == "Generic Cover Sheet" })
        assert(filtered.any { it.name == "Generic Custody Log" })
    }

    @Test
    fun `filterTemplates with California court returns correct templates including answer`() {
        val templatesWithCalifornia = templates +
            // Template("1", "California Pleading", "", "", "", "California") + // Removed duplicate
            Template("8", "California Answer", "", "", "", "California")
        val filtered = filterTemplates(templatesWithCalifornia, "California")
        assertEquals(4, filtered.size)
        assert(filtered.any { it.name == "California Pleading" })
        assert(filtered.any { it.name == "California Answer" })
    }

    @Test
    fun `filterTemplates with Federal court returns correct templates including answer`() {
        val templatesWithFederal = templates +
            // Template("2", "Federal Pleading", "", "", "", "Federal") + // Removed duplicate
            Template("9", "Federal Answer", "", "", "", "Federal")
        val filtered = filterTemplates(templatesWithFederal, "Federal")
        assertEquals(4, filtered.size)
        assert(filtered.any { it.name == "Federal Pleading" })
        assert(filtered.any { it.name == "Federal Answer" })
    }

    @Test
    fun `filterTemplates with Federal court returns correct templates including motion`() {
        val templatesWithFederal = templates +
            // Template("2", "Federal Pleading", "", "", "", "Federal") + // Removed duplicate
            Template("9", "Federal Answer", "", "", "", "Federal") +
            Template("10", "Federal Motion to Dismiss", "", "", "", "Federal")
        val filtered = filterTemplates(templatesWithFederal, "Federal")
        assertEquals(5, filtered.size)
        assert(filtered.any { it.name == "Federal Pleading" })
        assert(filtered.any { it.name == "Federal Answer" })
        assert(filtered.any { it.name == "Federal Motion to Dismiss" })
    }

    @Test
    fun `filterTemplates with California court returns correct templates including motion`() {
        val templatesWithCalifornia = templates +
            // Template("1", "California Pleading", "", "", "", "California") + // Removed duplicate
            Template("8", "California Answer", "", "", "", "California") +
            Template("11", "California Motion to Dismiss", "", "", "", "California")
        val filtered = filterTemplates(templatesWithCalifornia, "California")
        assertEquals(5, filtered.size)
        assert(filtered.any { it.name == "California Pleading" })
        assert(filtered.any { it.name == "California Answer" })
        assert(filtered.any { it.name == "California Motion to Dismiss" })
    }

    @Test
    fun `filterTemplates with Texas court returns correct templates including motion`() {
        val templatesWithTexas = templates +
            Template("7", "Texas Answer", "", "", "", "Texas") +
            Template("12", "Texas Motion to Dismiss", "", "", "", "Texas")
        val filtered = filterTemplates(templatesWithTexas, "Texas")
        assertEquals(4, filtered.size)
        assert(filtered.any { it.name == "Texas Answer" })
        assert(filtered.any { it.name == "Texas Motion to Dismiss" })
    }

    @Test
    fun `filterTemplates with Florida court returns correct templates`() {
        val templatesWithFlorida = templates +
            Template("13", "Florida Complaint", "", "", "", "Florida")
        val filtered = filterTemplates(templatesWithFlorida, "Florida")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "Florida Complaint" })
    }

    @Test
    fun `filterTemplates with Illinois court returns correct templates`() {
        val templatesWithIllinois = templates +
            Template("14", "Illinois Complaint", "", "", "", "Illinois")
        val filtered = filterTemplates(templatesWithIllinois, "Illinois")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "Illinois Complaint" })
    }
}
