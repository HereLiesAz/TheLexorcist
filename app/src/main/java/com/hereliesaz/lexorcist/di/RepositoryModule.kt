package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCaseRepository(
        caseRepositoryImpl: CaseRepositoryImpl
    ): CaseRepository

    @Binds
    abstract fun bindEvidenceRepository(
        evidenceRepositoryImpl: EvidenceRepositoryImpl
    ): EvidenceRepository
}
