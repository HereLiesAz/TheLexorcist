package com.hereliesaz.lexorcist.data.repository

import android.content.Context
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.AllegationCatalogEntry
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalRepository @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Loads and merges allegation data from two sources:
     * 1.  `master_allegations.json` (from `res/raw`): Contains descriptive data (type, category, description).
     * 2.  `allegations_catalog.json` (from `assets`): Contains the canonical `id` and `allegationName`.
     * It combines them into a single, complete list of `MasterAllegation` objects.
     */
    fun getMasterAllegations(): Flow<List<MasterAllegation>> = flow {
        try {
            // 1. Load descriptive data from res/raw/master_allegations.json
            val masterInputStream = context.resources.openRawResource(R.raw.master_allegations)
            val masterReader = InputStreamReader(masterInputStream)
            val masterListType = object : TypeToken<List<MasterAllegation>>() {}.type
            val masterAllegations: List<MasterAllegation> = Gson().fromJson(masterReader, masterListType)

            // 2. Load catalog data (with IDs) from assets/allegations_catalog.json
            val catalogInputStream = context.assets.open("allegations_catalog.json")
            val catalogReader = InputStreamReader(catalogInputStream)
            val catalogListType = object : TypeToken<List<AllegationCatalogEntry>>() {}.type
            val allegationCatalog: List<AllegationCatalogEntry> = Gson().fromJson(catalogReader, catalogListType)

            // Create a lookup map from the catalog: allegationName -> id
            val idMap = allegationCatalog.associateBy { it.allegationName }

            // 3. Merge the two lists
            val mergedAllegations = masterAllegations.map { master ->
                val catalogEntry = idMap[master.name]
                master.copy(
                    id = catalogEntry?.id // Assign the ID from the catalog
                )
            }.filter { it.id != null } // Ensure we only emit allegations that have a valid ID

            emit(mergedAllegations)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList()) // Emit an empty list on error
        }
    }
}