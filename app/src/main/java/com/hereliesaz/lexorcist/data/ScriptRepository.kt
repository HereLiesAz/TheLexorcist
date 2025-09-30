package com.hereliesaz.lexorcist.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.model.Script
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
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
        // Using model.Script here because that's what the rest of the app seems to use when saving/loading scripts.
        return gson.fromJson(json, object : TypeToken<List<com.hereliesaz.lexorcist.model.Script>>() {}.type)
    }

    fun saveScripts(scripts: List<com.hereliesaz.lexorcist.model.Script>) {
        val json = gson.toJson(scripts)
        scriptsFile.writeText(json)
    }

    private fun loadDefaultScripts(): List<com.hereliesaz.lexorcist.model.Script> {
        val defaultScripts = mutableListOf<com.hereliesaz.lexorcist.model.Script>()
        try {
            context.assets.open("default_scripts.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header line
                    reader.readLine()

                    val regex = "\"(.*?)\"".toRegex()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tokens = regex.findAll(line!!).map { it.groupValues[1] }.toList()

                        if (tokens.size >= 5) {
                            val script = com.hereliesaz.lexorcist.model.Script(
                                id = tokens[0],
                                name = tokens[1],
                                author = tokens[2],
                                description = tokens[3],
                                content = tokens[4],
                                authorName = "", // author field now contains both
                                authorEmail = "" // author field now contains both
                            )
                            defaultScripts.add(script)
                        }
                    }
                }
            }
            saveScripts(defaultScripts)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaultScripts
    }
}