package com.hereliesaz.lexorcist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The rule that used to lose people's work.
 *
 * `SyncManager` compared `local.lastModified()` with the cloud file's
 * `modifiedTime` and uploaded or downloaded accordingly. Two devices editing
 * the same case load meant the slower one's evidence was overwritten with no
 * copy kept and nothing said. These pin the replacement.
 */
class SyncDecisionTest {

    private val local = "digest-local"
    private val synced = "digest-synced"

    @Test
    fun `an empty cloud gets the local database`() {
        assertEquals(
            SyncDecision.UPLOAD_NEW,
            decideDatabaseSync(local, lastSyncedDigest = null, remoteExists = false, 0, 0),
        )
    }

    @Test
    fun `nothing changed on either side`() {
        assertEquals(
            SyncDecision.NOTHING_TO_DO,
            decideDatabaseSync(synced, synced, remoteExists = true, remoteModifiedTime = 500, lastSyncedRemoteTime = 500),
        )
    }

    @Test
    fun `only this device changed`() {
        assertEquals(
            SyncDecision.UPLOAD,
            decideDatabaseSync(local, synced, remoteExists = true, remoteModifiedTime = 500, lastSyncedRemoteTime = 500),
        )
    }

    @Test
    fun `only the cloud changed`() {
        assertEquals(
            SyncDecision.DOWNLOAD,
            decideDatabaseSync(synced, synced, remoteExists = true, remoteModifiedTime = 900, lastSyncedRemoteTime = 500),
        )
    }

    @Test
    fun `both changed is a conflict, not a race`() {
        assertEquals(
            SyncDecision.CONFLICT,
            decideDatabaseSync(local, synced, remoteExists = true, remoteModifiedTime = 900, lastSyncedRemoteTime = 500),
        )
    }

    @Test
    fun `a newer local copy does not win a conflict`() {
        // The old rule: local.lastModified > remote.modifiedTime, therefore
        // upload, therefore the cloud's changes are gone. A newer local file is
        // now irrelevant when both sides moved.
        val whicheverIsNewer = decideDatabaseSync(
            localDigest = local,
            lastSyncedDigest = synced,
            remoteExists = true,
            remoteModifiedTime = 1,
            lastSyncedRemoteTime = 0,
        )
        assertEquals(SyncDecision.CONFLICT, whicheverIsNewer)
        assertNotEquals(SyncDecision.UPLOAD, whicheverIsNewer)
    }

    @Test
    fun `a first sync against an existing cloud copy is a conflict`() {
        // No record of a previous sync: this device's database and the cloud's
        // may each hold work the other has never seen, and there is nothing to
        // tell them apart. Picking one would be a guess.
        assertEquals(
            SyncDecision.CONFLICT,
            decideDatabaseSync(local, lastSyncedDigest = null, remoteExists = true, remoteModifiedTime = 900, lastSyncedRemoteTime = 0),
        )
    }

    @Test
    fun `a save that changes nothing is not a change`() {
        // An .xlsx is rewritten wholesale on every save, so its mtime moves
        // when the case did not. The digest is of content, so it does not.
        val sameContent = SyncState.digestOf("workbook bytes".toByteArray())
        assertEquals(
            SyncDecision.NOTHING_TO_DO,
            decideDatabaseSync(sameContent, sameContent, remoteExists = true, remoteModifiedTime = 500, lastSyncedRemoteTime = 500),
        )
    }

    @Test
    fun `digests distinguish content`() {
        assertNotEquals(
            SyncState.digestOf("a".toByteArray()),
            SyncState.digestOf("b".toByteArray()),
        )
        assertEquals(
            SyncState.digestOf("a".toByteArray()),
            SyncState.digestOf("a".toByteArray()),
        )
    }
}
