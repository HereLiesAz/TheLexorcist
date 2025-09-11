package com.hereliesaz.lexorcist.di

import android.content.Context
import com.google.gson.Gson
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.InputStreamReader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SchemaModule {
    @Provides
    @Singleton
    fun provideSpreadsheetSchema(
        @ApplicationContext context: Context,
        gson: Gson,
    ): SpreadsheetSchema {
        val inputStream = context.resources.openRawResource(R.raw.spreadsheet_schema)
        return InputStreamReader(inputStream).use { reader ->
            gson.fromJson(reader, SpreadsheetSchema::class.java)
        }
    }
}
