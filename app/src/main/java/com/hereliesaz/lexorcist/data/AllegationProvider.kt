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
                if (item.containsKey("id") && item.containsKey("allegationName")) {
                    try {
                        val id = item["id"] as String
                        val allegationName = item["allegationName"] as String

                        val rawEvidenceMap = item["relevant_evidence"]
                        val evidenceMap: Map<String, List<String>> = if (rawEvidenceMap is Map<*, *>) {
                            rawEvidenceMap.entries.mapNotNull { entry ->
                                if (entry.key is String && entry.value is List<*>) {
                                    val key = entry.key as String
                                    val valueList = entry.value as List<*>
                                    if (valueList.all { it is String }) {
                                        @Suppress("UNCHECKED_CAST")
                                        key to (valueList as List<String>)
                                    } else {
                                        Log.w("AllegationProvider", "Skipping invalid evidence list for key '$key' due to non-string elements: $valueList in item: $item")
                                        null
                                    }
                                } else {
                                    Log.w("AllegationProvider", "Skipping invalid evidence entry due to incorrect key/value types: $entry in item: $item")
                                    null
                                }
                            }.toMap()
                        } else {
                            if (rawEvidenceMap != null) {
                                Log.w("AllegationProvider", "Relevant_evidence is not a Map, found: $rawEvidenceMap in item: $item. Using empty map.")
                            }
                            emptyMap<String, List<String>>()
                        }

                        AllegationCatalogEntry(id, allegationName, evidenceMap)
                    } catch (e: Exception) {
                        Log.w("AllegationProvider", "Skipping invalid allegation entry: $item", e)
                        null
                    }
                } else {
                    Log.w("AllegationProvider", "Skipping item due to missing 'id' or 'allegationName': $item")
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