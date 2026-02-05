package com.hereliesaz.lexorcist.utils

/**
 * Utility class for handling spreadsheet-related operations safely.
 */
object SpreadsheetUtils {
    /**
     * Sanitizes strings before writing to the spreadsheet to prevent CSV Injection (Formula Injection).
     *
     * Formula Injection occurs when a spreadsheet program misinterprets a cell starting with a control character
     * (like `=`) as a formula. This can be used to execute commands or exfiltrate data.
     *
     * This function prepends a single quote (`'`) to any string starting with `=`, `+`, `-`, or `@`,
     * forcing the spreadsheet application to treat the content as a string literal.
     *
     * @param value The raw string value.
     * @return The sanitized string safe for writing to a spreadsheet cell.
     */
    fun sanitizeForSpreadsheet(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val firstChar = value[0]
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@') {
            return "'$value"
        }
        return value
    }
}
