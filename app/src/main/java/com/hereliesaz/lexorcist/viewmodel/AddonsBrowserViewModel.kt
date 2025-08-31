package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.model.SaveState // Added import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddonsBrowserViewModel @Inject constructor(
    private val googleApiService: GoogleApiService
) : ViewModel() {

    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    val scripts: StateFlow<List<Script>> = _scripts.asStateFlow() // Added asStateFlow()

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates.asStateFlow() // Added asStateFlow()

    // Example of using SaveState if you were to implement a save/share operation here
    private val _shareOperationState = MutableStateFlow<SaveState>(SaveState.Idle)
    val shareOperationState: StateFlow<SaveState> = _shareOperationState.asStateFlow()

    init {
        loadAddons()
    }

    private fun loadAddons() {
        viewModelScope.launch {
            // In a real scenario, you'd handle potential errors from these calls
            _scripts.value = googleApiService.getSharedScripts()
            _templates.value = googleApiService.getSharedTemplates()
        }
    }

    fun shareAddon(name: String, description: String, content: String, type: String) {
        viewModelScope.launch {
            _shareOperationState.value = SaveState.Saving
            val success = googleApiService.shareAddon(name, description, content, type)
            if (success) {
                _shareOperationState.value = SaveState.Success
                loadAddons() // Refresh list after successful share
            } else {
                _shareOperationState.value = SaveState.Error("Failed to share addon")
            }
        }
    }

    fun rateAddon(id: String, rating: Int, type: String) {
        viewModelScope.launch {
            // Rating might not need a SaveState if it's a quick operation or UI updates immediately
            val success = googleApiService.rateAddon(id, rating, type)
            if (success) {
                loadAddons() // Refresh list to show updated ratings
            } else {
                // Handle rating failure, perhaps with a Snackbar or a different StateFlow for errors
            }
        }
    }
}
