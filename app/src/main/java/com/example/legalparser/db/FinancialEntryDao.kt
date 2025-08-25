package com.example.legalparser.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FinancialEntryDao {
    @Insert
    suspend fun insert(entry: FinancialEntry)

    @Query("SELECT * FROM financial_entries ORDER BY timestamp DESC")
    suspend fun getAllEntries(): List<FinancialEntry>
}
