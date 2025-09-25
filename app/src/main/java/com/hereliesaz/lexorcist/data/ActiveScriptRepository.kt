package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveScriptRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val sharedPref = context.getSharedPreferences("active_scripts", Context.MODE_PRIVATE)
    private val _activeScripts = MutableStateFlow<List<String>>(emptyList())
    val activeScripts: StateFlow<List<String>> = _activeScripts.asStateFlow()

    init {
        val savedScripts = sharedPref.getString("active_scripts", "") ?: ""
        _activeScripts.value = if (savedScripts.isNotBlank()) savedScripts.split(",") else emptyList()
    }

    fun toggleActiveScript(scriptId: String) {
        val currentActive = _activeScripts.value.toMutableList()
        if (currentActive.contains(scriptId)) {
            currentActive.remove(scriptId)
        } else {
            currentActive.add(scriptId)
        }
        _activeScripts.value = currentActive
        sharedPref.edit { putString("active_scripts", currentActive.joinToString(",")) }
    }

    fun getActiveScripts(): List<String> {
        return _activeScripts.value
    }

    fun reorderActiveScripts(from: Int, to: Int) {
        val currentActive = _activeScripts.value.toMutableList()
        val item = currentActive.removeAt(from)
        currentActive.add(to, item)
        _activeScripts.value = currentActive
        sharedPref.edit { putString("active_scripts", currentActive.joinToString(",")) }
    }
}