package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.GoogleApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EvidenceRepositoryImpl @Inject constructor(
    private val googleApiService: GoogleApiService?
) : EvidenceRepository {

    override fun getEvidenceForCase(spreadsheetId: String, caseId: Long): Flow<List<Evidence>> {
        return flow {
            val sheetData = withContext(Dispatchers.IO) {
                googleApiService?.readSpreadsheet(spreadsheetId)
            }
            val evidenceSheet = sheetData?.get("Evidence")
            if (evidenceSheet != null) {
                val evidenceList = evidenceSheet.mapNotNull { row ->
                    try {
                        Evidence(
                            id = row[0].toString().toInt(),
                            caseId = row[1].toString().toLong(),
                            spreadsheetId = spreadsheetId,
                            type = row[2].toString(),
                            content = row[3].toString(),
                            timestamp = row[4].toString().toLong(),
                            sourceDocument = row[5].toString(),
                            documentDate = row[6].toString().toLong(),
                            allegationId = row.getOrNull(7)?.toString()?.toIntOrNull(),
                            category = row[8].toString(),
                            tags = row[9].toString().split(",").map { it.trim() },
                            commentary = row.getOrNull(10)?.toString(),
                            linkedEvidenceIds = row.getOrNull(11)?.toString()?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList(),
                            parentVideoId = row.getOrNull(12)?.toString(),
                            entities = emptyMap() // TODO: Parse entities
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                emit(evidenceList)
            } else {
                emit(emptyList())
            }
        }
    }

    override suspend fun getEvidenceById(id: Int): Evidence? {
        // TODO: Implement using googleApiService to fetch evidence by ID from Google Sheets
        return null // Placeholder
    }

    override fun getEvidence(id: Int): Flow<Evidence> {
        // TODO: Implement using googleApiService to observe a single evidence item
        // This might be complex with Sheets and might need to return a flow that emits once
        // or be rethought based on how Sheets data is fetched.
        return flowOf() // Placeholder for a single item flow, emits nothing
    }

    override suspend fun addEvidence(evidence: Evidence) {
        val values = listOf(
            listOf(
                evidence.id.toString(),
                evidence.caseId.toString(),
                evidence.type,
                evidence.content,
                evidence.timestamp.toString(),
                evidence.sourceDocument,
                evidence.documentDate.toString(),
                evidence.allegationId?.toString() ?: "",
                evidence.category,
                evidence.tags.joinToString(","),
                evidence.commentary ?: "",
                evidence.linkedEvidenceIds.joinToString(","),
                evidence.parentVideoId ?: ""
            )
        )
        googleApiService?.appendData(evidence.spreadsheetId, "Evidence", values)
    }

    override suspend fun updateEvidence(evidence: Evidence) {
        val sheetData = googleApiService?.readSpreadsheet(evidence.spreadsheetId)
        val evidenceSheet = sheetData?.get("Evidence")
        if (evidenceSheet != null) {
            val evidenceList = evidenceSheet.map { row ->
                if (row[0].toString().toInt() == evidence.id) {
                    listOf(
                        evidence.id.toString(),
                        evidence.caseId.toString(),
                        evidence.type,
                        evidence.content,
                        evidence.timestamp.toString(),
                        evidence.sourceDocument,
                        evidence.documentDate.toString(),
                        evidence.allegationId?.toString() ?: "",
                        evidence.category,
                        evidence.tags.joinToString(","),
                        evidence.commentary ?: "",
                        evidence.linkedEvidenceIds.joinToString(","),
                        evidence.parentVideoId ?: ""
                    )
                } else {
                    row
                }
            }
            googleApiService.writeData(evidence.spreadsheetId, "Evidence", evidenceList)
        }
    }

    override suspend fun deleteEvidence(evidence: Evidence) {
        val sheetData = googleApiService?.readSpreadsheet(evidence.spreadsheetId)
        val evidenceSheet = sheetData?.get("Evidence")
        if (evidenceSheet != null) {
            val evidenceList = evidenceSheet.filter { row ->
                row[0].toString().toInt() != evidence.id
            }
            googleApiService.clearSheet(evidence.spreadsheetId, "Evidence")
            googleApiService.writeData(evidence.spreadsheetId, "Evidence", evidenceList)
        }
    }

    override suspend fun updateCommentary(id: Int, commentary: String) {
        // TODO: Implement using googleApiService to update commentary for specific evidence
        // This is complex: requires finding the evidence row by id, then updating the commentary cell.
    }
}
