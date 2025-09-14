package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddonsBrowserViewModel
    @Inject
    constructor(
        private val credentialHolder: CredentialHolder, // Inject CredentialHolder
    ) : ViewModel() {
        private val _scripts = MutableStateFlow<List<Script>>(emptyList())
        val scripts: StateFlow<List<Script>> = _scripts.asStateFlow()

        private val _templates = MutableStateFlow<List<Template>>(emptyList())
        val templates: StateFlow<List<Template>> = _templates.asStateFlow()

        private val _shareOperationState = MutableStateFlow<SaveState>(SaveState.Idle)
        val shareOperationState: StateFlow<SaveState> = _shareOperationState.asStateFlow()

        init {
            loadAddons()
        }

        private fun loadAddons() {
            viewModelScope.launch {
                val googleApiService = credentialHolder.googleApiService // Get service from holder
                if (googleApiService == null) {
                    _scripts.value = emptyList()
                    _templates.value = emptyList()
                    return@launch
                }
                // In a real scenario, you'd handle potential errors from these calls
                _scripts.value = googleApiService.getSharedScripts() ?: emptyList()
                _templates.value = googleApiService.getSharedTemplates() ?: emptyList()
            }
        }

        fun shareAddon(
            name: String,
            description: String,
            content: String,
            type: String,
        ) {
            viewModelScope.launch {
                val googleApiService = credentialHolder.googleApiService // Get service from holder
                if (googleApiService == null) {
                    _shareOperationState.value = SaveState.Error("Cannot share: User not signed in or service unavailable.")
                    return@launch
                }
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

        fun rateAddon(
            id: String,
            rating: Int,
            type: String,
        ) {
            viewModelScope.launch {
                val googleApiService = credentialHolder.googleApiService // Get service from holder
                if (googleApiService == null) {
                    // Handle rating failure when service is unavailable, perhaps with a Snackbar
                    // For now, just log or do nothing if no specific UI feedback is required here
                    return@launch
                }
                val success = googleApiService.rateAddon(id, rating, type)
                if (success) {
                    loadAddons() // Refresh list to show updated ratings
                } else {
                    // Handle rating failure, perhaps with a Snackbar or a different StateFlow for errors
                }
            }
        }
    }
