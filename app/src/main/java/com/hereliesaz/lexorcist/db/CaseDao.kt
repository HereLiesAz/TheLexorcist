package com.hereliesaz.lexorcist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM cases")
    fun getAllCases(): Flow<List<Case>>

    @Insert
    suspend fun insert(case: Case)
}
