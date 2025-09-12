package com.hereliesaz.lexorcist.auth

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.service.GoogleApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialHolder
    @Inject
    constructor() {
        var credential: GoogleAccountCredential? = null
        var googleApiService: GoogleApiService? = null
    }
