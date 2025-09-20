package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.ScriptResult
import com.hereliesaz.lexorcist.utils.Result
// Removed: import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel
import kotlinx.coroutines.flow.MutableSharedFlow // ADDED
import kotlinx.coroutines.flow.asSharedFlow // ADDED
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.inject.Inject
import javax.inject.Singleton

// ADDED: Sealed class for UI Actions
sealed class ScriptRunnerUiAction {
    data class AddOrUpdate(val id: String, val label: String, val isVisible: Boolean, val onClickAction: String?) : ScriptRunnerUiAction()
    data class Remove(val id: String) : ScriptRunnerUiAction()
    object ClearAll : ScriptRunnerUiAction()
}

@Singleton
class ScriptRunner @Inject constructor(
    private val generativeAIService: GenerativeAIService
    // Removed: private val scriptedMenuViewModel: ScriptedMenuViewModel
) {

    // ADDED: SharedFlow for UI actions
    private val _uiActions = MutableSharedFlow<ScriptRunnerUiAction>()
    val uiActions = _uiActions.asSharedFlow()

    class ScriptExecutionException(message: String, cause: Throwable) : Exception(message, cause)

    inner class AIApi {
        @Suppress("unused")
        fun generateContent(prompt: String): String {
            return runBlocking {
                generativeAIService.generateContent(prompt)
            }
        }
    }

    inner class UIApi {
        @Suppress("unused")
        fun addOrUpdate(id: String, label: String, isVisible: Boolean, onClickAction: String?) {
            // MODIFIED: Emit action
            runBlocking { // Use runBlocking if emitting from a non-suspending context to a SharedFlow
                _uiActions.emit(ScriptRunnerUiAction.AddOrUpdate(id, label, isVisible, onClickAction))
            }
        }

        @Suppress("unused")
        fun remove(id: String) {
            // MODIFIED: Emit action
            runBlocking {
                _uiActions.emit(ScriptRunnerUiAction.Remove(id))
            }
        }

        @Suppress("unused")
        fun clearAll() {
            // MODIFIED: Emit action
            runBlocking {
                _uiActions.emit(ScriptRunnerUiAction.ClearAll)
            }
        }
    }

    /**
     * Executes a given script with provided evidence context and returns a structured result.
     * This runner is backward compatible, supporting both a modern `lex` object API
     * and a legacy API of global functions like `addTag()`.
     */
    fun runScript(script: String, evidence: Evidence): Result<ScriptResult> {
        val rhino = Context.enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1
        try {
            val scope: Scriptable = rhino.initStandardObjects()

            // This will hold all outputs from the script.
            val scriptResult = ScriptResult()

            // --- Modern API (`lex` object) ---
            val lexObject = rhino.newObject(scope)
            ScriptableObject.putProperty(scope, "lex", lexObject)
            ScriptableObject.putProperty(lexObject, "ai", Context.javaToJS(AIApi(), scope))
            ScriptableObject.putProperty(lexObject, "ui", Context.javaToJS(UIApi(), scope))

            // For modern scripts that directly manipulate a 'tags' array.
            ScriptableObject.putProperty(scope, "tags", Context.javaToJS(scriptResult.tags, scope))

            // --- Legacy API (Global Functions) ---
            // Expose the result object to the script under a known name.
            ScriptableObject.putProperty(scope, "scriptResult", Context.javaToJS(scriptResult, scope))

            // Define legacy functions in JS that manipulate the exposed 'scriptResult' object.
            val legacyFunctionDefinitions = """
                function addTag(tag) { scriptResult.tags.add(tag); }
                function setSeverity(level) { scriptResult.severity = level; }
                function createNote(note) { scriptResult.note = note; }
                function linkToAllegation(allegation) { scriptResult.linkedAllegation = allegation; }
            """.trimIndent()
            rhino.evaluateString(scope, legacyFunctionDefinitions, "LegacyAPISetup", 1, null)


            // --- Context & Execution ---
            ScriptableObject.putProperty(scope, "evidence", Context.javaToJS(evidence, scope))
            val tags = mutableListOf<String>()
            ScriptableObject.putProperty(scope, "tags", Context.javaToJS(tags, scope))

            // Execute the user's script
            rhino.evaluateString(scope, script, "JavaScript<ScriptRunner>", 1, null)
            val tagsPropertyFromScope: Any? = ScriptableObject.getProperty(scope, "tags")
            val convertedJavaList: Any? = Context.jsToJava(tagsPropertyFromScope, List::class.java)

            // The scriptResult object has been modified by the script in-place,
            // both through the legacy functions and potentially modern array access.
            // No further extraction is needed.

            return Result.Success(scriptResult)

        } catch (e: org.mozilla.javascript.RhinoException) {
            return Result.Error(ScriptExecutionException("Error during JavaScript execution", e))
        } catch (e: Exception) {
            return Result.Error(ScriptExecutionException("An unexpected error occurred while running script", e))
        } finally {
            Context.exit()
        }
    }
}
