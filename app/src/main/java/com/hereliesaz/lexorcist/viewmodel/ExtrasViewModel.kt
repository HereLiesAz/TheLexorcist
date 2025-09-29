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

@HiltViewModel
class ExtrasViewModel @Inject constructor(
    private val extrasRepository: ExtrasRepository
) : ViewModel() {

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
        loadExtras()
    }

    fun setAuthSource(authSignInState: StateFlow<SignInState>) {
        viewModelScope.launch {
            authSignInState.collect { signInState ->
                val email = if (signInState is SignInState.Success) signInState.userInfo?.email else null
                _uiState.value = _uiState.value.copy(currentUserEmail = email)
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
            combine(uiState, _allItems) { currentUiState, allItemsList ->
                applySearchFilter(allItemsList, currentUiState.searchQuery)
            }.collect { filteredItems ->
                _uiState.value = _uiState.value.copy(items = filteredItems)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun loadExtras() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = extrasRepository.getSharedItems()) {
                is Result.Success -> {
                    val allItems = result.data
                    _allItems.value = allItems
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = applySearchFilter(allItems, _uiState.value.searchQuery),
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.exception.message ?: "Could not load extras.")
                }
                is Result.UserRecoverableError -> {
                     _uiState.value = _uiState.value.copy(isLoading = false, error = result.exception.message ?: "A user recoverable error occurred.")
                }
                is Result.Loading -> {
                    // isLoading is already true
                }
            }
        }
    }

    fun deleteItem(item: SharedItem) {
        val userEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = extrasRepository.deleteSharedItem(item, userEmail)) {
                is Result.Success -> loadExtras()
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
            when (extrasRepository.shareItem(name, description, content, type, authorName, authorEmail, court ?: "")) {
                is Result.Success -> {
                    loadExtras()
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
                loadExtras()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to rate item.")
            }
        }
    }
}