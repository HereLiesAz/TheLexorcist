package com.hereliesaz.lexorcist.data.repository

import android.content.Context
import com.hereliesaz.lexorcist.data.MasterAllegation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalRepository @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Loads the master list of allegations from the assets/allegations.csv file.
     */
    fun getMasterAllegations(): Flow<List<MasterAllegation>> = flow {
        try {
            val allegations = mutableListOf<MasterAllegation>()
            context.assets.open("allegations.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header line
                    reader.readLine()

                    val regex = "\"(.*?)\"".toRegex()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val matches = regex.findAll(line!!)
                        val tokens = matches.map { it.groupValues[1] }.toList()

                        if (tokens.size >= 7) {
                            val allegation = MasterAllegation(
                                id = tokens[0],
                                name = tokens[1],
                                description = tokens[2],
                                // elements = tokens[3], // This column is ignored
                                category = tokens[4],
                                type = tokens[5],
                                courtLevel = tokens[6]
                                // relevantEvidence = tokens[7] // This column is ignored
                            )
                            allegations.add(allegation)
                        }
                    }
                }
            }
            emit(allegations)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}