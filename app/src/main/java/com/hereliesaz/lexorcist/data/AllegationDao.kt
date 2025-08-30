package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AllegationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(allegation: Allegation)

    @Query("SELECT * FROM allegations WHERE caseId = :caseId ORDER BY id DESC")
    @Query("SELECT * FROM allegations WHERE caseId = :caseId")
    fun getAllegationsForCase(caseId: Int): Flow<List<Allegation>>

    @Query("DELETE FROM allegations WHERE id = :id")
    suspend fun deleteAllegationById(id: Int)
}
