package com.hereliesaz.lexorcist.di

import android.content.Context
import com.hereliesaz.lexorcist.data.ObjectBoxManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.BoxStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ObjectBoxModule {

    @Singleton
    @Provides
    fun provideObjectBoxManager(@ApplicationContext context: Context): ObjectBoxManager {
        return ObjectBoxManager(context)
    }

    @Singleton
    @Provides
    fun provideBoxStore(objectBoxManager: ObjectBoxManager): BoxStore {
        return objectBoxManager.getBoxStore()
    }
}
