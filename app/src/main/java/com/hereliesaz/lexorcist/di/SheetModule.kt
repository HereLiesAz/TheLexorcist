package com.hereliesaz.lexorcist.di

import android.accounts.Account // Added
import android.content.Context
import android.content.SharedPreferences // Added
// Removed: import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel // Added for PREF_USER_EMAIL_KEY
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SheetModule { // Changed to abstract class as @Provides can be in companion or object

    companion object { // Methods need to be in a companion object or the class needs to be an object
        @Provides
        @Singleton
        fun provideGoogleApiService(
            @ApplicationContext context: Context,
            sharedPreferences: SharedPreferences // Added
        ): GoogleApiService? {
            val userEmail = sharedPreferences.getString(AuthViewModel.PREF_USER_EMAIL_KEY, null)

            if (userEmail.isNullOrBlank()) {
                return null // User is not signed in or email not found in prefs
            }

            val account = Account(userEmail, "com.google") // Create Account object

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS)
            )
            credential.selectedAccount = account // Use the created Account
            return GoogleApiService(credential, "The Lexorcist")
        }
    }
}
