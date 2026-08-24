package com.hereliesaz.lexorcist.documents

/**
 * A fillable court document.
 *
 * @property id stable identifier, used to remember a selection.
 * @property name what the user sees.
 * @property jurisdiction the court family it is drawn for, or null when general.
 * @property rawResourceName the `res/raw` entry holding the HTML, for bundled
 *   templates; null for one supplied by the user or downloaded from Extras.
 * @property html the template body, when it did not come from a resource.
 */
data class DocumentTemplate(
    val id: String,
    val name: String,
    val jurisdiction: String? = null,
    val rawResourceName: String? = null,
    val html: String? = null,
)

/**
 * The placeholders a template may contain.
 *
 * Named rather than free-form so that [TemplateFiller] can report which ones a
 * given template needs and which of those the app cannot fill, instead of
 * silently emitting a document with `{{ATTORNEY_BAR_NUMBER}}` printed in it and
 * letting someone file it.
 */
object Placeholder {
    const val PATTERN = """\{\{([A-Z0-9_]+)\}\}"""

    // Case
    const val CASE_NUMBER = "CASE_NUMBER"
    const val DOCKET_NUMBER = "DOCKET_NUMBER"
    const val INDEX_NUMBER = "INDEX_NUMBER"
    const val PLAINTIFF_NAMES = "PLAINTIFF_NAMES"
    const val PLAINTIFFS = "PLAINTIFFS"
    const val DEFENDANT_NAMES = "DEFENDANT_NAMES"
    const val COURT_NAME = "COURT_NAME"

    // Exhibit
    const val EXHIBIT_NUMBER = "EXHIBIT_NUMBER"
    const val EXHIBIT_NAME = "EXHIBIT_NAME"
    const val EXHIBIT_DATE = "EXHIBIT_DATE"
    const val TABLE_OF_EXHIBITS = "TABLE_OF_EXHIBITS"

    // Author, from Settings
    const val ATTORNEY_NAME = "ATTORNEY_NAME"
    const val ATTORNEY_EMAIL = "ATTORNEY_EMAIL"
    const val AFFIANT_NAME = "AFFIANT_NAME"

    // Dates
    const val DATE = "DATE"
    const val CURRENT_DATE = "CURRENT_DATE"
    const val STAMP_DATE = "STAMP_DATE"
}
