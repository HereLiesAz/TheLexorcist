package com.hereliesaz.lexorcist.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DropboxAuthManager @Inject constructor(
    private val requestConfig: DbxRequestConfig,
    private val sharedPreferences: SharedPreferences
) {
    private var accessToken: String? = null
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    init {
        accessToken = sharedPreferences.getString("dropbox_access_token", null)
        _isAuthenticated.value = accessToken != null
    }

    fun setAccessToken(token: String) {
        this.accessToken = token
        sharedPreferences.edit {
            putString("dropbox_access_token", token)
        }
        _isAuthenticated.value = true
    }

    fun clearAccessToken() {
        this.accessToken = null
        sharedPreferences.edit {
            remove("dropbox_access_token")
        }
        _isAuthenticated.value = false
    }

    fun getClient(): DbxClientV2? {
        return accessToken?.let {
            DbxClientV2(requestConfig, it)
        }
    }
}
