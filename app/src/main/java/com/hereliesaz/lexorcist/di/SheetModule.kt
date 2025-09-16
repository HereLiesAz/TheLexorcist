package com.hereliesaz.lexorcist.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SheetModule {
    // provideGoogleApiService method removed.
    // Hilt will now rely on the @Inject constructor of GoogleApiService.
    // This requires GoogleApiService to be refactored to fetch credentials
    // on-demand within its methods rather than immediately in its constructor.
}
