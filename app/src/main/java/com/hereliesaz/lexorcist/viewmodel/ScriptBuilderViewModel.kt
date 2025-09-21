package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.data.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.model.Script
import java.io.InputStreamReader

@HiltViewModel
class ScriptBuilderViewModel
    @Inject
    constructor(
        private val settingsManager: SettingsManager,
        private val application: Application,
        private val gson: Gson,
    ) : ViewModel() {
        private val _scriptTitle = MutableStateFlow("")
        val scriptTitle: StateFlow<String> = _scriptTitle.asStateFlow()

        private val _scriptText = MutableStateFlow("")
        val scriptText: StateFlow<String> = _scriptText.asStateFlow()

        private val _caseScripts = MutableStateFlow<List<Script>>(emptyList())
        val caseScripts: StateFlow<List<Script>> = _caseScripts.asStateFlow()

        private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
        val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

        private val _showScriptSelectionDialog = MutableStateFlow(false)
        val showScriptSelectionDialog: StateFlow<Boolean> = _showScriptSelectionDialog.asStateFlow()

        fun openScriptSelectionDialog() {
            _showScriptSelectionDialog.value = true
        }

        fun closeScriptSelectionDialog() {
            _showScriptSelectionDialog.value = false
        }

        fun onScriptsSelected(selectedScripts: List<Script>) {
            val currentScript = _scriptText.value
            val newScripts = selectedScripts.joinToString(separator = "\n\n//---\n\n") {
                "// Imported Script: ${it.name}\n${it.content}"
            }

            val combinedScript = if (currentScript.isNotBlank()) {
                currentScript + "\n\n//---\n\n" + newScripts
            } else {
                newScripts
            }

            _scriptText.value = combinedScript
            closeScriptSelectionDialog()
        }

        init {
            loadSavedScript()
            loadSharedScripts()
        }

        fun onScriptTitleChanged(newText: String) {
            _scriptTitle.value = newText
        }

        fun onScriptTextChanged(newText: String) {
            _scriptText.value = newText
        }

        fun insertText(text: String) {
            _scriptText.value += text
        }

        private fun loadSavedScript() {
            viewModelScope.launch {
                val savedScript = settingsManager.getScript()
                if (savedScript.isBlank()) {
                    val sampleScripts = application.resources.getStringArray(R.array.sample_scripts)
                    _scriptText.value = sampleScripts.random()
                } else {
                    _scriptText.value = savedScript
                }
            }
        }

        private fun loadSharedScripts() {
            viewModelScope.launch {
                try {
                    val inputStream = application.resources.openRawResource(R.raw.shared_scripts)
                    val reader = InputStreamReader(inputStream)
                    val scriptListType = object : TypeToken<List<Script>>() {}.type
                    val scripts: List<Script> = gson.fromJson(reader, scriptListType)
                    _caseScripts.value = scripts
                } catch (e: Exception) {
                    // Handle error, e.g., log it or show a message
                }
            }
        }

        fun loadScript(script: Script) {
            _scriptTitle.value = script.name
            _scriptText.value = script.content
        }

        fun saveScript() {
            viewModelScope.launch {
                _saveState.value = SaveState.Saving
                try {
                    // Here you might want to save the script with its title,
                    // but current settingsManager only saves the script text.
                    // This would be a point of future improvement.
                    settingsManager.saveScript(_scriptText.value)
                    _saveState.value = SaveState.Success
                } catch (e: Exception) {
                    _saveState.value = SaveState.Error("Failed to save script")
                }
            }
        }
    }
// Removed local SaveState definition from here
