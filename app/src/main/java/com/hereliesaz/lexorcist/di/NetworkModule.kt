package com.hereliesaz.lexorcist.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton // Added for FirebaseAuth provider

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // GoogleApiService is not provided here because it requires a user's credential.
    // It will be created in the AuthViewModel after the user signs in.

    @Provides
    @Singleton // Added to ensure a single instance of FirebaseAuth
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @IODispatcher // Using the imported qualifier
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
}
