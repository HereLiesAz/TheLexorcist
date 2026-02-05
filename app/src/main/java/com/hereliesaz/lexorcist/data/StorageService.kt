package com.hereliesaz.lexorcist.data

import android.net.Uri
import com.hereliesaz.lexorcist.utils.Result

/**
 * An interface that abstracts the underlying storage mechanism for the application.
 * Implementations of this interface can use local files, Google Drive, or any other
 * storage system without changing the data repositories.
 */
interface StorageService {

    // --- Case Management ---

    suspend fun getAllCases(): Result<List<Case>>
    suspend fun getCaseBySpreadsheetId(spreadsheetId: String): Result<Case?>
    suspend fun createCase(case: Case): Result<Case>
    suspend fun updateCase(case: Case): Result<Unit>
    suspend fun deleteCase(case: Case): Result<Unit>

    // --- Evidence Management ---

    suspend fun getEvidenceForCase(caseSpreadsheetId: String): Result<List<Evidence>>
    suspend fun addEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Evidence>
    suspend fun addEvidenceList(caseSpreadsheetId: String, evidenceList: List<Evidence>): Result<List<Evidence>>
    suspend fun updateEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit>
    suspend fun deleteEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit>
    suspend fun uploadFile(caseSpreadsheetId: String, fileUri: Uri, mimeType: String): Result<String>

    // --- Allegation Management ---

    suspend fun getAllegationsForCase(caseSpreadsheetId: String): Result<List<Allegation>>
    suspend fun addAllegation(caseSpreadsheetId: String, allegation: Allegation): Result<Allegation>
    suspend fun removeAllegation(caseSpreadsheetId: String, allegation: Allegation): Result<Unit>

    // --- Transcript Management ---

    suspend fun updateTranscript(evidence: Evidence, newTranscript: String, reason: String): Result<Unit>

    // --- Exhibit Management ---
    suspend fun getExhibitsForCase(caseSpreadsheetId: String): Result<List<Exhibit>>
    suspend fun addExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Exhibit>
    suspend fun updateExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Unit>
    suspend fun deleteExhibit(caseSpreadsheetId: String, exhibit: Exhibit): Result<Unit>

    // --- Synchronization ---

    suspend fun synchronize(): Result<Unit>
}
