package com.hereliesaz.lexorcist.service

class ScriptExecutionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
