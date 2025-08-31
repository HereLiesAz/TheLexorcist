package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.GoogleApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptEditorViewModel @Inject constructor(
    private val googleApiService: GoogleApiService
) : ViewModel() {

    private val _scriptText = MutableStateFlow("")
    val scriptText: StateFlow<String> = _scriptText.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private var scriptId: String? = null

    fun loadScript(scriptId: String) {
        this.scriptId = scriptId
        viewModelScope.launch {
            val scriptContent = googleApiService.getScript(scriptId)
            _scriptText.value = scriptContent?.files?.firstOrNull()?.source ?: ""
        }
    }

    fun onScriptTextChanged(newText: String) {
        _scriptText.value = newText
    }

    fun insertText(text: String) {
        _scriptText.value += text
    }

    fun saveScript() {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            if (scriptId == null) {
                _saveState.value = SaveState.Error("No script loaded to save.")
                return@launch
            }
            try {
                // TODO: Implement save functionality using GoogleApiService
                // For example: googleApiService.updateScript(scriptId, _scriptText.value)
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Failed to save script")
            }
        }
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}
