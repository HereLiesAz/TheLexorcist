package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.ScriptResult
import com.hereliesaz.lexorcist.model.UiComponentModel
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for executing user-defined JavaScript code safely.
 *
 * This service uses the Mozilla Rhino engine to run scripts. It enforces a strict security sandbox
 * to prevent malicious code from accessing sensitive system resources or Java classes.
 */
@Singleton
class ScriptRunner @Inject constructor(
    private val generativeAIService: GenerativeAIService,
    private val googleApiService: GoogleApiService,
    private val semanticService: SemanticService
) {

    /**
     * The sandbox.
     *
     * The previous implementation was `rhino.setClassShutter { false }`, with
     * the comment "prevents the script from accessing ANY Java classes". It did
     * exactly that -- including the classes the scripting API is *made of*.
     * Rhino consults the ClassShutter whenever `NativeJavaObject` reflects on a
     * member, so denying every class also denied `evidence.content`,
     * `scriptResult.tags.add(...)` behind the legacy `addTag()` helper, and
     * every `lex.*` bridge method. Nothing that touched the injected objects
     * could run; the resulting `Result.Error` was discarded by the caller, so
     * scripts appeared to execute and simply never tagged anything.
     *
     * This is an allowlist instead: the API's own types plus the collection and
     * value types they expose, with reflection, class loading, process and I/O
     * escapes denied explicitly so that no future addition to the allowlist can
     * accidentally re-open them.
     */
    internal object SandboxClassShutter : ClassShutter {

        /** Denied unconditionally, even if an allowlist rule would match. */
        private val denyPrefixes = listOf(
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.Runtime",
            "java.lang.System",
            "java.lang.Process",
            "java.lang.Thread",
            "java.lang.reflect.",
            "java.lang.invoke.",
            "java.io.",
            "java.nio.",
            "java.net.",
            "javax.script.",
            "sun.",
            "jdk.",
            "android.",
            "dalvik.",
            "kotlin.reflect.",
        )

        private val allowExact = setOf(
            "com.hereliesaz.lexorcist.model.ScriptResult",
            "com.hereliesaz.lexorcist.model.UiComponentModel",
            "java.lang.String",
            "java.lang.CharSequence",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Number",
            "java.lang.Iterable",
            "java.lang.Comparable",
        )

        private val allowPrefixes = listOf(
            // The bridge classes: ScriptRunner$GoogleApi and friends.
            "com.hereliesaz.lexorcist.service.ScriptRunner\u0024",
            // Collections the API hands to scripts: tags, case.evidence, properties.
            "java.util.List",
            "java.util.ArrayList",
            "java.util.Collection",
            "java.util.Collections\u0024",
            "java.util.Map",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.Set",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.Iterator",
            "java.util.AbstractList",
            "java.util.AbstractCollection",
            "java.util.AbstractMap",
            "java.util.AbstractSet",
            "java.util.Arrays\u0024",
            "kotlin.collections.",
            "kotlin.jvm.internal.",
        )

        override fun visibleToScripts(fullClassName: String): Boolean {
            if (denyPrefixes.any { fullClassName == it || fullClassName.startsWith(it) }) return false
            if (fullClassName in allowExact) return true
            return allowPrefixes.any { fullClassName.startsWith(it) }
        }
    }

    /**
     * Exception thrown when script execution fails.
     */
    class ScriptExecutionException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    /**
     * Bridge class to expose Google API functionality to the JavaScript environment.
     * Methods in this class are callable from JS via `lex.google`.
     */
    inner class GoogleApi {
        @Suppress("unused") // Used by Rhino
        fun runAppsScript(scriptId: String, functionName: String, parameters: Array<Any>): Any? {
            // runBlocking is used here because Rhino's execution model is synchronous,
            // but our internal API is suspending.
            return runBlocking {
                when (val result = googleApiService.runGoogleAppsScript(scriptId, functionName, parameters.toList())) {
                    is Result.Success -> result.data
                    is Result.Error -> throw ScriptExecutionException("Error running Google Apps Script", result.exception)
                    is Result.UserRecoverableError -> throw ScriptExecutionException("User recoverable error running Google Apps Script", result.exception)
                    else -> null
                }
            }
        }
    }

    /**
     * Bridge class to expose Generative AI functionality to the JavaScript environment.
     * Methods in this class are callable from JS via `lex.ai`.
     */
    inner class GenerativeAIApi {
        @Suppress("unused") // Used by Rhino
        fun generateContent(prompt: String): String {
            return runBlocking {
                generativeAIService.generateContent(prompt)
            }
        }
    }

    /**
     * Bridge for `lex.ai.local`.
     *
     * [SemanticService.calculateSimilarity] is a `suspend fun`, so the method
     * Rhino sees by reflection takes a trailing `Continuation` and there is no
     * two-argument overload for `lex.ai.local.calculateSimilarity(a, b)` to
     * resolve against. Previously the raw service instance was handed to Rhino
     * via `Context.javaToJS(semanticService, scope)`, so every documented call
     * failed at the interop boundary. This wraps the suspend call the same way
     * [GenerativeAIApi] already did.
     */
    inner class SemanticApi {
        @Suppress("unused") // Used by Rhino
        fun calculateSimilarity(text1: String, text2: String): Float = runBlocking {
            semanticService.calculateSimilarity(text1, text2)
        }
    }

    /**
     * Bridge for `lex.ui`, the dynamic-UI namespace documented in
     * SCRIPT_EXAMPLES.md and used by the seeded scripts in
     * `assets/default_scripts.csv`. It was never registered on the scope, so
     * those scripts threw on every execution.
     *
     * Requests are collected onto [ScriptResult]; the caller decides what to
     * render. Nothing here can reach the view hierarchy directly.
     */
    inner class UiApi(private val result: ScriptResult) {
        @Suppress("unused") // Used by Rhino
        @JvmOverloads
        fun addOrUpdate(
            id: String,
            type: String,
            properties: Any? = null,
            onClick: String? = null,
        ) {
            result.removedUiComponentIds.remove(id)
            result.uiComponents[id] = UiComponentModel(
                id = id,
                type = type,
                properties = properties.toStringMap(),
                onClick = onClick,
            )
        }

        @Suppress("unused") // Used by Rhino
        fun remove(id: String) {
            result.uiComponents.remove(id)
            result.removedUiComponentIds.add(id)
        }

        @Suppress("unused") // Used by Rhino
        fun clearAll() {
            result.uiComponents.clear()
            result.removedUiComponentIds.clear()
            result.clearAllUiComponents = true
        }

        /** Flattens a JS object literal into the string map the model holds. */
        private fun Any?.toStringMap(): Map<String, String> = when (this) {
            null -> emptyMap()
            is Map<*, *> -> entries.mapNotNull { (k, v) ->
                if (k == null || v == null) null else k.toString() to v.jsToString()
            }.toMap()
            is Scriptable -> ids.mapNotNull { key ->
                val name = key?.toString() ?: return@mapNotNull null
                val value = ScriptableObject.getProperty(this, name)
                if (value == null || value == Scriptable.NOT_FOUND) null
                else name to value.jsToString()
            }.toMap()
            else -> mapOf("value" to jsToString())
        }

        /** Renders a JS value as text without Rhino's "1.0" for whole numbers. */
        private fun Any?.jsToString(): String = when (this) {
            null -> ""
            is Double -> if (this == kotlin.math.floor(this) && !this.isInfinite()) {
                this.toLong().toString()
            } else {
                this.toString()
            }
            else -> toString()
        }
    }

    /**
     * Executes a script against a specific piece of evidence.
     *
     * @param script The JavaScript source code.
     * @param evidence The [Evidence] object to be processed/analyzed.
     * @return A [Result] containing the [ScriptResult] (tags, notes, etc.) or an error.
     */
    @JvmOverloads
    suspend fun runScript(
        script: String,
        evidence: Evidence,
        /** The rest of the case, exposed to scripts as `case.evidence`. */
        caseEvidence: List<Evidence> = emptyList(),
    ): Result<ScriptResult> = withContext(Dispatchers.Default) {
        val rhino = Context.enter()
        // IMPORTANT: optimizationLevel = -1 is required for Android compatibility.
        // Higher levels use dynamic bytecode generation which is not supported by Dalvik/ART.
        @Suppress("deprecation")
        rhino.optimizationLevel = -1

        // SECURITY SANDBOX: see SandboxClassShutter.
        rhino.setClassShutter(SandboxClassShutter)

        try {
            val scope: Scriptable = rhino.initStandardObjects()
            val scriptResult = ScriptResult()

            // --- Modern API (`lex` object) ---
            // Construct the `lex` global object and its sub-namespaces (`ai`, `google`, `local`).
            val lexObject = rhino.newObject(scope)
            ScriptableObject.putProperty(scope, "lex", lexObject)

            val aiObject = rhino.newObject(scope)
            ScriptableObject.putProperty(lexObject, "ai", aiObject)
            ScriptableObject.putProperty(aiObject, "generate", Context.javaToJS(GenerativeAIApi(), scope))
            ScriptableObject.putProperty(aiObject, "local", Context.javaToJS(SemanticApi(), scope))

            val googleApiObject = rhino.newObject(scope)
            ScriptableObject.putProperty(lexObject, "google", googleApiObject)
            ScriptableObject.putProperty(googleApiObject, "runAppsScript", Context.javaToJS(GoogleApi(), scope))

            ScriptableObject.putProperty(lexObject, "ui", Context.javaToJS(UiApi(scriptResult), scope))

            // Expose a direct `tags` list for convenience.
            ScriptableObject.putProperty(scope, "tags", Context.javaToJS(scriptResult.tags, scope))

            // --- Legacy API (Global Functions) ---
            // Inject helper functions for backward compatibility with older scripts.
            ScriptableObject.putProperty(scope, "scriptResult", Context.javaToJS(scriptResult, scope))
            val legacyFunctionDefinitions = """
                function addTag(tag) { scriptResult.tags.add(tag); }
                function setSeverity(level) { scriptResult.severity = level; }
                function createNote(note) { scriptResult.note = note; }
                function linkToAllegation(allegation) { scriptResult.linkedAllegation = allegation; }
            """.trimIndent()
            rhino.evaluateString(scope, legacyFunctionDefinitions, "LegacyAPISetup", 1, null)

            // --- Context & Execution ---
            // Inject evidence as a plain JS object rather than a live Java
            // object. Two reasons: every documented example reads
            // `evidence.text` while the Kotlin property is `content`, so
            // LiveConnect exposed no such member and each of those scripts died
            // on `Cannot call method "toLowerCase" of undefined`; and a data
            // view keeps the script API stable if the Kotlin data class is
            // refactored, and gives scripts no handle on a real Java instance.
            ScriptableObject.putProperty(scope, "evidence", evidence.toJsView(rhino, scope))

            // The rest of the case. SCRIPT_EXAMPLES.md documents this as a
            // bare `case` global, which cannot work: `case` is a reserved word
            // in JavaScript, so `case.evidence` is a syntax error before the
            // script even runs -- the documented examples were never valid JS.
            // It is exposed as `lex.case` (legal: reserved words are permitted
            // after a dot) and as a `caseEvidence` global.
            val caseViews = caseEvidence.map { it.toJsView(rhino, scope) }
            val caseObject = rhino.newObject(scope)
            ScriptableObject.putProperty(lexObject, "case", caseObject)
            // Rhino's newArray requires an exact Object[]; Kotlin's
            // toTypedArray() yields a covariant subtype (Scriptable[],
            // String[]) which it rejects with IllegalArgumentException.
            ScriptableObject.putProperty(caseObject, "evidence", jsArray(rhino, scope, caseViews))
            // Distinct allegation names across the case. Seeded scripts read
            // this (previously as a bare `case.allegations`, which could not
            // parse) to decide whether a tag is relevant to the claim.
            ScriptableObject.putProperty(
                caseObject,
                "allegations",
                jsArray(rhino, scope, caseEvidence.mapNotNull { it.allegationId }.distinct()),
            )
            ScriptableObject.putProperty(scope, "caseEvidence", jsArray(rhino, scope, caseViews))

            // Execute the user's script.
            rhino.evaluateString(scope, script, "JavaScript<ScriptRunner>", 1, null)

            return@withContext Result.Success(scriptResult)

        } catch (e: org.mozilla.javascript.RhinoException) {
            return@withContext Result.Error(ScriptExecutionException("Error during JavaScript execution", e))
        } catch (e: Exception) {
            return@withContext Result.Error(ScriptExecutionException("An unexpected error occurred while running script or processing results", e))
        } finally {
            Context.exit()
        }
    }

    /**
     * Executes a generic script with a custom set of context objects.
     * Useful for background tasks or utility scripts that don't operate on a specific evidence item.
     *
     * @param script The JavaScript source code.
     * @param contextObjects A map of objects to expose to the script scope (key = variable name).
     */
    suspend fun runGenericScript(
        script: String,
        contextObjects: Map<String, Any>
    ): Result<Any?> {
        return withContext(Dispatchers.Default) {
            val rhino = Context.enter()
            @Suppress("deprecation")
            rhino.optimizationLevel = -1
            rhino.setClassShutter(SandboxClassShutter)
            try {
                val scope: Scriptable = rhino.initStandardObjects()

                // Setup the lex object with google api
                val lexObject = rhino.newObject(scope)
                ScriptableObject.putProperty(scope, "lex", lexObject)
                val googleApiObject = rhino.newObject(scope)
                ScriptableObject.putProperty(lexObject, "google", googleApiObject)
                ScriptableObject.putProperty(googleApiObject, "runAppsScript", Context.javaToJS(GoogleApi(), scope))

                // Generic scripts return a value rather than a ScriptResult, so
                // lex.ui is present but its requests are discarded. Registering
                // it keeps `lex.ui` from being undefined here too.
                ScriptableObject.putProperty(
                    lexObject,
                    "ui",
                    Context.javaToJS(UiApi(ScriptResult()), scope),
                )

                // Add context objects to the scope
                for ((key, value) in contextObjects) {
                    ScriptableObject.putProperty(scope, key, Context.javaToJS(value, scope))
                }

                val result = rhino.evaluateString(scope, script, "GenericScript", 1, null)

                // Convert the result to a Kotlin type
                val kotlinResult = if (result is org.mozilla.javascript.Undefined) {
                    null
                } else {
                    Context.jsToJava(result, Any::class.java)
                }
                Result.Success(kotlinResult)

            } catch (e: org.mozilla.javascript.RhinoException) {
                Result.Error(ScriptExecutionException("Error during JavaScript execution", e))
            } catch (e: Exception) {
                Result.Error(ScriptExecutionException("An unexpected error occurred while running script", e))
            } finally {
                Context.exit()
            }
        }
    }

    /**
     * Renders an [Evidence] as a plain JavaScript object for the script scope.
     *
     * `text` is an alias of `content`: every example in SCRIPT_EXAMPLES.md
     * reads `evidence.text`, and there has never been such a property on the
     * Kotlin class.
     */
    private fun Evidence.toJsView(rhino: Context, scope: Scriptable): Scriptable {
        val view = rhino.newObject(scope)
        ScriptableObject.putProperty(view, "id", id)
        ScriptableObject.putProperty(view, "caseId", caseId.toDouble())
        ScriptableObject.putProperty(view, "type", type)
        ScriptableObject.putProperty(view, "content", content)
        ScriptableObject.putProperty(view, "text", content)
        ScriptableObject.putProperty(view, "formattedContent", formattedContent ?: "")
        ScriptableObject.putProperty(view, "sourceDocument", sourceDocument)
        ScriptableObject.putProperty(view, "category", category)
        ScriptableObject.putProperty(view, "commentary", commentary ?: "")
        ScriptableObject.putProperty(view, "timestamp", timestamp.toDouble())
        ScriptableObject.putProperty(view, "documentDate", documentDate.toDouble())
        // 0L is OcrProcessingService.DATE_NOT_ESTABLISHED. Surfacing it as a
        // boolean stops scripts treating "the epoch" as a real date.
        ScriptableObject.putProperty(view, "hasDocumentDate", documentDate > 0L)
        ScriptableObject.putProperty(view, "allegationId", allegationId ?: "")
        ScriptableObject.putProperty(view, "fileHash", fileHash ?: "")
        ScriptableObject.putProperty(view, "tags", jsArray(rhino, scope, tags))
        val entitiesObject = rhino.newObject(scope)
        entities.forEach { (key, values) ->
            ScriptableObject.putProperty(entitiesObject, key, jsArray(rhino, scope, values))
        }
        ScriptableObject.putProperty(view, "entities", entitiesObject)
        return view
    }


    /**
     * Builds a JS array from [items].
     *
     * `Context.newArray(Scriptable, Object[])` requires the argument's runtime
     * class to be exactly `Object[]` and throws `IllegalArgumentException`
     * otherwise. Kotlin's `toTypedArray()` produces the covariant element type
     * (`String[]`, `Scriptable[]`), which Rhino rejects, so the array is
     * allocated as `Array<Any>` explicitly.
     */
    private fun jsArray(rhino: Context, scope: Scriptable, items: List<Any>): Scriptable {
        val elements = Array<Any>(items.size) { items[it] }
        return rhino.newArray(scope, elements)
    }

}
