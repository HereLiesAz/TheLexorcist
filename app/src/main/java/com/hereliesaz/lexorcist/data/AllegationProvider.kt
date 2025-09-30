package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

// AllegationCatalogEntry is now defined in Allegation.kt

object AllegationProvider {
    private var allegations: List<AllegationCatalogEntry> = emptyList()
    private var evidenceCatalog: EvidenceCatalogRoot? = null

    fun loadAllegations(context: Context) {
        if (allegations.isEmpty()) {
            try {
                context.assets.open("allegations_catalog.json").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val gson = Gson()
                        val allegationListType = object : TypeToken<List<AllegationCatalogEntry>>() {}.type
                        allegations = gson.fromJson(reader, allegationListType)
                        Log.i("AllegationProvider", "Successfully loaded ${allegations.size} allegation catalog entries.")
                    }
                }
            } catch (e: Exception) {
                Log.e("AllegationProvider", "Failed to load allegations_catalog.json", e)
            }
        }

        if (evidenceCatalog == null) {
            try {
                context.assets.open("evidence_catalog.json").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val gson = Gson()
                        evidenceCatalog = gson.fromJson(reader, EvidenceCatalogRoot::class.java)
                        val count = evidenceCatalog?.allegationEvidenceCatalog?.civilAllegations?.size ?: 0 +
                                    (evidenceCatalog?.allegationEvidenceCatalog?.criminalAllegations?.size ?: 0)
                        Log.i("AllegationProvider", "Successfully loaded $count evidence catalog entries.")
                    }
                }
            } catch (e: Exception) {
                Log.e("AllegationProvider", "Failed to load or parse evidence_catalog.json", e)
            }
        }
    }

    fun getAllegationById(id: Int): AllegationCatalogEntry? {
        val idString = id.toString()
        val paddedIdString = id.toString().padStart(3, '0')
        return allegations.find { it.id == idString || it.id == paddedIdString }
    }

    fun getEvidenceCatalogForAllegation(allegationName: String): EvidenceCatalogEntry? {
        val allAllegations = (evidenceCatalog?.allegationEvidenceCatalog?.civilAllegations ?: emptyList()) +
                               (evidenceCatalog?.allegationEvidenceCatalog?.criminalAllegations ?: emptyList())
        return allAllegations.find { it.allegationName.equals(allegationName, ignoreCase = true) }
    }

    fun getAllLoadedCatalogEntries(): List<AllegationCatalogEntry> {
        return allegations
    }
}