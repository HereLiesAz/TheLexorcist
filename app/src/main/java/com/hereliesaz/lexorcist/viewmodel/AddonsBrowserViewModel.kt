package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddonsBrowserViewModel @Inject constructor(
    private val googleApiService: GoogleApiService
) : ViewModel() {

    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    val scripts: StateFlow<List<Script>> = _scripts

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates

    init {
        loadAddons()
    }

    private fun loadAddons() {
        viewModelScope.launch {
            _scripts.value = googleApiService.getSharedScripts()
            _templates.value = googleApiService.getSharedTemplates()
        }
    }

    fun shareAddon(name: String, description: String, content: String, type: String) {
        viewModelScope.launch {
            googleApiService.shareAddon(name, description, content, type)
            loadAddons()
        }
    }

    fun rateAddon(id: String, rating: Int, type: String) {
        viewModelScope.launch {
            googleApiService.rateAddon(id, rating, type)
            loadAddons()
        }
    }
}
