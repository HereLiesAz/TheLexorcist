package com.hereliesaz.lexorcist.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cases")
data class Case(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val spreadsheetId: String
)
