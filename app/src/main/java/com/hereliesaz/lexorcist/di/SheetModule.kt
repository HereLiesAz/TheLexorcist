package com.hereliesaz.lexorcist.di

// import com.hereliesaz.lexorcist.auth.CredentialHolder // Potentially unused
// import com.hereliesaz.lexorcist.service.GoogleApiService // Potentially unused
import dagger.Module
// import dagger.Provides // Potentially unused
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
// import javax.inject.Singleton // Potentially unused

@Module
@InstallIn(SingletonComponent::class)
object SheetModule {
    // Removed provideGoogleApiService method
    // Hilt will provide CredentialHolder directly where needed,
    // and classes will access googleApiService via CredentialHolder.
}
