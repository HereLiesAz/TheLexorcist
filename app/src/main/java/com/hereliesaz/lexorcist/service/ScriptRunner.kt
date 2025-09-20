package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.ScriptResult
import com.hereliesaz.lexorcist.service.nlp.LegalBertService
import com.hereliesaz.lexorcist.utils.Result
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ScriptRunner @Inject constructor(
    private val legalBertService: LegalBertService
) {
    class ScriptExecutionException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    /**
     * API wrapper for the local LegalBertService.
     */
    inner class LocalAIApi {
        @Suppress("unused") // Used by Rhino
        fun getEmbedding(text: String): Array<Float> {
            return legalBertService.getEmbedding(text).toTypedArray()
        }

        @Suppress("unused") // Used by Rhino
        fun calculateSimilarity(text1: String, text2: String): Double {
            val embedding1 = legalBertService.getEmbedding(text1)
            val embedding2 = legalBertService.getEmbedding(text2)
            return cosineSimilarity(embedding1, embedding2)
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
            ScriptableObject.putProperty(aiObject, "local", Context.javaToJS(LocalAIApi(), scope))
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

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }
}
