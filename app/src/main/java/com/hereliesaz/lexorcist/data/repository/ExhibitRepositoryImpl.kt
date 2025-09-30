package com.hereliesaz.lexorcist.data.repository

import android.app.Application
import com.hereliesaz.lexorcist.data.ExhibitCatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExhibitRepositoryImpl @Inject constructor(
    private val application: Application
) : ExhibitRepository {

    override fun getExhibitCatalog(): Flow<List<ExhibitCatalogItem>> = flow {
        try {
            val exhibits = mutableListOf<ExhibitCatalogItem>()
            application.assets.open("exhibits.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header line
                    reader.readLine()

                    val regex = "\"(.*?)\"".toRegex()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val matches = regex.findAll(line!!)
                        val tokens = matches.map { it.groupValues[1] }.toList()

                        if (tokens.size >= 4) {
                            val exhibit = ExhibitCatalogItem(
                                id = tokens[0],
                                type = tokens[1],
                                description = tokens[2],
                                applicableAllegationIds = tokens[3].split(",").map { it.trim().toInt() }
                            )
                            exhibits.add(exhibit)
                        }
                    }
                }
            }
            emit(exhibits)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}