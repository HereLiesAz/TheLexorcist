package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.utils.Result
import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRunner @Inject constructor(
    private val generativeAIService: GenerativeAIService,
    private val scriptedMenuViewModel: ScriptedMenuViewModel
) {

    /**
     * Custom exception for script execution errors.
     */
    class ScriptExecutionException(message: String, cause: Throwable) : Exception(message, cause)

    /**
     * API wrapper for the GenerativeAIService, exposed to Javascript.
     * This makes calls appear synchronous to the script writer.
     */
    inner class AIApi {
        @Suppress("unused") // Used by Rhino
        fun generateContent(prompt: String): String {
            // Bridge the suspend function to the synchronous JS world.
            // This blocks the script's thread, which is acceptable as scripts run in the background.
            return runBlocking {
                generativeAIService.generateContent(prompt)
            }
        }
    }

    /**
     * API wrapper for the ScriptedMenuViewModel, exposed to Javascript.
     * Provides a clean interface for scripts to control the UI.
     */
    inner class UIApi {
        @Suppress("unused") // Used by Rhino
        fun addOrUpdate(id: String, label: String, isVisible: Boolean, onClickAction: String?) {
            scriptedMenuViewModel.addOrUpdateMenuItem(id, label, isVisible, onClickAction)
        }

        @Suppress("unused") // Used by Rhino
        fun remove(id: String) {
            scriptedMenuViewModel.removeMenuItem(id)
        }

        @Suppress("unused") // Used by Rhino
        fun clearAll() {
            scriptedMenuViewModel.clearAllMenuItems()
        }
    }

    fun runScript(script: String, evidence: Evidence): Result<List<String>> {
        val rhino = Context.enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1 // Necessary for Android compatibility
        try {
            val scope: Scriptable = rhino.initStandardObjects()

            // Set up the 'lex' namespace
            val lexObject = rhino.newObject(scope)
            ScriptableObject.putProperty(scope, "lex", lexObject)

            // Add APIs to the 'lex' namespace
            ScriptableObject.putProperty(lexObject, "ai", Context.javaToJS(AIApi(), scope))
            ScriptableObject.putProperty(lexObject, "ui", Context.javaToJS(UIApi(), scope))

            // Provide context-specific data
            ScriptableObject.putProperty(scope, "evidence", Context.javaToJS(evidence, scope))

            // The script is expected to modify this 'tags' array
            val tags = mutableListOf<String>()
            ScriptableObject.putProperty(scope, "tags", Context.javaToJS(tags, scope))

            // Execute the user's script
            rhino.evaluateString(scope, script, "JavaScript<ScriptRunner>", 1, null)

            // Extract the results from the 'tags' variable in the script's scope
            val tagsPropertyFromScope: Any? = ScriptableObject.getProperty(scope, "tags")
            val convertedJavaList: Any? = Context.jsToJava(tagsPropertyFromScope, List::class.java)

            @Suppress("UNCHECKED_CAST")
            return if (convertedJavaList is List<*>) {
                Result.Success(convertedJavaList as List<String>)
            } else {
                val actualType = if (convertedJavaList != null) convertedJavaList.javaClass.name else "null"
                Result.Error(
                    ScriptExecutionException(
                        "Script 'tags' property is not a List or could not be converted. Actual type: $actualType",
                        Exception("Unexpected type for 'tags' property from script.")
                    )
                )
            }
        } catch (e: org.mozilla.javascript.RhinoException) {
            return Result.Error(ScriptExecutionException("Error during JavaScript execution", e))
        } catch (e: Exception) {
            return Result.Error(ScriptExecutionException("An unexpected error occurred while running script", e))
        } finally {
            Context.exit()
        }
    }
}
