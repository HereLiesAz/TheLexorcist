package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(case: Case)

    @Query("SELECT * FROM `case` ORDER BY id DESC")
    fun getAllCases(): Flow<List<Case>>

    @Query("SELECT * FROM `case` WHERE id = :id")
    suspend fun getCaseById(id: Int): Case?

    @Query("DELETE FROM `case` WHERE id = :id")
    suspend fun deleteCaseById(id: Int)
}
