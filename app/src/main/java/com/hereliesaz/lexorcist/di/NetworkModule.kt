package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.GoogleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import android.app.Application
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.hereliesaz.lexorcist.R
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGoogleApiService(
        credential: GoogleAccountCredential,
        @ApplicationContext context: Application
    ): GoogleApiService {
        return GoogleApiService(credential, context.getString(R.string.app_name))
    }
}
