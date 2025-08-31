package com.hereliesaz.lexorcist.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.hereliesaz.lexorcist.GoogleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SheetModule {

    companion object {
        @Provides
        @Singleton
        fun provideGoogleApiService(@ApplicationContext context: Context): GoogleApiService {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            checkNotNull(account) { "User must be signed in to use GoogleApiService" }
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS)
            )
            credential.selectedAccount = account.account
            return GoogleApiService(credential, "The Lexorcist")
        }
    }
}
