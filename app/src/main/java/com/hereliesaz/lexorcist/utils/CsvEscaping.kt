package com.hereliesaz.lexorcist.utils

/**
 * Parsing for the seed CSV files under `app/src/main/assets`.
 *
 * Both seed loaders -- `ScriptRepository.loadDefaultScripts` and
 * `DefaultExtrasSeeder.loadDefaultScriptsFromCsv` -- pulled fields out of a row
 * with the regex `"(.*?)"` and used the captured text verbatim. That is not a
 * CSV parser, and the seed file needs one. Two independent consequences, both
 * of which made the shipped script library non-functional:
 *
 *  1. **Embedded quotes truncated every script.** The file follows RFC 4180 and
 *     writes a literal `"` inside a field as `""`. A non-greedy `"(.*?)"` stops
 *     at the first one, so `const curses = [""f***"", ...]` was cut down to
 *     `const curses = [` -- a syntax error. Nearly every script contains a
 *     string literal, so nearly every script was mangled.
 *  2. **Escaped newlines reached the engine literally.** Multi-line bodies are
 *     stored on one physical line with newlines written as the two characters
 *     `\` and `n`. Passed through unchanged, Rhino rejected them with
 *     `illegal character: \`.
 *
 * Neither was ever noticed because nothing parsed or executed these scripts.
 */

/**
 * Splits one RFC 4180 record into its fields.
 *
 * Handles quoted fields, commas inside quotes, and a doubled `""` standing for
 * one literal quote. Unquoted fields are returned trimmed.
 */
fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i += 2
            }
            c == '"' -> {
                inQuotes = !inQuotes
                i++
            }
            !inQuotes && c == ',' -> {
                fields += current.toString()
                current.clear()
                i++
            }
            else -> {
                current.append(c)
                i++
            }
        }
    }
    fields += current.toString()
    return fields
}

/**
 * Resolves the backslash escapes the seed files use inside a field.
 *
 * A single left-to-right pass, so an escaped backslash cannot be re-read as the
 * start of another escape: `\\n` yields a backslash followed by `n` (a regex
 * word boundary in the resulting script source), while `\n` yields an actual
 * newline. Sequences this does not own are preserved verbatim, so regex escapes
 * written with a single backslash survive.
 */
fun String.unescapeCsvField(): String {
    if ('\\' !in this) return this

    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c != '\\' || i == lastIndex) {
            out.append(c)
            i++
            continue
        }
        when (val next = this[i + 1]) {
            'n' -> out.append('\n')
            'r' -> out.append('\r')
            't' -> out.append('\t')
            '"' -> out.append('"')
            '\'' -> out.append('\'')
            '\\' -> out.append('\\')
            else -> out.append('\\').append(next)
        }
        i += 2
    }
    return out.toString()
}
