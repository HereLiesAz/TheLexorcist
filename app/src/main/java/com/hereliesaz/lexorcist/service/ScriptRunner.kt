package com.hereliesaz.lexorcist.service

import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.model.FinancialEntry
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject

class ScriptRunner {

    fun runScript(script: String, evidence: Evidence): List<FinancialEntry> {
        val rhino = Context.enter()
        rhino.optimizationLevel = -1 // Required for Android
        try {
            val scope = rhino.initStandardObjects()
            val entries = mutableListOf<FinancialEntry>()
            ScriptableObject.putProperty(scope, "evidence", Context.javaToJS(evidence, scope))
            ScriptableObject.putProperty(scope, "entries", Context.javaToJS(entries, scope))
            rhino.evaluateString(scope, script, "script", 1, null)
            return entries
        } finally {
            Context.exit()
        }
    }
}