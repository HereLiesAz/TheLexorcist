package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.Result
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
// import android.util.Log // Uncomment if logging for non-string elements is added

// import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel // REMOVED
import javax.inject.Inject
import javax.inject.Singleton

import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.javascript.Function

@Singleton
class ScriptRunner @Inject constructor(
    private val generativeAIService: GenerativeAIService,
    private val scriptedMenuViewModel: ScriptedMenuViewModel
) {
    class ScriptExecutionException(
        message: String,
        cause: Throwable, // Cause is non-null
    ) : Exception(message, cause)

    // API wrapper for MenuItem to be exposed to Javascript
    inner class MenuItemApi {
        fun setLabel(label: String) {
            // Assuming a single menu item for now, with a fixed ID
            scriptedMenuViewModel.setMenuItemLabel("scripted_item_1", label)
        }

        fun setVisible(isVisible: Boolean) {
            scriptedMenuViewModel.setMenuItemVisibility("scripted_item_1", isVisible)
        }
    }

    // API wrapper for AI service to handle async calls
    inner class AIApi {
        fun generate(prompt: String, callback: Function) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = generativeAIService.generateContent(prompt)
                val rhino = org.mozilla.javascript.Context.enter()
                try {
                    val scope = callback.parentScope
                    callback.call(rhino, scope, scope, arrayOf(result))
                } finally {
                    org.mozilla.javascript.Context.exit()
                }
            }
        }
    }

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
            ScriptableObject.putProperty(
                scope,
                "MenuItem",
                org.mozilla.javascript.Context
                    .javaToJS(MenuItemApi(), scope),
            )
            ScriptableObject.putProperty(
                scope,
                "AI",
                org.mozilla.javascript.Context
                    .javaToJS(AIApi(), scope),
            )
            rhino.evaluateString(scope, script, "JavaScript<ScriptRunner>", 1, null)

            val tagsPropertyFromScope: Any? = ScriptableObject.getProperty(scope, "tags")
            val convertedJavaList: Any? = org.mozilla.javascript.Context.jsToJava(tagsPropertyFromScope, List::class.java)

            if (convertedJavaList is List<*>) {
                val stringListFromTags: List<String> = convertedJavaList.filterIsInstance<String>()
                return Result.Success(stringListFromTags)
            } else {
                val actualType = if (convertedJavaList != null) convertedJavaList.javaClass.name else "null"
                return Result.Error(
                    ScriptExecutionException(
                        "Script 'tags' property is not a List or could not be converted. Actual type: $actualType",
                        Exception("Unexpected type for 'tags' property from script.")
                    )
                )
            }
        } catch (e: org.mozilla.javascript.RhinoException) {
            return Result.Error(ScriptExecutionException("Error during JavaScript execution", e))
        } catch (e: Exception) {
            return Result.Error(ScriptExecutionException("An unexpected error occurred while running script or processing results", e))
        } finally {
            org.mozilla.javascript.Context
                .exit()
        }
    }
}
