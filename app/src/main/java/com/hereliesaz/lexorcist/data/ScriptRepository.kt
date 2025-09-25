package com.hereliesaz.lexorcist.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.model.Script
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val scriptsFile: File by lazy { File(context.filesDir, "scripts.json") }

    suspend fun getScripts(): List<Script> = withContext(Dispatchers.IO) {
        if (scriptsFile.exists()) {
            val json = scriptsFile.readText()
            gson.fromJson(json, object : TypeToken<List<Script>>() {}.type)
        } else {
            emptyList()
        }
    }

    suspend fun saveScripts(scripts: List<Script>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(scripts)
        scriptsFile.writeText(json)
    }
}