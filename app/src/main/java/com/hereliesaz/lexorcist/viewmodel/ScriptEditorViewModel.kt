package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.service.GoogleApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScriptEditorViewModel(private val googleApiService: GoogleApiService) : ViewModel() {

    private val _scriptContent = MutableStateFlow("")
    val scriptContent = _scriptContent.asStateFlow()

    fun loadScript(scriptId: String) {
        viewModelScope.launch {
            val content = googleApiService.getScript(scriptId)
            _scriptContent.value = content ?: ""
        }
    }

    fun saveScript(scriptId: String) {
        viewModelScope.launch {
            googleApiService.updateScript(scriptId, _scriptContent.value)
        }
    }

    fun onScriptContentChange(newContent: String) {
        _scriptContent.value = newContent
    }
}
