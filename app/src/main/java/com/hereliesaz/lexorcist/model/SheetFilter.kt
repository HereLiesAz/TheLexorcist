package com.hereliesaz.lexorcist.model

/**
 * Represents a filter to be applied to a Google Sheet.
 *
 * This data class defines a simple key-value pair for filtering data.
 *
 * @property name The name of the filter (e.g., "Status", "Assignee").
 * @property value The value to filter by (e.g., "Open", "John Doe").
 */
data class SheetFilter(
    val name: String,
    val value: String,
)
