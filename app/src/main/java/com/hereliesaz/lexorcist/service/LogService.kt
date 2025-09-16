package com.hereliesaz.lexorcist.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogService @Inject constructor() {
    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    fun addLog(message: String) {
        val currentLogs = _logMessages.value.toMutableList()
        currentLogs.add(0, message) // Add to the top of the list
        _logMessages.value = currentLogs
    }

    fun clearLogs() {
        _logMessages.value = emptyList()
    }
}
