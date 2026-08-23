package com.hereliesaz.lexorcist.data.storage

import android.content.Context
import com.hereliesaz.lexorcist.data.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single answer to "where does this case's data live?".
 *
 * Four components used to compute this independently and disagree:
 *
 * - `LocalFileStorageService` wrote evidence under `filesDir`, honouring a
 *   custom location only when it was a real filesystem path.
 * - `SyncManager` looked under `getExternalFilesDir(null)`, so the file it
 *   tried to sync was one the app never wrote. Its `synchronize` opened with
 *   `if (!spreadsheetFile.exists()) return Success(Unit)` -- so cloud sync
 *   silently did nothing at all.
 * - `CaseViewModel` and `FinalizeCaseDialog` built `File(storageLocation, id)`
 *   straight from the settings string, which after the user picks a folder is a
 *   Storage Access Framework tree URI. `File("content://...")` is a relative
 *   path that resolves against the process working directory; the case
 *   directory never exists, so Finalize offered an empty file list.
 *
 * The settings value is a SAF tree URI, not a path, and a SAF tree cannot be
 * turned into a `File`. Rather than pretend otherwise, this treats a non-path
 * value as "not a usable storage root" and falls back to internal storage --
 * the same rule `LocalFileStorageService` already applied, now applied
 * everywhere.
 */
@Singleton
class CaseStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
) {

    /**
     * Root directory for all case data.
     *
     * Deliberately a `get()` rather than `by lazy`: the previous lazy field
     * captured the directory for the process lifetime, so changing the location
     * in Settings had no effect until the app was killed.
     */
    val root: File
        get() {
            val custom = settingsManager.getStorageLocation()
                ?.takeIf { it.startsWith("/") }
                ?.let { File(it) }
                ?.takeIf { it.isDirectory || it.mkdirs() }
            val dir = custom ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /** True when the configured location is a SAF tree URI we cannot write to as a file. */
    val customLocationIsUnusable: Boolean
        get() = settingsManager.getStorageLocation()?.let { it.isNotBlank() && !it.startsWith("/") } == true

    /** The case database. */
    val databaseFile: File get() = File(root, DATABASE_FILE_NAME)

    /**
     * Directory holding one case's files.
     *
     * The id is sanitised for the same reason `LocalFileStorageService`
     * sanitises it: a spreadsheet id arrives from parsed spreadsheet content
     * and must not be able to escape the storage root.
     */
    fun caseDirectory(caseSpreadsheetId: String): File =
        File(root, sanitizeSafePathSegment(caseSpreadsheetId))

    /** Directory holding one case's original evidence files. */
    fun rawDirectory(caseSpreadsheetId: String): File =
        File(caseDirectory(caseSpreadsheetId), RAW_DIR_NAME)

    /** Every file belonging to a case, or empty when the case has no directory yet. */
    fun caseFiles(caseSpreadsheetId: String): List<File> {
        val dir = caseDirectory(caseSpreadsheetId)
        if (!dir.isDirectory) return emptyList()
        return dir.walkTopDown().filter { it.isFile }.toList()
    }

    /**
     * Resolves a path that is expected to sit inside [root], or null when it
     * escapes. Used by the evidence content provider, where the path arrives
     * from an untrusted caller.
     */
    fun resolveWithinRoot(relativePath: String): File? {
        val base = root.canonicalFile
        val candidate = File(base, relativePath).canonicalFile
        return if (candidate.path == base.path || candidate.path.startsWith(base.path + File.separator)) {
            candidate
        } else {
            null
        }
    }

    /** The path of [file] relative to [root], or null when it is not under it. */
    fun relativize(file: File): String? {
        val base = root.canonicalFile.path
        val target = file.canonicalFile.path
        return if (target.startsWith(base + File.separator)) {
            target.substring(base.length + 1)
        } else {
            null
        }
    }

    companion object {
        const val DATABASE_FILE_NAME = "lexorcist_data.xlsx"
        const val RAW_DIR_NAME = "raw"

        fun sanitizeSafePathSegment(value: String): String =
            value.replace(Regex("[^a-zA-Z0-9\\-_]"), "_")
    }
}
