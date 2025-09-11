package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.GoogleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SheetModule {

    @Provides
    @Singleton
    fun provideGoogleApiService(credentialHolder: CredentialHolder): GoogleApiService? {
        // GoogleApiService is initialized in AuthViewModel and stored in CredentialHolder.
        // This might be null if accessed before sign-in or after sign-out.
        return credentialHolder.googleApiService
    }
}
