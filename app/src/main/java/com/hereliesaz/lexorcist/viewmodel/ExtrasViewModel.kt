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
import kotlinx.coroutines.flow.combine // Added import for combine
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

    init {
        observeSearchQuery()
    }

    fun setAuthSource(authSignInState: StateFlow<SignInState>) {
        viewModelScope.launch {
            authSignInState.collect { signInState ->
                val email = if (signInState is SignInState.Success) signInState.userInfo?.email else null // Used safe call
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
            // Changed to directly combine _uiState and _allItems
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
                    // Ensure search filtering is reapplied after loading new items
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
            }
        }
    }

    fun deleteItem(item: SharedItem) {
        val userEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            when (val result = extrasRepository.deleteSharedItem(item, userEmail)) {
                is Result.Success -> loadSharedItems() // Refresh list on success
                is Result.Error -> _uiState.value = _uiState.value.copy(error = "Failed to delete item: ${result.exception.localizedMessage}")
                is Result.UserRecoverableError -> _uiState.value = _uiState.value.copy(error = "Failed to delete item: User recoverable error - ${result.exception.localizedMessage}")
            }
        }
    }

    fun isAuthor(item: SharedItem): Boolean {
        val email = _uiState.value.currentUserEmail
        return email != null && (item.author == email || email == "hereliesaz@gmail.com")
    }

    // Added court parameter with a default value
    fun shareItem(name: String, description: String, content: String, type: String, court: String? = null) {
        val authorEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            // Pass the court parameter to the repository
            extrasRepository.shareItem(name, description, content, type, authorEmail, court)
            loadSharedItems() // Refresh list after sharing
        }
    }
}
