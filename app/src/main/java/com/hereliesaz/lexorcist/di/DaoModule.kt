package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.CaseDao
import com.hereliesaz.lexorcist.data.EvidenceDao
import com.hereliesaz.lexorcist.data.GoogleSheetsCaseDao
import com.hereliesaz.lexorcist.data.GoogleSheetsEvidenceDao
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DaoModule {

    @Binds
    @Singleton
    abstract fun bindEvidenceDao(impl: GoogleSheetsEvidenceDao): EvidenceDao

    @Binds
    @Singleton
    abstract fun bindCaseDao(impl: GoogleSheetsCaseDao): CaseDao
}
