package com.hereliesaz.lexorcist.data

import android.net.Uri
import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CloudStorageService @Inject constructor(
    private val localFileStorageService: LocalFileStorageService,
    private val syncManager: SyncManager,
    @Named("googleDrive") private val googleDriveProvider: CloudStorageProvider,
    @Named("dropbox") private val dropboxProvider: CloudStorageProvider,
    @Named("oneDrive") private val oneDriveProvider: CloudStorageProvider,
    private val settingsManager: SettingsManager
) : StorageService {

    override suspend fun getAllCases(): Result<List<Case>> {
        return localFileStorageService.getAllCases()
    }

    override suspend fun createCase(case: Case): Result<Case> {
        return localFileStorageService.createCase(case)
    }

    override suspend fun updateCase(case: Case): Result<Unit> {
        return localFileStorageService.updateCase(case)
    }

    override suspend fun deleteCase(case: Case): Result<Unit> {
        return localFileStorageService.deleteCase(case)
    }

    override suspend fun getEvidenceForCase(caseSpreadsheetId: String): Result<List<Evidence>> {
        return localFileStorageService.getEvidenceForCase(caseSpreadsheetId)
    }

    override suspend fun addEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Evidence> {
        return localFileStorageService.addEvidence(caseSpreadsheetId, evidence)
    }

    override suspend fun updateEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit> {
        return localFileStorageService.updateEvidence(caseSpreadsheetId, evidence)
    }

    override suspend fun deleteEvidence(caseSpreadsheetId: String, evidence: Evidence): Result<Unit> {
        return localFileStorageService.deleteEvidence(caseSpreadsheetId, evidence)
    }

    override suspend fun uploadFile(caseSpreadsheetId: String, fileUri: Uri, mimeType: String): Result<String> {
        return localFileStorageService.uploadFile(caseSpreadsheetId, fileUri, mimeType)
    }

    override suspend fun getAllegationsForCase(caseSpreadsheetId: String): Result<List<Allegation>> {
        return localFileStorageService.getAllegationsForCase(caseSpreadsheetId)
    }

    override suspend fun addAllegation(caseSpreadsheetId: String, allegation: Allegation): Result<Allegation> {
        return localFileStorageService.addAllegation(caseSpreadsheetId, allegation)
    }

    override suspend fun updateTranscript(evidence: Evidence, newTranscript: String, reason: String): Result<Unit> {
        return localFileStorageService.updateTranscript(evidence, newTranscript, reason)
    }

    override suspend fun synchronize(): Result<Unit> {
        val selectedProvider = settingsManager.getSelectedCloudProvider()
        val cloudStorageProvider = when (selectedProvider) {
            "GoogleDrive" -> googleDriveProvider
            "Dropbox" -> dropboxProvider
            "OneDrive" -> oneDriveProvider
            else -> null
        }

        return if (cloudStorageProvider != null) {
            syncManager.synchronize(cloudStorageProvider, localFileStorageService)
        } else {
            Result.Success(Unit) // No provider selected, so nothing to sync
        }
    }
}
