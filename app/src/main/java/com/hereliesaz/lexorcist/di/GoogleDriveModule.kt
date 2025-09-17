package com.hereliesaz.lexorcist.di

import android.content.Context
// Removed: import com.hereliesaz.lexorcist.auth.CredentialHolder // No longer needed here
import com.hereliesaz.lexorcist.data.CloudFile
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.GoogleDriveProvider
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleDriveModule {

    // Removed the provideGoogleApiService method as GoogleApiService is now directly injectable by Hilt
    // @Provides
    // @Singleton
    // fun provideGoogleApiService(credentialHolder: CredentialHolder): GoogleApiService? {
    //     return credentialHolder.googleApiService
    // }

    @Provides
    @Singleton
    @Named("googleDrive")
    fun provideGoogleDriveProvider(
        googleApiService: GoogleApiService, // Changed to non-nullable
        @ApplicationContext context: Context
    ): CloudStorageProvider {
        // The null check for googleApiService is removed as Hilt will provide a non-null instance.
        // If GoogleApiService cannot operate (e.g., no credentials), its methods will handle that,
        // and GoogleDriveProvider should be robust to those responses.
        return GoogleDriveProvider(googleApiService, context)
    }
}
