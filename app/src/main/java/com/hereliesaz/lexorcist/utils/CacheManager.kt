package com.hereliesaz.lexorcist.utils // Corrected package

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.Case
import java.io.File

class CacheManager(private val context: Context) {

    private val gson = Gson()
    private val caseCacheFile = File(context.cacheDir, "case_cache.json")

    fun saveCases(cases: List<Case>) {
        val json = gson.toJson(cases)
        caseCacheFile.writeText(json)
    }

    fun loadCases(): List<Case>? {
        if (!caseCacheFile.exists()) {
            return null
        }
        val json = caseCacheFile.readText()
        val type = object : TypeToken<List<Case>>() {}.type
        return gson.fromJson(json, type)
    }
}
