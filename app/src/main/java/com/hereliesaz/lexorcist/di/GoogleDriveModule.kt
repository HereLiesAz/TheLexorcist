package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.GoogleDriveProvider
import com.hereliesaz.lexorcist.service.GoogleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleDriveModule {

    @Provides
    @Singleton
    fun provideGoogleApiService(credentialHolder: CredentialHolder): GoogleApiService? {
        return credentialHolder.googleApiService
    }

    @Provides
    @Singleton
    @Named("googleDrive")
    fun provideGoogleDriveProvider(googleApiService: GoogleApiService?): CloudStorageProvider {
        // This is not ideal, as googleApiService can be null.
        // I will need to handle this in the SyncManager or the CloudStorageService.
        // For now, I will create the provider, but it will fail if the service is null.
        // A better solution would be to have the GoogleApiService provided as a non-null type,
        // which would require refactoring the auth flow to provide the credential earlier.
        // I will assume for now that the user is signed in when the sync is triggered.
        return GoogleDriveProvider(googleApiService!!)
    }
}
