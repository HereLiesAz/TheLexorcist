package com.hereliesaz.lexorcist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo


/**
 * Represents a single allegation within a legal case.
 *
 * This data class stores the details of an allegation, including its text and
 * the case it belongs to.
 *
 * @property id The unique identifier for the allegation.
import androidx.room.Entity
import androidx.room.PrimaryKey

 * @property caseId The ID of the case this allegation belongs to.
 * @property text The text of the allegation.
 */
@Entity(tableName = "allegations")
data class Allegation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "caseId")
    val caseId: Int,
    @ColumnInfo(name = "text")
    val text: String
)
