package com.hereliesaz.lexorcist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a filter that can be applied to a case.
 *
 * This data class is a Room entity that stores filters in a local database.
 * Filters are used to narrow down the evidence or data displayed to the user.
 *
 * @property id The unique identifier for the filter.
 * @property caseId The ID of the case this filter belongs to.
 * @property name The name of the filter (e.g., "Date Range", "Tag").
 * @property value The value of the filter (e.g., "2023-01-01 to 2023-12-31", "disputed").
 */
@Entity(tableName = "filters")
data class Filter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val name: String,
    val value: String
)
