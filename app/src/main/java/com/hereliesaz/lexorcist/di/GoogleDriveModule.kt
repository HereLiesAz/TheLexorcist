package com.hereliesaz.lexorcist.di

import android.content.Context
// Removed: import com.hereliesaz.lexorcist.auth.CredentialHolder // No longer needed here
import com.hereliesaz.lexorcist.data.CloudFile
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.GoogleDriveProvider
// import com.hereliesaz.lexorcist.service.GoogleApiService // Removed for diagnostics
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
        @ApplicationContext context: Context
    ): CloudStorageProvider {
        return GoogleDriveProvider(context) // Now calling the modified constructor
    }
}
