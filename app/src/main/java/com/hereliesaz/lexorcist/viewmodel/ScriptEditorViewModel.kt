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

    private val _saveState = MutableStateFlow<SaveState>(SaveState.NONE)
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
            _saveState.value = SaveState.SAVING
            try {
                scriptId?.let {
                    googleApiService.updateScript(it, _scriptText.value)
                    _saveState.value = SaveState.SUCCESS
                } ?: run {
                    _saveState.value = SaveState.FAILURE("Script ID is not set")
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.FAILURE("Failed to save script: ${e.message}")
            }
        }
    }
}
