package com.hereliesaz.lexorcist.service

import android.content.Context
import com.google.api.client.json.webtoken.JsonWebSignature
import com.hereliesaz.lexorcist.model.Evidence
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class ScriptRunner {

    fun runScript(script: String, evidence: Evidence): JsonWebSignature.Parser {
        val rhino = Context.enter()
        rhino.optimizationLevel = -1 // Required for Android
        try {
            val scope = rhino.initStandardObjects()
            val parser = Parser()
            ScriptableObject.putProperty(scope, "evidence", Context.javaToJS(evidence, scope))
            ScriptableObject.putProperty(scope, "parser", Context.javaToJS(parser, scope))
            rhino.evaluateString(scope, script, "script", 1, null)
            return parser
        } finally {
            Context.exit()
        }
    }
}