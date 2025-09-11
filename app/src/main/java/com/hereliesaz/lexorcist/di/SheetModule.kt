package com.hereliesaz.lexorcist.di

import android.accounts.Account
import android.content.Context
import android.content.SharedPreferences
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.FolderManager
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
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
    fun provideGoogleAccountCredential(
        credentialHolder: com.hereliesaz.lexorcist.auth.CredentialHolder
    ): GoogleAccountCredential? {
        return credentialHolder.credential
    }

    @Provides
    @Singleton
    fun provideDriveService(
        credential: GoogleAccountCredential?,
        @ApplicationContext context: Context,
    ): Drive? {
        if (credential == null) return null
        return Drive
            .Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential,
            ).setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    @Provides
    @Singleton
    fun provideSheetsService(
        credential: GoogleAccountCredential?,
        @ApplicationContext context: Context,
    ): Sheets? {
        if (credential == null) return null
        return Sheets
            .Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential,
            ).setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleApiService(
        drive: Drive?,
        sheets: Sheets?,
    ): GoogleApiService? {
        if (drive == null || sheets == null) {
            return null
        }
        return GoogleApiService(drive, sheets)
    }

    @Provides
    @Singleton
    fun provideFolderManager(
        drive: Drive?,
        @ApplicationContext context: Context,
    ): FolderManager? {
        if (drive == null) return null
        return FolderManager(drive, context)
    }
}
