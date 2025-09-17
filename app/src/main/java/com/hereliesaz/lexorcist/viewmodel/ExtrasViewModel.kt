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

    init {
        observeSearchQuery()
    }

    fun setAuthSource(authSignInState: StateFlow<SignInState>) {
        viewModelScope.launch {
            authSignInState.collect { signInState ->
                val email = if (signInState is SignInState.Success) signInState.userInfo.email else null
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
            _uiState.map { it.searchQuery }.combine(_allItems) { query, items ->
                if (query.isBlank()) {
                    items
                } else {
                    items.filter {
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
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.exception.message ?: "An unknown error occurred.")
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "An error occurred.")
                }
            }
        }
    }

    fun deleteItem(item: SharedItem) {
        val userEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            when (extrasRepository.deleteSharedItem(item, userEmail)) {
                is Result.Success -> loadSharedItems() // Refresh list on success
                is Result.Error -> _uiState.value = _uiState.value.copy(error = "Failed to delete item.")
                else -> { /* Handle other cases */ }
            }
        }
    }

    fun isAuthor(item: SharedItem): Boolean {
        val email = _uiState.value.currentUserEmail
        return email != null && (item.author == email || email == "hereliesaz@gmail.com")
    }

    fun shareItem(name: String, description: String, content: String, type: String) {
        val authorEmail = _uiState.value.currentUserEmail ?: return
        viewModelScope.launch {
            extrasRepository.shareItem(name, description, content, type, authorEmail)
            loadSharedItems()
        }
    }
}
