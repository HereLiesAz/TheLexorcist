package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.ExtrasRepository
import com.hereliesaz.lexorcist.data.SharedItem
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import java.io.IOException

@HiltViewModel
class ExtrasViewModel @Inject constructor(
    private val extrasRepository: ExtrasRepository,
    private val application: Application // Inject Application context
) : ViewModel() {

    data class DefaultExtras(
        val scripts: List<Script>,
        val templates: List<Template>
    )

    data class ExtrasUiState(
        val isLoading: Boolean = true,
        val items: List<SharedItem> = emptyList(),
        val searchQuery: String = "",
        val error: String? = null,
        val currentUserEmail: String? = null
    )

    private val _uiState = MutableStateFlow(ExtrasUiState())
    val uiState: StateFlow<ExtrasUiState> = _uiState.asStateFlow()

    private val _allItems = MutableStateFlow<List<SharedItem>>(emptyList())

    private val _pendingSharedItemName = MutableStateFlow<String?>(null)
    val pendingSharedItemName: StateFlow<String?> = _pendingSharedItemName.asStateFlow()

    private val _pendingSharedItemType = MutableStateFlow<String?>(null)
    val pendingSharedItemType: StateFlow<String?> = _pendingSharedItemType.asStateFlow()

    private val _pendingSharedItemContent = MutableStateFlow<String?>(null)
    val pendingSharedItemContent: StateFlow<String?> = _pendingSharedItemContent.asStateFlow()

    private val _pendingSharedItemDescription = MutableStateFlow<String?>(null)
    val pendingSharedItemDescription: StateFlow<String?> = _pendingSharedItemDescription.asStateFlow()

    init {
        observeSearchQuery()
        loadExtras(isUserLoggedIn = _uiState.value.currentUserEmail != null)
    }

    fun setAuthSource(authSignInState: StateFlow<SignInState>) {
        viewModelScope.launch {
            authSignInState.collect { signInState ->
                val email = if (signInState is SignInState.Success) signInState.userInfo?.email else null
                _uiState.value = _uiState.value.copy(currentUserEmail = email)
                if (email == null) {
                    _uiState.value = _uiState.value.copy(items = emptyList(), error = "Sign in to browse community extras.")
                }
                loadExtras(isUserLoggedIn = email != null)
            }
        }
    }

    private fun applySearchFilter(allItems: List<SharedItem>, query: String): List<SharedItem> {
        return if (query.isBlank()) {
            allItems
        } else {
            allItems.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.authorName.contains(query, ignoreCase = true) || 
                it.authorEmail.contains(query, ignoreCase = true)
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            combine(uiState, _allItems) { currentUiState, allItemsList -> // Observe uiState and allItems
                applySearchFilter(allItemsList, currentUiState.searchQuery)
            }.collect { filteredItems ->
                // Update only items; isLoading might be true or error might be set from loadExtras
                _uiState.value = _uiState.value.copy(items = filteredItems)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    private fun loadDefaultExtras(): List<SharedItem> {
        return try {
            val jsonString = application.assets.open("default_extras.json").bufferedReader().use { it.readText() }
            val typeToken = object : TypeToken<DefaultExtras>() {}.type
            val extras: DefaultExtras = Gson().fromJson(jsonString, typeToken)

            val correctedScripts = extras.scripts.map {
                it.copy(authorName = "Az", authorEmail = "hereliesaz@gmail.com")
            }
            val correctedTemplates = extras.templates.map {
                it.copy(authorName = "Az", authorEmail = "hereliesaz@gmail.com")
            }

            val sharedItems = mutableListOf<SharedItem>()
            sharedItems.addAll(correctedScripts.map { SharedItem.from(it) })
            sharedItems.addAll(correctedTemplates.map { SharedItem.from(it) })
            sharedItems
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun loadExtras(isUserLoggedIn: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val localItems = loadDefaultExtras()
            _allItems.value = localItems
            _uiState.value = _uiState.value.copy(items = applySearchFilter(localItems, _uiState.value.searchQuery))

            if (isUserLoggedIn) {
                when (val result = extrasRepository.getSharedItems()) {
                    is Result.Success -> {
                        val remoteItems = result.data
                        val mergedItems = localItems.toMutableList()
                        val localItemIds = localItems.map { it.id }.toSet()
                        remoteItems.forEach { remoteItem ->
                            if (!localItemIds.contains(remoteItem.id)) {
                                mergedItems.add(remoteItem)
                            }
                        }
                        _allItems.value = mergedItems
                        _uiState.value = _uiState.value.copy(isLoading = false, items = applySearchFilter(mergedItems, _uiState.value.searchQuery), error = null)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = result.exception.message ?: "Could not refresh extras from server.")
                    }
                    is Result.UserRecoverableError -> {
                         _uiState.value = _uiState.value.copy(isLoading = false, error = result.exception.message ?: "A user recoverable error occurred.")
                    }
                    is Result.Loading -> {
                        // isLoading is already true
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteItem(item: SharedItem) {
        val userEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = extrasRepository.deleteSharedItem(item, userEmail)) {
                is Result.Success -> loadExtras(isUserLoggedIn = true)
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to delete item: ${result.exception.localizedMessage}")
                is Result.UserRecoverableError -> _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to delete item: User recoverable error - ${result.exception.localizedMessage}")
                is Result.Loading -> { /* isLoading is true */ }
            }
        }
    }

    fun isAuthor(item: SharedItem): Boolean {
        val email = _uiState.value.currentUserEmail
        return email != null && (item.authorEmail == email || email == "hereliesaz@gmail.com")
    }

    fun shareItem(
        name: String,
        description: String,
        content: String,
        type: String,
        authorName: String,
        authorEmail: String,
        court: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Pass court ?: "" to ensure it's not null if the repository expects a non-null String
            when (extrasRepository.shareItem(name, description, content, type, authorName, authorEmail, court ?: "")) {
                is Result.Success -> {
                    loadExtras(isUserLoggedIn = true)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to share item.")
                }
                 is Result.UserRecoverableError -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to share item. A recoverable error occurred.")
                }
                is Result.Loading -> { /* isLoading is true */ }
            }
        }
    }

    fun prepareForSharing(name: String, description: String, type: String, content: String) {
        _pendingSharedItemName.value = name
        _pendingSharedItemDescription.value = description
        _pendingSharedItemType.value = type
        _pendingSharedItemContent.value = content
    }

    fun clearPendingSharedItem() {
        _pendingSharedItemName.value = null
        _pendingSharedItemDescription.value = null
        _pendingSharedItemType.value = null
        _pendingSharedItemContent.value = null
    }

    fun rateAddon(id: String, rating: Int, type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = extrasRepository.rateAddon(id, rating, type)
            if (success) {
                loadExtras(isUserLoggedIn = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to rate item.")
            }
        }
    }
}
