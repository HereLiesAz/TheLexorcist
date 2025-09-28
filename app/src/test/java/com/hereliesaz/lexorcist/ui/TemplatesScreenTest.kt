package com.hereliesaz.lexorcist.ui

import com.hereliesaz.lexorcist.model.Template
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplatesScreenTest {

    private val templates = listOf(
        Template(id = "1", name = "California Pleading", description = "", content = "", authorName = "", authorEmail = "", court = "California"),
        Template(id = "2", name = "Federal Pleading", description = "", content = "", authorName = "", authorEmail = "", court = "Federal"),
        Template(id = "3", name = "Louisiana Pleading", description = "", content = "", authorName = "", authorEmail = "", court = "Louisiana"),
        Template(id = "4", name = "Generic Cover Sheet", description = "", content = "", authorName = "", authorEmail = "", court = "Generic"),
        Template(id = "5", name = "Generic Custody Log", description = "", content = "", authorName = "", authorEmail = "", court = "Generic")
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
        val templatesWithTexas = templates + Template(id = "7", name = "Texas Answer", description = "", content = "", authorName = "", authorEmail = "", court = "Texas")
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
        val templatesWithNY = templates + Template(id = "6", name = "New York Complaint", description = "", content = "", authorName = "", authorEmail = "", court = "New York")
        val filtered = filterTemplates(templatesWithNY, "New York")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "New York Complaint" })
        assert(filtered.any { it.name == "Generic Cover Sheet" })
        assert(filtered.any { it.name == "Generic Custody Log" })
    }

    @Test
    fun `filterTemplates with California court returns correct templates including answer`() {
        val templatesWithCalifornia = templates +
            Template(id = "8", name = "California Answer", description = "", content = "", authorName = "", authorEmail = "", court = "California")
        val filtered = filterTemplates(templatesWithCalifornia, "California")
        assertEquals(4, filtered.size)
        assert(filtered.any { it.name == "California Pleading" })
        assert(filtered.any { it.name == "California Answer" })
    }

    @Test
    fun `filterTemplates with Federal court returns correct templates including answer`() {
        val templatesWithFederal = templates +
            Template(id = "9", name = "Federal Answer", description = "", content = "", authorName = "", authorEmail = "", court = "Federal")
        val filtered = filterTemplates(templatesWithFederal, "Federal")
        assertEquals(4, filtered.size)
        assert(filtered.any { it.name == "Federal Pleading" })
        assert(filtered.any { it.name == "Federal Answer" })
    }

    @Test
    fun `filterTemplates with Federal court returns correct templates including motion`() {
        val templatesWithFederal = templates +
            Template(id = "9", name = "Federal Answer", description = "", content = "", authorName = "", authorEmail = "", court = "Federal") +
            Template(id = "10", name = "Federal Motion to Dismiss", description = "", content = "", authorName = "", authorEmail = "", court = "Federal")
        val filtered = filterTemplates(templatesWithFederal, "Federal")
        assertEquals(5, filtered.size)
        assert(filtered.any { it.name == "Federal Pleading" })
        assert(filtered.any { it.name == "Federal Answer" })
        assert(filtered.any { it.name == "Federal Motion to Dismiss" })
    }

    @Test
    fun `filterTemplates with California court returns correct templates including motion`() {
        val templatesWithCalifornia = templates +
            Template(id = "8", name = "California Answer", description = "", content = "", authorName = "", authorEmail = "", court = "California") +
            Template(id = "11", name = "California Motion to Dismiss", description = "", content = "", authorName = "", authorEmail = "", court = "California")
        val filtered = filterTemplates(templatesWithCalifornia, "California")
        assertEquals(5, filtered.size)
        assert(filtered.any { it.name == "California Pleading" })
        assert(filtered.any { it.name == "California Answer" })
        assert(filtered.any { it.name == "California Motion to Dismiss" })
    }

    @Test
    fun `filterTemplates with Texas court returns correct templates including motion`() {
        val templatesWithTexas = templates +
            Template(id = "7", name = "Texas Answer", description = "", content = "", authorName = "", authorEmail = "", court = "Texas") +
            Template(id = "12", name = "Texas Motion to Dismiss", description = "", content = "", authorName = "", authorEmail = "", court = "Texas")
        val filtered = filterTemplates(templatesWithTexas, "Texas")
        assertEquals(4, filtered.size)
        assert(filtered.any { it.name == "Texas Answer" })
        assert(filtered.any { it.name == "Texas Motion to Dismiss" })
    }

    @Test
    fun `filterTemplates with Florida court returns correct templates`() {
        val templatesWithFlorida = templates +
            Template(id = "13", name = "Florida Complaint", description = "", content = "", authorName = "", authorEmail = "", court = "Florida")
        val filtered = filterTemplates(templatesWithFlorida, "Florida")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "Florida Complaint" })
    }

    @Test
    fun `filterTemplates with Illinois court returns correct templates`() {
        val templatesWithIllinois = templates +
            Template(id = "14", name = "Illinois Complaint", description = "", content = "", authorName = "", authorEmail = "", court = "Illinois")
        val filtered = filterTemplates(templatesWithIllinois, "Illinois")
        assertEquals(3, filtered.size)
        assert(filtered.any { it.name == "Illinois Complaint" })
    }
}
