package com.hereliesaz.lexorcist.di

import android.content.Context
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.service.GoogleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SheetModule {

    @Provides
    @Singleton
    fun provideGoogleApiService(
        credentialHolder: CredentialHolder,
        @ApplicationContext context: Context
    ): GoogleApiService {
        val credential = credentialHolder.credential
            ?: throw IllegalStateException(
                "GoogleAccountCredential not available. " +
                "Ensure user is signed in before injecting GoogleApiService."
            )
        val applicationName = context.getString(R.string.app_name)
        return GoogleApiService(credential, applicationName)
    }
}
