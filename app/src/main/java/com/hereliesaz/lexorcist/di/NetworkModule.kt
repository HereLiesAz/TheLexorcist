package com.hereliesaz.lexorcist.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // GoogleApiService is now provided dynamically by AuthViewModel
}
