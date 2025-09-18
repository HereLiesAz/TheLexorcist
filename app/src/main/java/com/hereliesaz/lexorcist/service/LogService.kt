package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.model.LogEntry
import com.hereliesaz.lexorcist.model.LogLevel // Ensure this is your custom LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
// import java.util.logging.Level // Removed java.util.logging.Level
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogService @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _logEventFlow = MutableSharedFlow<LogEntry>(replay = 100)
    val logEventFlow: SharedFlow<LogEntry> = _logEventFlow.asSharedFlow()

    // Changed parameter 'level' to use com.hereliesaz.lexorcist.model.LogLevel
    fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        scope.launch {
            // Assuming LogEntry constructor expects com.hereliesaz.lexorcist.model.LogLevel
            val newLog = LogEntry(System.currentTimeMillis(), message, level)
            _logEventFlow.emit(newLog)
        }
    }
}
