package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRunner @Inject constructor(
    private val generativeAIService: GenerativeAIService,
    private val scriptedMenuViewModel: ScriptedMenuViewModel
) {
    // A coroutine scope for this service. Since ScriptRunner is a @Singleton, this scope will
    // live as long as the application.
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    class ScriptExecutionException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    // The main API object exposed to the Javascript context under the global name "Lexorcist".
    inner class LexorcistApi {
        val ai = AIApi()
        val ui = UIApi()
    }

    // API wrapper for the GenerativeAIService to handle asynchronous calls from Javascript.
    inner class AIApi {
        /**
         * Generates content using the AI model. This is an async operation.
         * @param prompt The text prompt to send to the model.
         * @param callback A Javascript function that will be called with the result.
         *                 The function should accept one argument: the response string.
         */
        @Suppress("unused") // Used by scripts
        fun generate(prompt: String, callback: Function) {
            coroutineScope.launch(Dispatchers.IO) {
                val result = generativeAIService.generateContent(prompt)
                // We need to enter a Rhino context on this background thread to call the JS function
                val rhino = org.mozilla.javascript.Context.enter()
                try {
                    val scope = callback.parentScope
                    // Call the Javascript function with the result
                    callback.call(rhino, scope, scope, arrayOf(result))
                } finally {
                    org.mozilla.javascript.Context.exit()
                }
            }
        }
    }

    // API wrapper for UI control functions, delegating to the ScriptedMenuViewModel.
    inner class UIApi {
        /**
         * Adds or updates a menu item in the navigation rail.
         * @param id The unique ID of the menu item.
         * @param label The text to display.
         * @param isVisible Whether the item should be visible.
         * @param onClickFunction The name of a global JS function to call when clicked. Can be null.
         * @param screenJson The JSON definition of a screen to open when clicked. Can be null.
         */
        @Suppress("unused") // Used by scripts
        fun upsertMenuItem(id: String, label: String, isVisible: Boolean, onClickFunctionObj: Any?, screenJsonObj: Any?) {
            val onClickFunction = (onClickFunctionObj as? String)?.takeIf { it.isNotBlank() }
            val screenJson = (screenJsonObj as? String)?.takeIf { it.isNotBlank() }
            // The ViewModel's update method is thread-safe
            scriptedMenuViewModel.upsertMenuItem(id, label, isVisible, onClickFunction, screenJson)
        }

        /**
         * Removes a menu item from the navigation rail.
         * @param id The unique ID of the menu item to remove.
         */
        @Suppress("unused") // Used by scripts
        fun removeMenuItem(id: String) {
            scriptedMenuViewModel.removeMenuItem(id)
        }
    }

    fun runScript(
        script: String,
        evidence: Evidence,
    ): Result<List<String>> {
        val rhino = org.mozilla.javascript.Context.enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1 // Necessary for Android compatibility
        try {
            val scope: Scriptable = rhino.initStandardObjects()
            val tags = mutableListOf<String>()
            ScriptableObject.putProperty(
                scope,
                "evidence",
                org.mozilla.javascript.Context.javaToJS(evidence, scope),
            )
            ScriptableObject.putProperty(
                scope,
                "tags",
                org.mozilla.javascript.Context.javaToJS(tags, scope),
            )
            // Expose the new, consolidated API to the script
            ScriptableObject.putProperty(
                scope,
                "Lexorcist",
                org.mozilla.javascript.Context.javaToJS(LexorcistApi(), scope),
            )

            rhino.evaluateString(scope, script, "JavaScript<ScriptRunner>", 1, null)

            val tagsPropertyFromScope: Any? = ScriptableObject.getProperty(scope, "tags")
            val convertedJavaList: Any? = org.mozilla.javascript.Context.jsToJava(tagsPropertyFromScope, List::class.java)

            if (convertedJavaList is List<*>) {
                // The list might contain non-string elements from JS, so we filter and convert.
                val stringListFromTags: List<String> = convertedJavaList.mapNotNull { it?.toString() }
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
            org.mozilla.javascript.Context.exit()
        }
    }
}
