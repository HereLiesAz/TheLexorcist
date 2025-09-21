package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.ScriptResult
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRunner @Inject constructor(
    private val generativeAIService: GenerativeAIService
) {
    class ScriptExecutionException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    inner class GenerativeAIApi {
        @Suppress("unused") // Used by Rhino
        fun generateContent(prompt: String): String {
            return runBlocking {
                generativeAIService.generateContent(prompt)
            }
        }
    }

    fun runScript(
        script: String,
        evidence: Evidence,
    ): Result<ScriptResult> {
        val rhino = Context.enter()
        @Suppress("deprecation")
        rhino.optimizationLevel = -1 // Necessary for Android compatibility
        try {
            val scope: Scriptable = rhino.initStandardObjects()
            val scriptResult = ScriptResult()

            // --- Modern API (`lex` object) ---
            val lexObject = rhino.newObject(scope)
            ScriptableObject.putProperty(scope, "lex", lexObject)
            val aiObject = rhino.newObject(scope)
            ScriptableObject.putProperty(lexObject, "ai", aiObject)
            ScriptableObject.putProperty(aiObject, "generate", Context.javaToJS(GenerativeAIApi(), scope))
            ScriptableObject.putProperty(scope, "tags", Context.javaToJS(scriptResult.tags, scope))

            // --- Legacy API (Global Functions) ---
            ScriptableObject.putProperty(scope, "scriptResult", Context.javaToJS(scriptResult, scope))
            val legacyFunctionDefinitions = """
                function addTag(tag) { scriptResult.tags.add(tag); }
                function setSeverity(level) { scriptResult.severity = level; }
                function createNote(note) { scriptResult.note = note; }
                function linkToAllegation(allegation) { scriptResult.linkedAllegation = allegation; }
            """.trimIndent()
            rhino.evaluateString(scope, legacyFunctionDefinitions, "LegacyAPISetup", 1, null)

            // --- Context & Execution ---
            ScriptableObject.putProperty(scope, "evidence", Context.javaToJS(evidence, scope))
            rhino.evaluateString(scope, script, "JavaScript<ScriptRunner>", 1, null)

            return Result.Success(scriptResult)

        } catch (e: org.mozilla.javascript.RhinoException) {
            return Result.Error(ScriptExecutionException("Error during JavaScript execution", e))
        } catch (e: Exception) {
            return Result.Error(ScriptExecutionException("An unexpected error occurred while running script or processing results", e))
        } finally {
            Context.exit()
        }
    }
}
