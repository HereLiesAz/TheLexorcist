package com.hereliesaz.lexorcist.auth

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DropboxAuthManager @Inject constructor(
    private val requestConfig: DbxRequestConfig
) {
    private var accessToken: String? = null

    fun setAccessToken(token: String) {
        this.accessToken = token
    }

    fun getClient(): DbxClientV2? {
        return accessToken?.let {
            DbxClientV2(requestConfig, it)
        }
    }
}
