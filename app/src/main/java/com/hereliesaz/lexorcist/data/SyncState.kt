package com.hereliesaz.lexorcist.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the last successful sync saw, so the next one can tell who changed.
 *
 * Without this, `SyncManager` had only two timestamps to compare -- the local
 * file's `lastModified` and the cloud file's `modifiedTime` -- and whichever
 * was newer overwrote the other outright. That is not a merge and not a
 * conflict resolution; it is a coin toss whose loser's work is gone with no
 * copy and no message. Editing a case on a phone and a tablet on the same day
 * was enough to lose one of them.
 *
 * Recording the state at the last sync turns the same two timestamps into a
 * three-way comparison: it becomes possible to say "only this side moved", and
 * so to know when both did.
 */
@Singleton
class SyncState @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences("lexorcist_sync_state", Context.MODE_PRIVATE)

    /** Fingerprint of the database contents as they stood after the last sync. */
    fun lastSyncedDigest(provider: String): String? = prefs.getString(digestKey(provider), null)

    /** The cloud file's modification time as it stood after the last sync. */
    fun lastSyncedRemoteTime(provider: String): Long = prefs.getLong(remoteTimeKey(provider), 0L)

    fun record(provider: String, digest: String, remoteModifiedTime: Long) {
        prefs.edit {
            putString(digestKey(provider), digest)
            putLong(remoteTimeKey(provider), remoteModifiedTime)
        }
    }

    fun clear(provider: String) {
        prefs.edit {
            remove(digestKey(provider))
            remove(remoteTimeKey(provider))
        }
    }

    private fun digestKey(provider: String) = "digest_$provider"

    private fun remoteTimeKey(provider: String) = "remote_time_$provider"

    companion object {
        /**
         * Content fingerprint.
         *
         * Content rather than mtime: an .xlsx is rewritten wholesale on every
         * save, so its mtime changes when nothing about the case did, and a
         * sync driven by mtime alone would report a conflict for every save on
         * every device.
         */
        fun digestOf(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
    }
}

/** What a sync should do with the database. */
enum class SyncDecision {
    /** Neither side changed since the last sync. */
    NOTHING_TO_DO,

    /** Only this device changed. */
    UPLOAD,

    /** Only the cloud copy changed. */
    DOWNLOAD,

    /** The cloud has no copy yet. */
    UPLOAD_NEW,

    /**
     * Both changed since the last sync.
     *
     * The case that the previous implementation resolved by comparing two
     * timestamps and overwriting the loser.
     */
    CONFLICT,
}

/**
 * Decides what to do, from what the last sync recorded.
 *
 * Pure, and separated from the provider calls, because this is the logic that
 * lost people's work and it should be possible to state its behaviour as
 * tests rather than by reading it.
 *
 * @param localDigest fingerprint of the local database now.
 * @param lastSyncedDigest fingerprint recorded after the last successful sync,
 *   or null when this device has never synced with this provider.
 * @param remoteExists whether the cloud holds a database.
 * @param remoteModifiedTime the cloud copy's modification time now.
 * @param lastSyncedRemoteTime the modification time recorded after the last sync.
 */
fun decideDatabaseSync(
    localDigest: String,
    lastSyncedDigest: String?,
    remoteExists: Boolean,
    remoteModifiedTime: Long,
    lastSyncedRemoteTime: Long,
): SyncDecision {
    if (!remoteExists) return SyncDecision.UPLOAD_NEW

    // No record of a previous sync. The local database may hold work the cloud
    // has never seen and the cloud may hold work this device has never seen,
    // and there is nothing to tell them apart -- so this is a conflict, not an
    // excuse to pick one. A first sync from a device with an empty database is
    // the common case and is handled by the caller, which does not reach here
    // when there is no local database at all.
    if (lastSyncedDigest == null) return SyncDecision.CONFLICT

    val localChanged = lastSyncedDigest != localDigest
    val remoteChanged = remoteModifiedTime != lastSyncedRemoteTime

    return when {
        !localChanged && !remoteChanged -> SyncDecision.NOTHING_TO_DO
        localChanged && !remoteChanged -> SyncDecision.UPLOAD
        !localChanged && remoteChanged -> SyncDecision.DOWNLOAD
        else -> SyncDecision.CONFLICT
    }
}
