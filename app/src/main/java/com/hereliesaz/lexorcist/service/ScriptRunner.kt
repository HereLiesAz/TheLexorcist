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

/**
 * A Hilt Singleton service responsible for executing user-provided Javascript scripts
 * within a sandboxed Mozilla Rhino environment.
 *
 * This class sets up the script's execution context, providing a rich API for scripts
 * to interact with application data and services in a controlled manner. It supports
 * both a modern, namespaced API (`lex.ai.local`) and a legacy, global function API
 * (`addTag()`, etc.) for backward compatibility.
 *
 * @property legalBertService The injected service for local AI model interactions.
 */
@Singleton
class ScriptRunner @Inject constructor(
    private val legalBertService: LegalBertService
) {
    /**
     * Custom exception thrown when a script fails to execute or returns an error.
     */
    class ScriptExecutionException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    /**
     * An inner class that acts as a bridge to the `LegalBertService`. An instance of this
     * class is exposed to the Javascript environment under `lex.ai.local`.
     */
    inner class LocalAIApi {
        /**
         * Exposes `LegalBertService.getEmbedding` to scripts.
         * @param text The text to embed.
         * @return An array of Floats representing the text embedding.
         */
        @Suppress("unused") // Used by Rhino
        fun getEmbedding(text: String): Array<Float> {
            return legalBertService.getEmbedding(text).toTypedArray()
        }

        /**
         * Exposes a high-level cosine similarity function to scripts.
         * @param text1 The first text to compare.
         * @param text2 The second text to compare.
         * @return A Double between -1.0 and 1.0 indicating the semantic similarity.
         */
        @Suppress("unused") // Used by Rhino
        fun calculateSimilarity(text1: String, text2: String): Double {
            val embedding1 = legalBertService.getEmbedding(text1)
            val embedding2 = legalBertService.getEmbedding(text2)
            return cosineSimilarity(embedding1, embedding2)
        }
    }

    /**
     * Executes a given script against a piece of evidence.
     *
     * @param script The Javascript code to execute.
     * @param evidence The evidence object to be analyzed by the script.
     * @return A [Result] wrapper containing the populated [ScriptResult] on success,
     *         or a [ScriptExecutionException] on failure.
     */
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

    /**
     * Calculates the cosine similarity between two vectors.
     * @param vec1 The first vector.
     * @param vec2 The second vector.
     * @return A value between -1.0 and 1.0, where 1.0 means identical.
     */
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
