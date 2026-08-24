package com.hereliesaz.lexorcist.documents

import android.content.Context
import android.util.Log
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.data.storage.CaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produces a filled court document from a template, on the device.
 *
 * Writes HTML into the case folder. HTML rather than PDF because it stays
 * editable -- a document with an unfilled placeholder needs completing before
 * it can be used -- and because PDF is better produced by [DocumentPrinter],
 * which hands the finished HTML to Android's own print service. That paginates
 * properly and offers "Save as PDF"; rendering pages here by drawing a WebView
 * onto a canvas would cut text across page boundaries, which is not acceptable
 * in something destined for a court file.
 */
@Singleton
class DocumentGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val templates: TemplateRepository,
    private val settingsManager: SettingsManager,
    private val caseStorage: CaseStorage,
) {

    suspend fun generate(
        case: Case,
        template: DocumentTemplate,
        exhibit: Exhibit?,
        exhibitEvidence: List<Evidence> = emptyList(),
        allExhibits: List<Exhibit> = emptyList(),
    ): GeneratedDocument = withContext(Dispatchers.IO) {
        val filled = TemplateFiller.fill(
            templates.htmlFor(template),
            valuesFor(case, exhibit, exhibitEvidence, allExhibits),
        )

        val dir = File(caseStorage.caseDirectory(case.spreadsheetId), "documents")
            .apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val base = buildString {
            append(template.jurisdiction?.let { "$it " }.orEmpty())
            append(template.name)
            exhibit?.let { append(" - ${it.name}") }
            append(" $stamp")
        }.replace(Regex("[^A-Za-z0-9 \\-]"), "_")

        val htmlFile = File(dir, "$base.html").apply { writeText(filled.html) }

        GeneratedDocument(
            html = htmlFile,
            title = base,
            unresolved = filled.unresolved,
        )
    }

    /**
     * Values for the placeholders the app can actually supply.
     *
     * Deliberately partial. The templates reference court divisions, bar
     * numbers, hearing dates and pleaded paragraphs that the app has never
     * asked anyone for; inventing them would be worse than leaving a gap in a
     * document destined for a court file, so those stay unresolved and are
     * reported to the user.
     */
    private fun valuesFor(
        case: Case,
        exhibit: Exhibit?,
        exhibitEvidence: List<Evidence>,
        allExhibits: List<Exhibit>,
    ): Map<String, String?> {
        val today = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        val author = settingsManager.getAuthorName().takeIf { it.isNotBlank() }
        return buildMap {
            put(Placeholder.CASE_NUMBER, case.name)
            put(Placeholder.DOCKET_NUMBER, case.name)
            put(Placeholder.INDEX_NUMBER, case.name)
            put(Placeholder.PLAINTIFF_NAMES, case.plaintiffs)
            put(Placeholder.PLAINTIFFS, case.plaintiffs)
            put(Placeholder.DEFENDANT_NAMES, case.defendants)
            put(Placeholder.COURT_NAME, case.court)

            put(Placeholder.EXHIBIT_NAME, exhibit?.name)
            put(Placeholder.EXHIBIT_NUMBER, exhibit?.let { extractExhibitNumber(it.name) })
            put(Placeholder.EXHIBIT_DATE, exhibitEvidence.minOfOrNull { it.documentDate }
                ?.takeIf { it > 0L }
                ?.let { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(it)) })
            put(Placeholder.TABLE_OF_EXHIBITS, tableOfExhibits(allExhibits))

            put(Placeholder.ATTORNEY_NAME, author)
            put(Placeholder.AFFIANT_NAME, author)
            put(Placeholder.ATTORNEY_EMAIL, settingsManager.getAuthorEmail().takeIf { it.isNotBlank() })

            put(Placeholder.DATE, today)
            put(Placeholder.CURRENT_DATE, today)
            put(Placeholder.STAMP_DATE, today)
        }
    }

    /** "Exhibit A" -> "A"; anything else is used whole. */
    private fun extractExhibitNumber(name: String): String =
        Regex("""(?:exhibit\s+)?(.+)""", RegexOption.IGNORE_CASE)
            .find(name.trim())?.groupValues?.get(1)?.trim().orEmpty().ifEmpty { name }

    private fun tableOfExhibits(exhibits: List<Exhibit>): String? {
        if (exhibits.isEmpty()) return null
        // Built as markup rather than escaped text, so it is inserted after
        // substitution rather than through it.
        return exhibits.joinToString("") { e ->
            "<tr><td>${e.name.escaped()}</td><td>${e.description.escaped()}</td></tr>"
        }
    }

    private fun String.escaped(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private companion object {
        const val TAG = "DocumentGenerator"
    }
}

/**
 * @property html the generated document, written into the case folder.
 * @property title the document name, used as the print job name.
 * @property unresolved placeholders the app had no value for; each appears in
 *   the document as a blank for the user to complete.
 */
data class GeneratedDocument(
    val html: File,
    val title: String,
    val unresolved: List<String>,
)
