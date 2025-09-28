package com.hereliesaz.lexorcist.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

data class AllegationCatalogEntry(
    val id: String,
    val name: String,
    val relevant_evidence: Map<String, List<String>>
)

object AllegationProvider {
    private var allegations: List<AllegationCatalogEntry> = emptyList()

    fun loadAllegations(context: Context) {
        if (allegations.isNotEmpty()) return

        try {
            val jsonString = context.assets.open("allegations_catalog.json").use {
                InputStreamReader(it).readText()
            }

            // Clean the malformed JSON string
            val cleanedJson = jsonString
                // Replace invalid empty values like "key":, with "key":[]
                .replace(Regex(":\\s*,"), ":[]")
                .replace(Regex(":\\s*}"), ":[]}")
                // Remove the corrupted segment between civil and criminal allegations
                .replace(Regex("],\\s*\"criminal_allegations\":,"), ",")
                .replace(Regex("],\n\\\"criminal_allegations\":,"), ",")

            // Wrap the stream of objects into a valid JSON array
            val jsonArrayString = "[$cleanedJson]"
                // Remove any trailing comma before the final closing bracket
                .replace(Regex(",\\s*]"), "]")

            val gson = Gson()
            // Use a generic list of maps to avoid parsing errors due to the heterogeneous objects
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(jsonArrayString, listType)

            // Map the raw list to our data class, ignoring objects that don't match
            val catalogEntries = rawList.mapNotNull { item ->
                if (item.containsKey("id") && item.containsKey("allegationName")) {
                    try {
                        val id = item["id"] as String
                        val name = item["allegationName"] as String
                        val evidenceMap = item["relevant_evidence"] as? Map<String, List<String>> ?: emptyMap()
                        AllegationCatalogEntry(id, name, evidenceMap)
                    } catch (e: Exception) {
                        Log.w("AllegationProvider", "Skipping invalid allegation entry: $item", e)
                        null
                    }
                } else {
                    null // Ignore the description object and any other malformed entries
                }
            }

            allegations = catalogEntries
            Log.i("AllegationProvider", "Successfully loaded ${allegations.size} allegation catalog entries.")

        } catch (e: Exception) {
            Log.e("AllegationProvider", "Failed to load or parse allegations_catalog.json", e)
        }
    }

    fun getAllegationById(id: Int): AllegationCatalogEntry? {
        // The ID in the JSON is a string, sometimes with leading zeros, so we'll compare it as a string.
        val idString = id.toString().padStart(3, '0')
        return allegations.find { it.id == idString || it.id == id.toString() }
    }

    fun getAllLoadedCatalogEntries(): List<AllegationCatalogEntry> {
        return allegations
    }
}