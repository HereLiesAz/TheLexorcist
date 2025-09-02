package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.CaseRepositoryImpl
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.EvidenceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.hereliesaz.lexorcist.data.AllegationsRepository
import com.hereliesaz.lexorcist.data.AllegationsRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEvidenceRepository(
        evidenceRepositoryImpl: EvidenceRepositoryImpl
    ): EvidenceRepository

    @Binds
    @Singleton
    abstract fun bindCaseRepository(
        caseRepositoryImpl: CaseRepositoryImpl
    ): CaseRepository
}
