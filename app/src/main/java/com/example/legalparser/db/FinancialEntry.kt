package com.example.legalparser.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_entries")
data class FinancialEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: String,
    val timestamp: Long
)
