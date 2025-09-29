package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

// AllegationCatalogEntry is now defined in Allegation.kt

object AllegationProvider {
    private var allegations: List<AllegationCatalogEntry> = emptyList()

    fun loadAllegations(context: Context) {
        if (allegations.isNotEmpty()) return

        try {
            context.assets.open("allegations_catalog.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val gson = Gson()
                    val allegationListType = object : TypeToken<List<AllegationCatalogEntry>>() {}.type
                    val loadedAllegations: List<AllegationCatalogEntry> = gson.fromJson(reader, allegationListType)
                    allegations = loadedAllegations
                    Log.i("AllegationProvider", "Successfully loaded ${allegations.size} allegation catalog entries.")
                }
            }
        } catch (e: Exception) {
            Log.e("AllegationProvider", "Failed to load or parse allegations_catalog.json", e)
        }
    }

    fun getAllegationById(id: Int): AllegationCatalogEntry? {
        val idString = id.toString()
        val paddedIdString = id.toString().padStart(3, '0')
        return allegations.find { it.id == idString || it.id == paddedIdString }
    }

    fun getAllLoadedCatalogEntries(): List<AllegationCatalogEntry> {
        return allegations
    }
}