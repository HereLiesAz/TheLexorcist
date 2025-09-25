package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "script_state")

@Singleton
class ScriptStateRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val SCRIPT_STATE_KEY = stringSetPreferencesKey("script_state")

    val scriptState: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SCRIPT_STATE_KEY] ?: emptySet()
        }

    suspend fun addScriptState(evidenceId: Int, scriptHash: Int) {
        context.dataStore.edit { settings ->
            val currentSet = settings[SCRIPT_STATE_KEY] ?: emptySet()
            settings[SCRIPT_STATE_KEY] = currentSet + "${evidenceId}:${scriptHash}"
        }
    }

    suspend fun clearScriptState(scriptHash: Int) {
        context.dataStore.edit { settings ->
            val currentSet = settings[SCRIPT_STATE_KEY] ?: emptySet()
            settings[SCRIPT_STATE_KEY] = currentSet.filter { !it.endsWith(":${scriptHash}") }.toSet()
        }
    }
}