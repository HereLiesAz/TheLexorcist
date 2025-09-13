package com.hereliesaz.lexorcist.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SheetModule {
    // Removed provideGoogleApiService method
    // Hilt will provide CredentialHolder directly where needed,
    // and classes will access googleApiService via CredentialHolder.
}
