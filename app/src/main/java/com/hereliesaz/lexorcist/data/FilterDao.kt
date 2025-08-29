package com.hereliesaz.lexorcist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the [Filter] entity.
 *
 * This interface defines the database operations for filters, such as retrieving and inserting them.
 */
@Dao
interface FilterDao {
    /**
     * Retrieves all filters for a given case as a [Flow].
     *
     * @param caseId The ID of the case to retrieve filters for.
     * @return A [Flow] that emits a list of [Filter] objects.
     */
    @Query("SELECT * FROM filters WHERE caseId = :caseId")
    fun getFiltersForCase(caseId: Int): Flow<List<Filter>>

    /**
     * Inserts a new filter into the database.
     *
     * @param filter The [Filter] object to insert.
     */
    @Insert
    suspend fun insert(filter: Filter)
}
