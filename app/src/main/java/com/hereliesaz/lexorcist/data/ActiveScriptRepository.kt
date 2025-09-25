package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveScriptRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences("active_scripts", Context.MODE_PRIVATE)
    private val _activeScriptIds = MutableStateFlow<List<String>>(emptyList())
    val activeScriptIds = _activeScriptIds.asStateFlow()

    init {
        _activeScriptIds.value = getActiveScriptIdsFromPrefs()
    }

    private fun getActiveScriptIdsFromPrefs(): List<String> {
        val json = prefs.getString("active_scripts_order", "[]")
        return com.google.gson.Gson().fromJson(json, Array<String>::class.java).toList()
    }

    fun toggleActiveScript(scriptId: String) {
        val currentIds = _activeScriptIds.value.toMutableList()
        if (currentIds.contains(scriptId)) {
            currentIds.remove(scriptId)
        } else {
            currentIds.add(scriptId)
        }
        saveActiveScriptIds(currentIds)
    }

    fun reorderActiveScripts(from: Int, to: Int) {
        val currentIds = _activeScriptIds.value.toMutableList()
        val item = currentIds.removeAt(from)
        currentIds.add(to, item)
        saveActiveScriptIds(currentIds)
    }

    private fun saveActiveScriptIds(ids: List<String>) {
        val json = com.google.gson.Gson().toJson(ids)
        prefs.edit {
            putString("active_scripts_order", json)
        }
        _activeScriptIds.value = ids
    }
}