package com.hereliesaz.lexorcist.data.repository

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExhibitRepositoryImpl @Inject constructor(
    private val application: Application,
    private val gson: Gson
) : ExhibitRepository {

    override fun getExhibitCatalog(): Flow<List<ExhibitCatalogItem>> = flow {
        try {
            application.assets.open("exhibits_catalog.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val listType = object : TypeToken<List<ExhibitCatalogItem>>() {}.type
                    val exhibits: List<ExhibitCatalogItem> = gson.fromJson(reader, listType)
                    emit(exhibits)
                }
            }
        } catch (e: Exception) {
            // In a real app, more specific error handling would be better.
            // For now, we emit an empty list to prevent crashes.
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO) // Perform file I/O on a background thread.
}