package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create the DataStore singleton.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "script_state")

/**
 * Repository for managing the execution state of scripts on evidence.
 *
 * This repository uses Jetpack DataStore to persist a set of "executed scripts".
 * This allows the UI (specifically menus) to know which scripts have already been applied
 * to a specific piece of evidence, preventing re-execution or enabling toggle-like behavior.
 */
@Singleton
class ScriptStateRepository @Inject constructor(@ApplicationContext private val context: Context) {

    // Key for storing the set of strings in the format "evidenceId:scriptId".
    private val SCRIPT_STATE_KEY = stringSetPreferencesKey("script_state")

    /**
     * A flow emitting the current set of executed script markers.
     * Each string in the set follows the format "evidenceId:scriptId".
     */
    val scriptState = context.dataStore.data.map { preferences ->
        preferences[SCRIPT_STATE_KEY] ?: emptySet()
    }

    /**
     * Marks a script as executed for a specific evidence item.
     *
     * @param evidenceId The ID of the evidence.
     * @param scriptId The ID of the script.
     */
    suspend fun addScriptState(evidenceId: Int, scriptId: String) {
        context.dataStore.edit { settings ->
            val currentState = settings[SCRIPT_STATE_KEY] ?: emptySet()
            // Append the new state marker.
            settings[SCRIPT_STATE_KEY] = currentState + "$evidenceId:$scriptId"
        }
    }

    /**
     * Clears the execution state for a specific script across all evidence.
     * Useful when a script is modified or reset.
     *
     * @param scriptId The ID of the script to clear state for.
     */
    suspend fun clearScriptState(scriptId: String) {
        context.dataStore.edit { settings ->
            val currentState = settings[SCRIPT_STATE_KEY] ?: emptySet()
            // Remove all entries matching the scriptId.
            settings[SCRIPT_STATE_KEY] = currentState.filter { !it.endsWith(":$scriptId") }.toSet()
        }
    }
}
