package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.model.Court
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JurisdictionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getJurisdictions(): List<Court> {
        return withContext(Dispatchers.IO) {
            val inputStream = context.assets.open("jurisdictions.json")
            val reader = InputStreamReader(inputStream)
            val listType = object : TypeToken<List<Court>>() {}.type
            Gson().fromJson(reader, listType)
        }
    }
}