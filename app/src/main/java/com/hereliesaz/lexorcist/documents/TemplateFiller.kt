package com.hereliesaz.lexorcist.documents

/**
 * Substitutes `{{PLACEHOLDER}}` tokens in a template.
 *
 * Pure and free of Android types so the substitution -- the part that decides
 * what appears in a document someone may file with a court -- can be tested
 * directly.
 *
 * Two rules follow from that audience:
 *
 * A value is HTML-escaped. Party names and case numbers are user input, and a
 * defendant named `Smith & Sons <Holdings>` must not silently break the
 * document's markup or vanish from it.
 *
 * A placeholder with no value is not left as `{{ATTORNEY_BAR_NUMBER}}` in the
 * output and is not blanked either. It is rendered as a visible gap and
 * reported in [FilledDocument.unresolved], so the caller can tell the user what
 * still has to be filled in by hand before the document is usable.
 */
object TemplateFiller {

    private val PATTERN = Regex(Placeholder.PATTERN)

    /** Marker left where a value was not available. */
    const val GAP = "__________"

    fun fill(template: String, values: Map<String, String?>): FilledDocument {
        val unresolved = linkedSetOf<String>()
        val filled = PATTERN.replace(template) { match ->
            val key = match.groupValues[1]
            val value = values[key]?.takeIf { it.isNotBlank() }
            if (value == null) {
                unresolved += key
                GAP
            } else {
                escape(value)
            }
        }
        return FilledDocument(filled, unresolved.toList())
    }

    /** Every placeholder a template contains, in first-appearance order. */
    fun placeholdersIn(template: String): List<String> =
        PATTERN.findAll(template).map { it.groupValues[1] }.distinct().toList()

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { c ->
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }
}

/**
 * @property html the substituted document.
 * @property unresolved placeholders the app had no value for, each rendered as
 *   [TemplateFiller.GAP].
 */
data class FilledDocument(
    val html: String,
    val unresolved: List<String>,
)
