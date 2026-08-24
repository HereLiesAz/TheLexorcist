package com.hereliesaz.lexorcist.documents

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The document templates available to generate from.
 *
 * The app has shipped eighteen court templates in `res/raw` since early on and
 * never used one. "Generate Document" instead listed HTML files from the user's
 * Google Drive, via `CaseRepositoryImpl.refreshHtmlTemplates`, whose entire
 * body was `{ /* TODO */ }`. The list was therefore always empty, no template
 * could be selected, and the Generate button did nothing.
 *
 * Even with a selection it would not have produced anything: generation called
 * `lex.google.runAppsScript(case.scriptId, ...)` behind
 * `val scriptId = currentCase.scriptId ?: return@launch`, and `scriptId` is set
 * to null by the only case parser that runs. The path was dead at both ends.
 *
 * Templates now come from the bundled resources, and generation happens on the
 * device -- which also means it works offline and without a Google account, as
 * the rest of the app does.
 */
@Singleton
class TemplateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Every bundled template, ordered by jurisdiction then name. */
    fun bundled(): List<DocumentTemplate> = BUNDLED

    fun htmlFor(template: DocumentTemplate): String {
        template.html?.let { return it }
        val resourceName = template.rawResourceName
            ?: error("Template ${template.id} has neither HTML nor a resource")
        val id = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        require(id != 0) { "Missing template resource $resourceName" }
        return context.resources.openRawResource(id).bufferedReader().use { it.readText() }
    }

    private companion object {
        private fun t(resource: String, name: String, jurisdiction: String?) = DocumentTemplate(
            id = resource,
            name = name,
            jurisdiction = jurisdiction,
            rawResourceName = resource,
        )

        val BUNDLED: List<DocumentTemplate> = listOf(
            t("template_cover_sheet", "Cover Sheet", null),
            t("template_declaration", "Declaration of Records Custodian", null),
            t("template_table_of_exhibits", "Table of Exhibits", null),
            t("template_custody_log", "Chain of Custody Log", null),
            t("template_metadata", "Evidence Metadata Sheet", null),

            t("template_federal_complaint", "Complaint", "Federal"),
            t("template_federal_answer", "Answer", "Federal"),
            t("template_federal_motion_to_dismiss", "Motion to Dismiss", "Federal"),

            t("template_california_complaint", "Complaint", "California"),
            t("template_california_answer", "Answer", "California"),
            t("template_california_motion_to_dismiss", "Motion to Dismiss", "California"),

            t("template_texas_complaint", "Petition", "Texas"),
            t("template_texas_answer", "Answer", "Texas"),
            t("template_texas_motion_to_dismiss", "Motion to Dismiss", "Texas"),

            t("template_florida_complaint", "Complaint", "Florida"),
            t("template_illinois_complaint", "Complaint", "Illinois"),
            t("template_louisiana_complaint", "Petition", "Louisiana"),
            t("template_new_york_complaint", "Complaint", "New York"),
        )
    }
}
