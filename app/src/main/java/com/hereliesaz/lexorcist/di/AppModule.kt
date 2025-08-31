package com.hereliesaz.lexorcist.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.hereliesaz.lexorcist.GoogleApiService // Assuming this is the correct import
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.SettingsManager // Assuming this is the correct import
import com.hereliesaz.lexorcist.service.ScriptRunner // Assuming this is the correct import
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    @Provides
    @Singleton
    fun provideGoogleApiService(): GoogleApiService? {
        // This will initially be null.
        // AuthViewModel will be responsible for creating and providing the actual instance
        // to other components that need it, likely via a SharedFlow or StateFlow.
        // For direct injection, Hilt provides null if no other @Provides method returns a non-null instance.
        return null
    }

    @Provides
    @Singleton // Or appropriate scope
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        // TODO: Replace with actual constructor and dependencies if any
        return SettingsManager(context)
    }

    @Provides
    @Singleton // Or appropriate scope
    fun provideScriptRunner(): ScriptRunner {
        // TODO: Replace with actual constructor and dependencies if any
        return ScriptRunner() // Assuming a default constructor for now
    }
}
