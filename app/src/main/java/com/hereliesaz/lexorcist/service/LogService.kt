package com.hereliesaz.lexorcist.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

import com.hereliesaz.lexorcist.model.LogEntry
import com.hereliesaz.lexorcist.model.LogLevel

@Singleton
class LogService @Inject constructor() {
    private val _logMessages = MutableStateFlow<List<LogEntry>>(emptyList())
    val logMessages: StateFlow<List<LogEntry>> = _logMessages.asStateFlow()

    fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        val newLog = LogEntry(System.currentTimeMillis(), message, level)
        val currentLogs = _logMessages.value.toMutableList()
        currentLogs.add(0, newLog) // Add to the top of the list
        _logMessages.value = currentLogs
    }

    fun clearLogs() {
        _logMessages.value = emptyList()
    }
}
