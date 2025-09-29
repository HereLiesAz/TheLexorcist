package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.data.ActiveScriptRepository
import com.hereliesaz.lexorcist.data.ScriptRepository
import com.hereliesaz.lexorcist.data.ScriptStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.hereliesaz.lexorcist.model.Script
import java.util.UUID

@HiltViewModel
class ScriptBuilderViewModel
@Inject
constructor(
    private val scriptRepository: ScriptRepository,
    private val activeScriptRepository: ActiveScriptRepository,
    private val extrasRepository: com.hereliesaz.lexorcist.data.ExtrasRepository,
    private val application: Application,
) : ViewModel() {
    private val _scriptId = MutableStateFlow<String?>(null)
    val scriptId: StateFlow<String?> = _scriptId.asStateFlow()

    private val _scriptTitle = MutableStateFlow("")
    val scriptTitle: StateFlow<String> = _scriptTitle.asStateFlow()

    private val _scriptDescription = MutableStateFlow("")
    val scriptDescription: StateFlow<String> = _scriptDescription.asStateFlow()

    private val _scriptText = MutableStateFlow("")
    val scriptText: StateFlow<String> = _scriptText.asStateFlow()

    private val _allScripts = MutableStateFlow<List<Script>>(emptyList())
    val allScripts: StateFlow<List<Script>> = _allScripts.asStateFlow()

    val activeScripts: StateFlow<List<String>> = activeScriptRepository.activeScriptIds

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
        loadScripts()
    }

    fun onScriptTitleChanged(newText: String) {
        _scriptTitle.value = newText
    }

    fun onScriptDescriptionChanged(newText: String) {
        _scriptDescription.value = newText
    }

    fun onScriptTextChanged(newText: String) {
        _scriptText.value = newText
    }

    fun insertText(text: String) {
        _scriptText.value += text
    }

    private fun loadScripts() {
        viewModelScope.launch {
            _allScripts.value = scriptRepository.getScripts()
        }
    }

    fun loadScript(script: Script) {
        _scriptId.value = script.id
        _scriptTitle.value = script.name
        _scriptDescription.value = script.description
        _scriptText.value = script.content
    }

    fun newScript() {
        _scriptId.value = null
        _scriptTitle.value = ""
        _scriptDescription.value = ""
        _scriptText.value = ""
    }

    fun saveScript() {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val currentScripts = _allScripts.value.toMutableList()
                val id = _scriptId.value ?: UUID.randomUUID().toString()
                val existingScriptIndex = currentScripts.indexOfFirst { it.id == id }

                val newScript = Script(
                    id = id,
                    name = _scriptTitle.value,
                    description = _scriptDescription.value,
                    content = _scriptText.value
                )

                if (existingScriptIndex != -1) {
                    currentScripts[existingScriptIndex] = newScript
                } else {
                    currentScripts.add(newScript)
                }
                scriptRepository.saveScripts(currentScripts)
                _allScripts.value = currentScripts
                _scriptId.value = id // Ensure new scripts have their ID set
                activeScriptRepository.activateScript(id) // Automatically activate the script
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Failed to save script")
            }
        }
    }

    fun toggleActiveScript(scriptId: String) {
        activeScriptRepository.toggleActiveScript(scriptId)
    }


    fun reorderActiveScripts(from: Int, to: Int) {
        activeScriptRepository.reorderActiveScripts(from, to)
    }

    fun shareScriptToExtras(authorName: String, authorEmail: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val result = extrasRepository.shareItem(
                name = _scriptTitle.value,
                description = _scriptDescription.value,
                content = _scriptText.value,
                type = "Script",
                authorName = authorName,
                authorEmail = authorEmail,
                court = null // Scripts don't have a court property
            )
            when (result) {
                is com.hereliesaz.lexorcist.utils.Result.Success -> {
                    _saveState.value = SaveState.Success
                }
                is com.hereliesaz.lexorcist.utils.Result.Error -> {
                    _saveState.value = SaveState.Error(result.exception.message ?: application.getString(R.string.failed_to_share_script))
                }
                else -> {
                     _saveState.value = SaveState.Error(application.getString(R.string.failed_to_share_script))
                }
            }
        }
    }
}
