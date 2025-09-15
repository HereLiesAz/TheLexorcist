package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.Result
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
// import android.util.Log // Uncomment if logging for non-string elements is added

class ScriptRunner {
    class ScriptExecutionException(
        message: String,
        cause: Throwable, // Cause is non-null
    ) : Exception(message, cause)

    fun runScript(
        script: String,
        evidence: Evidence,
    ): Result<List<String>> {
        val rhino =
            org.mozilla.javascript.Context
                .enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1 // Necessary for Android compatibility
        try {
            val scope: Scriptable = rhino.initStandardObjects()
            val tags = mutableListOf<String>() // This is the initial list passed to JS
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
            rhino.evaluateString(scope, script, "JavaScript<ScriptRunner>", 1, null)

            // Safely retrieve and process the 'tags' property from the script scope
            val tagsPropertyFromScope: Any? = ScriptableObject.getProperty(scope, "tags")
            val convertedJavaList: Any? = org.mozilla.javascript.Context.jsToJava(tagsPropertyFromScope, List::class.java)

            if (convertedJavaList is List<*>) {
                // It was successfully converted to a List. Now, filter for String instances.
                val stringListFromTags: List<String> = convertedJavaList.filterIsInstance<String>()
                
                // Optional: Log or handle if some elements were not strings and got filtered out
                // if (stringListFromTags.size < convertedJavaList.size) {
                //     Log.w("ScriptRunner", "Script 'tags' list contained non-string elements that were ignored.")
                // }
                return Result.Success(stringListFromTags)
            } else {
                // The 'tags' property from scope was not a list or could not be converted.
                val actualType = if (convertedJavaList != null) convertedJavaList.javaClass.name else "null"
                return Result.Error(
                    ScriptExecutionException(
                        "Script 'tags' property is not a List or could not be converted. Actual type: $actualType",
                        Exception("Unexpected type for 'tags' property from script.") // Provide a generic cause
                    )
                )
            }
        } catch (e: org.mozilla.javascript.RhinoException) {
            // Specific catch for errors during script execution (e.g., syntax errors in JS)
            return Result.Error(ScriptExecutionException("Error during JavaScript execution", e))
        } catch (e: Exception) {
            // Catch other unexpected exceptions during the process
            return Result.Error(ScriptExecutionException("An unexpected error occurred while running script or processing results", e))
        } finally {
            org.mozilla.javascript.Context
                .exit()
        }
    }
}
