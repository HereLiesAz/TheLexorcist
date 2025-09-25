package com.hereliesaz.lexorcist.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

object AllegationProvider {

    fun getAllAllegations(context: Context): List<Allegation> {
        return try {
            val jsonString = context.assets.open("allegations.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<Allegation>>() {}.type
            Gson().fromJson(jsonString, listType)
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            emptyList()
        }
    }
}


