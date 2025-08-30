package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.flow.Flow
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceRepositoryImpl @Inject constructor(
    private val evidenceDao: EvidenceDao,
    private val googleApiService: GoogleApiService?
) : EvidenceRepository {
    private var googleApiService: GoogleApiService? = null


    private var caseSpreadsheetId: String? = null
    private var caseScriptId: String? = null

    override fun setGoogleApiService(googleApiService: GoogleApiService?) {
        this.googleApiService = googleApiService
    }

    override fun setCaseSpreadsheetId(id: String) {
        this.caseSpreadsheetId = id
    }

    override fun setCaseScriptId(id: String) {
        this.caseScriptId = id
    }

    override fun getEvidenceForCase(caseId: Long): Flow<List<Evidence>> {
        return evidenceDao.getEvidenceForCase(caseId)
    }

    override fun getEvidence(id: Int): Flow<Evidence> {
        return evidenceDao.getEvidence(id)
    }

    override suspend fun addEvidence(evidence: Evidence) {
        evidenceDao.insert(evidence)
    override suspend fun getEvidenceById(id: Int): Evidence? {
        return evidenceDao.getEvidenceById(id)
    }


    override suspend fun addEvidence(caseId: Int, content: String, sourceDocument: String, category: String, allegationId: Int?) {
        // Create a temporary evidence object to run the script against
        val tempEvidenceForScript = Evidence(
            id = -1, // Dummy ID
            caseId = caseId,
            content = content,
            timestamp = System.currentTimeMillis(),
            sourceDocument = sourceDocument,
            documentDate = System.currentTimeMillis(),
            allegationId = allegationId,
            category = category,
            tags = emptyList()
        )

        var tags = emptyList<String>()
        googleApiService?.let { api ->
            caseScriptId?.let { scriptId ->
                val scriptContent = api.getScript(scriptId)?.files?.find { it.name == "Code" }?.source
                if (scriptContent != null) {
                    val scriptRunner = com.hereliesaz.lexorcist.service.ScriptRunner()
                    val result = scriptRunner.runScript(scriptContent, tempEvidenceForScript)
                    tags = result.tags
                }
            }
        }

        val evidenceForSheet = tempEvidenceForScript.copy(tags = tags)

        googleApiService?.let { api ->
            caseSpreadsheetId?.let { spreadsheetId ->
                val newId = api.addEvidenceToCase(spreadsheetId, evidenceForSheet)
                if (newId != null) {
                    val finalEvidence = evidenceForSheet.copy(id = newId)
                    evidenceDao.insert(finalEvidence)
                }
            }
        }
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        var evidenceToUpdate = evidence
        googleApiService?.let { api ->
            caseScriptId?.let { scriptId ->
                val scriptContent = api.getScript(scriptId)?.files?.find { it.name == "Code" }?.source
                if (scriptContent != null) {
                    val scriptRunner = com.hereliesaz.lexorcist.service.ScriptRunner()
                    val result = scriptRunner.runScript(scriptContent, evidence)
                    evidenceToUpdate = evidence.copy(tags = result.tags)
                }
            }
        }
        evidenceDao.update(evidenceToUpdate)
        googleApiService?.let { api ->
            caseSpreadsheetId?.let { spreadsheetId ->
                api.updateEvidenceInCase(spreadsheetId, evidenceToUpdate)
            }
        }
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        evidenceDao.delete(evidence)
        googleApiService?.let { api ->
            caseSpreadsheetId?.let { spreadsheetId ->
                api.deleteEvidenceFromCase(spreadsheetId, evidence.id)
            }
        }
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        evidenceDao.updateCommentary(id, commentary)
    }
}
