package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.service.GoogleApiService // Import GoogleApiService
import com.hereliesaz.lexorcist.utils.Result // Import Result
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
        private val googleApiService: GoogleApiService // Inject GoogleApiService directly
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
                // Use the injected googleApiService directly
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
            authorEmail: String, // Added authorEmail parameter
            court: String? // Added court parameter
        ) {
            viewModelScope.launch {
                _shareOperationState.value = SaveState.Saving
                val shareResult = googleApiService.shareAddon(name, description, content, type, authorEmail, court ?: "")
                when (shareResult) {
                    is Result.Success -> {
                        _shareOperationState.value = SaveState.Success
                        loadAddons() // Refresh list after successful share
                    }
                    is Result.Error -> {
                        _shareOperationState.value = SaveState.Error("Failed to share addon: ${shareResult.exception.localizedMessage ?: "Unknown error"}")
                    }
                    is Result.UserRecoverableError -> {
                        _shareOperationState.value = SaveState.Error("Failed to share addon: User recoverable error - ${shareResult.exception.localizedMessage ?: "Unknown user error"}")
                    }
                }
            }
        }

        fun rateAddon(
            id: String,
            rating: Int,
            type: String,
        ) {
            viewModelScope.launch {
                // Use the injected googleApiService directly
                val success = googleApiService.rateAddon(id, rating, type) // Assuming this returns Boolean or needs similar Result handling
                if (success) { // Adjust if rateAddon also returns Result
                    loadAddons() // Refresh list to show updated ratings
                } else {
                    // Handle rating failure, perhaps with a Snackbar or a different StateFlow for errors
                }
            }
        }
    }
