package com.hereliesaz.lexorcist.utils

object SpreadsheetUtils {
    /**
     * Sanitizes strings before writing to the spreadsheet to prevent CSV Injection (Formula Injection).
     * If a string starts with =, +, -, or @, it is prepended with a single quote.
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
