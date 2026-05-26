package com.hereliesaz.lexorcist.auth

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores small secrets (e.g. OAuth credentials) encrypted at rest using Google Tink AEAD.
 *
 * The AEAD keyset is persisted in a dedicated SharedPreferences file and is itself wrapped by a
 * master key held in the Android Keystore, so the on-disk values are useless without the device's
 * hardware-backed key. Each value is bound to its storage key as associated data, so ciphertext
 * cannot be moved between keys.
 */
@Singleton
class TinkSecureStorage
@Inject
constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetHandle =
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    /** Encrypts and stores [value] under [key]. */
    fun putString(key: String, value: String) {
        val ciphertext = aead.encrypt(value.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(key, Base64.encodeToString(ciphertext, Base64.NO_WRAP)).apply()
    }

    /** Returns the decrypted value for [key], or null if absent or if decryption fails. */
    fun getString(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return try {
            val plaintext = aead.decrypt(Base64.decode(stored, Base64.NO_WRAP), key.toByteArray(Charsets.UTF_8))
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            // Tampered/corrupt/rotated-key data — treat as absent rather than crashing.
            null
        }
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    companion object {
        private const val PREFS_NAME = "lexorcist_secure_store"
        private const val KEYSET_PREFS_NAME = "lexorcist_tink_keyset_prefs"
        private const val KEYSET_NAME = "lexorcist_tink_keyset"
        private const val MASTER_KEY_URI = "android-keystore://lexorcist_secure_store_master_key"
    }
}
