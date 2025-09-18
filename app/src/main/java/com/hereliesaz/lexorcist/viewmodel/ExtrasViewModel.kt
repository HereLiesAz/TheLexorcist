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

    init {
        observeSearchQuery()
    }

    fun setAuthSource(authSignInState: StateFlow<SignInState>) {
        viewModelScope.launch {
            authSignInState.collect { signInState ->
                val email = if (signInState is SignInState.Success) signInState.userInfo?.email else null
                _uiState.value = _uiState.value.copy(currentUserEmail = email)
                if (email != null) {
                    loadSharedItems()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, items = emptyList(), error = "Please sign in to view extras.")
                }
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            combine(_uiState, _allItems) { uiState, allItems ->
                val query = uiState.searchQuery
                if (query.isBlank()) {
                    allItems
                } else {
                    allItems.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
                    }
                }
            }.collect { filteredItems ->
                _uiState.value = _uiState.value.copy(items = filteredItems)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun loadSharedItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = extrasRepository.getSharedItems()) {
                is Result.Success -> {
                    _allItems.value = result.data
                    val currentQuery = _uiState.value.searchQuery
                    val filteredItems = if (currentQuery.isBlank()) {
                        result.data
                    } else {
                        result.data.filter {
                            it.name.contains(currentQuery, ignoreCase = true) ||
                            it.description.contains(currentQuery, ignoreCase = true) ||
                            it.author.contains(currentQuery, ignoreCase = true)
                        }
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, items = filteredItems, error = null)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.exception.message ?: "An unknown error occurred.")
                }
                is Result.UserRecoverableError -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.exception.message ?: "A user recoverable error occurred.")
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true) // Keep isLoading true
                }
            }
        }
    }

    fun deleteItem(item: SharedItem) {
        val userEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Indicate loading state
            when (val result = extrasRepository.deleteSharedItem(item, userEmail)) {
                is Result.Success -> loadSharedItems() // Refresh list on success, loadSharedItems will handle isLoading
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to delete item: ${result.exception.localizedMessage}")
                is Result.UserRecoverableError -> _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to delete item: User recoverable error - ${result.exception.localizedMessage}")
                is Result.Loading -> {
                    // isLoading is already true, or will be set by loadSharedItems if success
                }
            }
        }
    }

    fun isAuthor(item: SharedItem): Boolean {
        val email = _uiState.value.currentUserEmail
        return email != null && (item.author == email || email == "hereliesaz@gmail.com")
    }

    fun shareItem(name: String, description: String, content: String, type: String, court: String? = null) {
        val authorEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Indicate loading state
            when (extrasRepository.shareItem(name, description, content, type, authorEmail, court)) {
                is Result.Success -> {
                    loadSharedItems() // Refresh list after sharing, loadSharedItems will handle isLoading
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to share item.")
                }
                 is Result.UserRecoverableError -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to share item. A recoverable error occurred.")
                }
                is Result.Loading -> {
                     // isLoading is already true
                }
            }
        }
    }

    fun prepareForSharing(name: String, type: String, content: String) {
        _pendingSharedItemName.value = name
        _pendingSharedItemType.value = type
        _pendingSharedItemContent.value = content
    }

    fun clearPendingSharedItem() {
        _pendingSharedItemName.value = null
        _pendingSharedItemType.value = null
        _pendingSharedItemContent.value = null
    }

    fun rateAddon(id: String, rating: Int, type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = extrasRepository.rateAddon(id, rating, type)
            if (result) {
                loadSharedItems() // loadSharedItems will set isLoading to false on completion/error
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to rate item.")
            }
        }
    }
}
