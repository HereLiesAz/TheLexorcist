package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {
    @Query("SELECT * FROM filters WHERE caseId = :caseId")
    fun getFiltersForCase(caseId: Int): Flow<List<Filter>>

    @Insert
    suspend fun insert(filter: Filter)
}
