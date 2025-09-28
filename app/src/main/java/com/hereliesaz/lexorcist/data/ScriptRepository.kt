package com.hereliesaz.lexorcist.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.model.Script
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val scriptsFile = File(context.filesDir, "scripts.json")
    private val gson = Gson()

    fun getScripts(): List<Script> {
        if (!scriptsFile.exists()) {
            return loadDefaultScripts()
        }
        val json = scriptsFile.readText()
        return gson.fromJson(json, object : TypeToken<List<Script>>() {}.type)
    }

    fun saveScripts(scripts: List<Script>) {
        val json = gson.toJson(scripts)
        scriptsFile.writeText(json)
    }

    private fun loadDefaultScripts(): List<Script> {
        return try {
            context.assets.open("default_extras.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val extrasType = object : TypeToken<Map<String, List<Any>>>() {}.type
                    val extrasMap: Map<String, List<Any>> = gson.fromJson(reader, extrasType)

                    val scriptsJson = gson.toJson(extrasMap["scripts"])
                    val scriptType = object : TypeToken<List<Script>>() {}.type
                    val defaultScripts: List<Script> = gson.fromJson(scriptsJson, scriptType)

                    saveScripts(defaultScripts)
                    defaultScripts
                }
            }
        } catch (e: Exception) {
            // Log the error
            e.printStackTrace()
            emptyList()
        }
    }
}