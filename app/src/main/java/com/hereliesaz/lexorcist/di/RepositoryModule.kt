package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.AllegationsRepository
import com.hereliesaz.lexorcist.data.AllegationsRepositoryImpl
import com.hereliesaz.lexorcist.data.CaseRepository // Added import
import com.hereliesaz.lexorcist.data.CaseRepositoryImpl // Added import
import com.hereliesaz.lexorcist.data.EvidenceRepository // Added import
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl // Added import
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAllegationsRepository(
        allegationsRepositoryImpl: AllegationsRepositoryImpl
    ): AllegationsRepository

    @Binds
    @Singleton
    abstract fun bindCaseRepository( // Added this binding
        caseRepositoryImpl: CaseRepositoryImpl
    ): CaseRepository

    @Binds
    @Singleton
    abstract fun bindEvidenceRepository( // Added this binding
        evidenceRepositoryImpl: EvidenceRepositoryImpl
    ): EvidenceRepository
}
