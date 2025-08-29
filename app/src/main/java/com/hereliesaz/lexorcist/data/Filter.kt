package com.hereliesaz.lexorcist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filters")
data class Filter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val name: String,
    val value: String
)
