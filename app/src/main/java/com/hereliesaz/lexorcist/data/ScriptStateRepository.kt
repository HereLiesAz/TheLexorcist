package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "script_state")

@Singleton
class ScriptStateRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val SCRIPT_STATE_KEY = stringSetPreferencesKey("script_state")

    val scriptState = context.dataStore.data.map { preferences ->
        preferences[SCRIPT_STATE_KEY] ?: emptySet()
    }

    suspend fun addScriptState(evidenceId: Int, scriptId: Int) {
        context.dataStore.edit { settings ->
            val currentState = settings[SCRIPT_STATE_KEY] ?: emptySet()
            settings[SCRIPT_STATE_KEY] = currentState + "$evidenceId:$scriptId"
        }
    }

    suspend fun hasScriptRun(evidenceId: Int, scriptId: Int): Boolean {
        val key = "$evidenceId:$scriptId"
        return scriptState.first().contains(key)
    }

    suspend fun clearScriptState(scriptId: Int) {
        context.dataStore.edit { settings ->
            val currentState = settings[SCRIPT_STATE_KEY] ?: emptySet()
            settings[SCRIPT_STATE_KEY] = currentState.filter { !it.endsWith(":$scriptId") }.toSet()
        }
    }
}