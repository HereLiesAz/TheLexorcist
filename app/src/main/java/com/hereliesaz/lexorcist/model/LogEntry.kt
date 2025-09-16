package com.hereliesaz.lexorcist.model

data class LogEntry(
    val timestamp: Long,
    val message: String,
    val level: LogLevel,
)

enum class LogLevel {
    INFO,
    ERROR,
    DEBUG,
}
