package com.hereliesaz.lexorcist.auth

import android.app.Application
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.service.GoogleApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialHolder @Inject constructor(private val application: Application) {
    var credential: GoogleAccountCredential? = null

    val googleApiService: GoogleApiService?
        get() {
            return credential?.let { cred ->
                GoogleApiService(cred, "The Lexorcist") // Using "The Lexorcist" as the application name
            }
        }
}
