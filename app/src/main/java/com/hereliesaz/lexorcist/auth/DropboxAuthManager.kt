package com.hereliesaz.lexorcist.auth

import android.util.Log
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the Dropbox OAuth credential and produces authenticated clients.
 *
 * The full [DbxCredential] (short-lived access token + long-lived refresh token) is persisted
 * encrypted via [TinkSecureStorage]. Because the credential carries a refresh token, the
 * [DbxClientV2] built from it transparently refreshes the access token when it expires — there is
 * no manual refresh logic and no plaintext token on disk.
 */
@Singleton
class DropboxAuthManager
@Inject
constructor(
    private val requestConfig: DbxRequestConfig,
    private val secureStorage: TinkSecureStorage,
) {
    private var credential: DbxCredential? = null
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    init {
        credential = loadCredential()
        _isAuthenticated.value = credential != null
    }

    /** Persists the full OAuth credential (access + refresh token), encrypted at rest. */
    fun saveCredential(credential: DbxCredential) {
        // Auth.getDbxCredential() reads AuthActivity's static result, which the SDK never clears,
        // so MainActivity.onResume re-delivers the same credential on every foreground. Skip the
        // redundant encrypt + disk write when it's unchanged, while still persisting a genuinely
        // new credential (e.g. after the user re-authenticates).
        val current = this.credential
        if (current != null &&
            current.accessToken == credential.accessToken &&
            current.refreshToken == credential.refreshToken
        ) {
            return
        }
        this.credential = credential
        secureStorage.putString(KEY_CREDENTIAL, DbxCredential.Writer.writeToString(credential))
        _isAuthenticated.value = true
    }

    fun clearCredential() {
        credential = null
        secureStorage.remove(KEY_CREDENTIAL)
        _isAuthenticated.value = false
    }

    /** Builds an auto-refreshing client, or null if not signed in. */
    fun getClient(): DbxClientV2? = credential?.let { DbxClientV2(requestConfig, it) }

    private fun loadCredential(): DbxCredential? {
        val json = secureStorage.getString(KEY_CREDENTIAL) ?: return null
        return try {
            DbxCredential.Reader.readFully(json)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse stored Dropbox credential; treating as signed out.", e)
            null
        }
    }

    companion object {
        private const val TAG = "DropboxAuthManager"
        private const val KEY_CREDENTIAL = "dropbox_credential"
    }
}
