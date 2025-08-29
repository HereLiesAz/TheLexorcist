package com.hereliesaz.lexorcist.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "evidence")
data class Evidence(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var caseId: Int = 0,
    val content: String, // Added
    val amount: Double?,
    val timestamp: Date,
    val sourceDocument: String,
    val documentDate: Date,
    val allegationId: String?, // Changed to nullable String
    val category: String? = null,
    val tags: List<String>? = null // Added
)
