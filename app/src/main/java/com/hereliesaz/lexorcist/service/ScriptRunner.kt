package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.Result
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class ScriptRunner {
    class Parser {
        var tags: MutableList<String> = mutableListOf()
    }

    class ScriptExecutionException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    fun runScript(
        script: String,
        evidence: Evidence,
    ): Result<Parser> {
        val rhino =
            org.mozilla.javascript.Context
                .enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1
        try {
            val scope: Scriptable = rhino.initStandardObjects()
            val customParserInstance = Parser()
            ScriptableObject.putProperty(
                scope,
                "evidence",
                org.mozilla.javascript.Context
                    .javaToJS(evidence, scope),
            )
            ScriptableObject.putProperty(
                scope,
                "parser",
                org.mozilla.javascript.Context
                    .javaToJS(customParserInstance, scope),
            )
            rhino.evaluateString(scope, script, "JavaScript<MainViewModel>", 1, null)
            return Result.Success(customParserInstance)
        } catch (e: Exception) {
            return Result.Error(ScriptExecutionException("Failed to execute script", e))
        } finally {
            org.mozilla.javascript.Context
                .exit()
        }
    }
}
