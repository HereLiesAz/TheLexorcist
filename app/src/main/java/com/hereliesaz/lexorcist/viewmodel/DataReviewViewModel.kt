package com.hereliesaz.lexorcist.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DataReviewViewModel : ViewModel() {
    private val _reviewedText = MutableStateFlow("")
    val reviewedText: StateFlow<String> = _reviewedText.asStateFlow()

    fun onTextChange(newText: String) {
        _reviewedText.value = newText
    }
}
