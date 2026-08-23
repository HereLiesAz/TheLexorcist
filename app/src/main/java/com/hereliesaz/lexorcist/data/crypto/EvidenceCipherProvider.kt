package com.hereliesaz.lexorcist.data.crypto

import android.content.Context
import android.util.Log
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the cipher for original evidence files.
 *
 * The database has been encrypted at rest since the previous change, but the
 * files it points at -- the photographs, screenshots, audio and video that are
 * the actual evidence -- were still written as plaintext into the app's
 * directory. The privacy policy said so explicitly, and this closes it.
 *
 * A separate keyset from the database's, for two reasons. It can be rotated or
 * lost independently: losing the evidence key costs the user their media but
 * leaves the case records readable, and vice versa, rather than making every
 * failure total. And the two stores use distinct associated data, so a
 * ciphertext moved from one to the other fails authentication instead of
 * decrypting into the wrong reader.
 */
@Singleton
class EvidenceCipherProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * The cipher, or [NoOpFileCipher] when no key could be obtained.
     *
     * Degrades to plaintext for the same reason the database cipher does: the
     * Android Keystore can fail or come back reset, and refusing to open the
     * user's own evidence would be worse than storing it as it was stored
     * before this existed. [FileCipher.isEnabled] lets callers report it.
     */
    val cipher: FileCipher by lazy { build() }

    /** The concrete cipher when encryption is active, for the write path. */
    val streamingCipher: StreamingAeadFileCipher? get() = cipher as? StreamingAeadFileCipher

    private fun build(): FileCipher = try {
        StreamingAeadConfig.register()
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        StreamingAeadFileCipher(
            handle.getPrimitive(StreamingAead::class.java),
            FileCipher.EVIDENCE_ASSOCIATED_DATA,
        )
    } catch (e: Exception) {
        Log.e(TAG, "Evidence keyset unavailable; evidence files will not be encrypted.", e)
        NoOpFileCipher
    }

    private companion object {
        const val TAG = "EvidenceCipher"
        const val KEYSET_NAME = "lexorcist_evidence_keyset"
        const val KEYSET_PREFS_NAME = "lexorcist_evidence_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://lexorcist_evidence_master_key"
    }
}
