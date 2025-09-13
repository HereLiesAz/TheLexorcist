package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.Result
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class ScriptRunner {
    class ScriptExecutionException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    fun runScript(
        script: String,
        evidence: Evidence,
    ): Result<List<String>> {
        val rhino =
            org.mozilla.javascript.Context
                .enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1
        try {
            val scope: Scriptable = rhino.initStandardObjects()
            val tags = mutableListOf<String>()
            ScriptableObject.putProperty(
                scope,
                "evidence",
                org.mozilla.javascript.Context
                    .javaToJS(evidence, scope),
            )
            ScriptableObject.putProperty(
                scope,
                "tags",
                org.mozilla.javascript.Context
                    .javaToJS(tags, scope),
            )
            rhino.evaluateString(scope, script, "JavaScript<MainViewModel>", 1, null)
            val tagsFromScope = ScriptableObject.getProperty(scope, "tags")
            val resultTags = org.mozilla.javascript.Context.jsToJava(tagsFromScope, List::class.java) as List<String>
            return Result.Success(resultTags)
        } catch (e: Exception) {
            return Result.Error(ScriptExecutionException("Failed to execute script", e))
        } finally {
            org.mozilla.javascript.Context
                .exit()
        }
    }
}
