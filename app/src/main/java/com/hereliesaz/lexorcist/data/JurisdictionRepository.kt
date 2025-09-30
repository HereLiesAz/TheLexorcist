package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.model.Court
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JurisdictionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getJurisdictions(): List<Court> {
        return withContext(Dispatchers.IO) {
            val courts = mutableListOf<Court>()
            try {
                context.assets.open("jurisdictions.csv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        // Skip header line
                        reader.readLine()

                        val regex = "\"(.*?)\"".toRegex()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val matches = regex.findAll(line!!)
                            val tokens = matches.map { it.groupValues[1] }.toList()

                            if (tokens.size >= 2) {
                                val court = Court(
                                    id = tokens[0],
                                    courtName = tokens[1]
                                )
                                courts.add(court)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            courts
        }
    }
}