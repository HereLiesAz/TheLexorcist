package com.hereliesaz.lexorcist.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // GoogleApiService is not provided here because it requires a user's credential.
    // It will be created in the AuthViewModel after the user signs in.
}
