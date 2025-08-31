package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(case: Case): Long

    @Query("SELECT * FROM cases ORDER BY name ASC")
    fun getAllCases(): Flow<List<Case>>

    @Query("SELECT * FROM cases WHERE spreadsheetId = :spreadsheetId")
    suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Case?

    @Query("SELECT * FROM cases WHERE id = :id")
    suspend fun getCaseById(id: Int): Case?

    @Update
    suspend fun update(case: Case)

    @Delete
    suspend fun delete(case: Case)
}
