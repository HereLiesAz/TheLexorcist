package com.hereliesaz.lexorcist.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import com.hereliesaz.lexorcist.model.SelectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectionRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
    private val gson: Gson
) : SelectionRepository {

    private val _selectionState = MutableStateFlow(loadSelectionState())
    override val selectionState: Flow<SelectionState> = _selectionState

    private fun loadSelectionState(): SelectionState {
        val json = prefs.getString(KEY_SELECTION_STATE, null)
        return if (json != null) {
            gson.fromJson(json, SelectionState::class.java)
        } else {
            SelectionState()
        }
    }

    private suspend fun saveSelectionState(newState: SelectionState) = withContext(Dispatchers.IO) {
        val json = gson.toJson(newState)
        prefs.edit().putString(KEY_SELECTION_STATE, json).apply()
        _selectionState.value = newState
    }

    override suspend fun selectCase(caseId: String?) {
        val currentState = _selectionState.value
        if (currentState.selectedCaseId != caseId) {
            saveSelectionState(currentState.copy(selectedCaseId = caseId))
        }
    }

    override suspend fun selectExhibit(exhibitId: String?) {
        val currentState = _selectionState.value
        if (currentState.selectedExhibitId != exhibitId) {
            saveSelectionState(currentState.copy(selectedExhibitId = exhibitId))
        }
    }

    companion object {
        private const val KEY_SELECTION_STATE = "selection_state"
    }
}