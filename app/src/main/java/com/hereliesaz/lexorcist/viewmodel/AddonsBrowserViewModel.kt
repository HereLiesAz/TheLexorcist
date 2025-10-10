package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddonsBrowserViewModel
@Inject
constructor(
    private val googleApiService: GoogleApiService
) : ViewModel() {
    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    private val _templates = MutableStateFlow<List<Template>>(emptyList())

    private val _shareOperationState = MutableStateFlow<SaveState>(SaveState.Idle)
    val shareOperationState: StateFlow<SaveState> = _shareOperationState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredScripts: StateFlow<List<Script>> =
        searchQuery.combine(_scripts) { query, scripts ->
            if (query.isBlank()) {
                scripts
            } else {
                scripts.filter { script ->
                    script.name.contains(query, ignoreCase = true) ||
                            script.description.contains(query, ignoreCase = true) ||
                            script.authorName.contains(query, ignoreCase = true) ||
                            (script.court != null && script.court.contains(query, ignoreCase = true))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTemplates: StateFlow<List<Template>> =
        searchQuery.combine(_templates) { query, templates ->
            if (query.isBlank()) {
                templates
            } else {
                templates.filter { template ->
                    template.name.contains(query, ignoreCase = true) ||
                            template.description.contains(query, ignoreCase = true) ||
                            template.authorName.contains(query, ignoreCase = true) ||
                            (template.court != null && template.court.contains(query, ignoreCase = true))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        loadAddons()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun loadAddons() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _scripts.value = googleApiService.getSharedScripts()
                _templates.value = googleApiService.getSharedTemplates()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun shareAddon(
        name: String,
        description: String,
        content: String,
        type: String,
        authorName: String,
        authorEmail: String,
        court: String?
    ) {
        viewModelScope.launch {
            _shareOperationState.value = SaveState.Saving
            val shareResult = googleApiService.shareAddon(name, description, content, type, authorName, authorEmail, court ?: "")
            when (shareResult) {
                is Result.Success -> {
                    _shareOperationState.value = SaveState.Success
                    loadAddons()
                }
                is Result.Error -> {
                    _shareOperationState.value = SaveState.Error("Failed to share addon: ${shareResult.exception.localizedMessage ?: "Unknown error"}")
                }
                is Result.UserRecoverableError -> {
                    _shareOperationState.value = SaveState.Error("Failed to share addon: User recoverable error - ${shareResult.exception.localizedMessage ?: "Unknown user error"}")
                }
                is Result.Loading -> {
                    // The state is already SaveState.Saving
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
            val success = googleApiService.rateAddon(id, rating, type)
            if (success) {
                loadAddons()
            } else {
                // Handle rating failure
            }
        }
    }

    fun suggestEditViaEmail(context: android.content.Context, authorEmail: String, itemName: String, content: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(authorEmail))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Suggested Edit for Lexorcist Addon: $itemName")
            putExtra(android.content.Intent.EXTRA_TEXT, "Hello,\n\nI have a suggested edit for your addon '$itemName'.\n\nHere is the modified content:\n\n---\n\n$content\n\n---\n\nThank you!")
        }
        try {
            context.startActivity(android.content.Intent.createChooser(intent, "Send Email"))
        } catch (e: android.content.ActivityNotFoundException) {
            // Handle case where no email app is installed
        }
    }
}
