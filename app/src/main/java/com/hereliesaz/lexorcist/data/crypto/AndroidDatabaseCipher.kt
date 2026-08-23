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
 * Supplies the real [DatabaseCipher], keyed from the Android Keystore.
 *
 * Mirrors the arrangement `TinkSecureStorage` already uses for OAuth tokens: a
 * Tink keyset in its own SharedPreferences file, itself wrapped by a
 * hardware-backed master key, so the on-disk keyset is useless off the device.
 */
@Singleton
class AndroidDatabaseCipherProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * The cipher to use, or [NoOpDatabaseCipher] when no key could be obtained.
     *
     * Deliberately degrades to plaintext rather than throwing. This is
     * constructed on the storage path at startup, and the Android Keystore can
     * throw or come back reset on some devices and custom ROMs; failing hard
     * there would mean the app cannot open any case at all. Losing encryption
     * on those devices leaves them exactly where every device was before this
     * existed, whereas failing hard loses the user their data access. The
     * fallback is logged, and [DatabaseCipher.isEnabled] lets callers report it.
     */
    val cipher: DatabaseCipher by lazy { build() }

    private fun build(): DatabaseCipher = try {
        StreamingAeadConfig.register()
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        StreamingAeadDatabaseCipher(handle.getPrimitive(StreamingAead::class.java))
    } catch (e: Exception) {
        // Note the difference from TinkSecureStorage's fallback: that one
        // discards the unreadable keyset and generates a fresh unencrypted one,
        // which is safe for OAuth tokens because they can simply be re-acquired
        // by signing in again. Doing the same here would destroy the key that
        // the user's existing database is encrypted under, so the keyset is
        // left untouched and the failure is surfaced instead.
        Log.e(TAG, "Database keyset unavailable; the case database will not be encrypted.", e)
        NoOpDatabaseCipher
    }

    private companion object {
        const val TAG = "DatabaseCipher"
        const val KEYSET_NAME = "lexorcist_database_keyset"
        const val KEYSET_PREFS_NAME = "lexorcist_database_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://lexorcist_database_master_key"
    }
}
