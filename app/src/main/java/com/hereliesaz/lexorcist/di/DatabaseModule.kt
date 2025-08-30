package com.hereliesaz.lexorcist.di

import android.content.Context
import com.hereliesaz.lexorcist.data.AppDatabase
import com.hereliesaz.lexorcist.data.CaseDao
import com.hereliesaz.lexorcist.data.EvidenceDao
import com.hereliesaz.lexorcist.data.FilterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getDatabase(appContext)
    }

    @Provides
    fun provideCaseDao(database: AppDatabase): CaseDao {
        return database.caseDao()
    }

    @Provides
    fun provideEvidenceDao(database: AppDatabase): EvidenceDao {
        return database.evidenceDao()
    }

    @Provides
    fun provideFilterDao(database: AppDatabase): FilterDao {
        return database.filterDao()
    }
}
