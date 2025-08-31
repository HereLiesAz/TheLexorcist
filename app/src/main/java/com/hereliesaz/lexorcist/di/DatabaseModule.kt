package com.hereliesaz.lexorcist.di

import android.content.Context
import androidx.room.Room
import com.hereliesaz.lexorcist.data.AppDatabase
import com.hereliesaz.lexorcist.data.CaseDao
import com.hereliesaz.lexorcist.data.EvidenceDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lexorcist.db"
        ).build()
    }

    @Provides
    fun provideCaseDao(appDatabase: AppDatabase): CaseDao {
        return appDatabase.caseDao()
    }

    @Provides
    fun provideEvidenceDao(appDatabase: AppDatabase): EvidenceDao {
        return appDatabase.evidenceDao()
    }
}
