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
            val jsonString = context.assets.open("allegations_catalog.json").use {
                InputStreamReader(it).readText()
            }

            val cleanedJson = jsonString
                .replace(Regex(":\\s*,"), ":[]")
                .replace(Regex(":\\s*}"), ":[]}")
                .replace(Regex("],\\s*\"criminal_allegations\":,"), ",")
                .replace(Regex("],\n\\\"criminal_allegations\":,"), ",")

            val jsonArrayString = "[$cleanedJson]"
                .replace(Regex(",\\s*]"), "]")

            val gson = Gson()
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(jsonArrayString, listType)

            val catalogEntries = rawList.mapNotNull { item ->
                // Ensure we are using "allegationName" as per the definition in Allegation.kt
                if (item.containsKey("id") && item.containsKey("allegationName")) {
                    try {
                        val id = item["id"] as String
                        // Use "allegationName" here
                        val allegationName = item["allegationName"] as String 
                        val evidenceMap = item["relevant_evidence"] as? Map<String, List<String>> ?: emptyMap()
                        // Construct AllegationCatalogEntry using "allegationName"
                        AllegationCatalogEntry(id, allegationName, evidenceMap) 
                    } catch (e: Exception) {
                        Log.w("AllegationProvider", "Skipping invalid allegation entry: $item", e)
                        null
                    }
                } else {
                    null
                }
            }

            allegations = catalogEntries
            Log.i("AllegationProvider", "Successfully loaded ${allegations.size} allegation catalog entries.")

        } catch (e: Exception) {
            Log.e("AllegationProvider", "Failed to load or parse allegations_catalog.json", e)
        }
    }

    fun getAllegationById(id: Int): AllegationCatalogEntry? {
        val idString = id.toString().padStart(3, '0')
        return allegations.find { it.id == idString || it.id == id.toString() }
    }

    fun getAllLoadedCatalogEntries(): List<AllegationCatalogEntry> {
        return allegations
    }
}