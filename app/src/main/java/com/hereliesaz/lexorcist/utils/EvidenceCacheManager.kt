package com.hereliesaz.lexorcist.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.Evidence
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Added Singleton annotation
class EvidenceCacheManager
    @Inject
    constructor( // Added @Inject
        // Changed here
        @param:ApplicationContext private val context: Context,
    ) {
        private val gson = Gson()

        private fun getCacheFile(caseId: Long): File = File(context.cacheDir, "evidence_cache_$caseId.json")

        fun saveEvidence(
            caseId: Long,
            evidence: List<Evidence>,
        ) {
            val json = gson.toJson(evidence)
            getCacheFile(caseId).writeText(json)
        }

        fun loadEvidence(caseId: Long): List<Evidence>? {
            val cacheFile = getCacheFile(caseId)
            if (!cacheFile.exists()) {
                return null
            }
            val json = cacheFile.readText()
            val type = object : TypeToken<List<Evidence>>() {}.type
            return gson.fromJson(json, type)
        }
    }
