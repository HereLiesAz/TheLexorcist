package com.hereliesaz.lexorcist.data

import android.content.Context
import com.google.gson.Gson
import com.hereliesaz.lexorcist.model.Script
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val scriptsFile = File(context.filesDir, "scripts.json")
    private val gson = Gson()

    fun getScripts(): List<Script> {
        if (!scriptsFile.exists()) {
            return emptyList()
        }
        val json = scriptsFile.readText()
        return gson.fromJson(json, Array<Script>::class.java).toList()
    }

    fun saveScripts(scripts: List<Script>) {
        val json = gson.toJson(scripts)
        scriptsFile.writeText(json)
    }
}