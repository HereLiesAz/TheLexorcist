package com.hereliesaz.lexorcist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FinancialEntryDao {
    @Insert
    suspend fun insert(entry: FinancialEntry)

    @Query("SELECT * FROM financial_entries WHERE documentDate BETWEEN :startDate AND :endDate ORDER BY " +
            "CASE WHEN :sortOrder = 'DATE_ASC' THEN documentDate END ASC, " +
            "CASE WHEN :sortOrder = 'DATE_DESC' THEN documentDate END DESC, " +
            "CASE WHEN :sortOrder = 'AMOUNT_ASC' THEN CAST(REPLACE(REPLACE(amount, '$', ''), ',', '') AS REAL) END ASC, " +
            "CASE WHEN :sortOrder = 'AMOUNT_DESC' THEN CAST(REPLACE(REPLACE(amount, '$', ''), ',', '') AS REAL) END DESC")
    suspend fun getEntries(sortOrder: SortOrder, startDate: Long, endDate: Long): List<FinancialEntry>
}
