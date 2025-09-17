package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.model.LogEntry
import com.hereliesaz.lexorcist.model.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogService @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // A shared flow to broadcast log events. Replays the last 100 logs for new collectors.
    private val _logEventFlow = MutableSharedFlow<LogEntry>(replay = 100)
    val logEventFlow: SharedFlow<LogEntry> = _logEventFlow.asSharedFlow()

    fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        scope.launch {
            val newLog = LogEntry(System.currentTimeMillis(), message, level)
            _logEventFlow.emit(newLog)
        }
    }
}
