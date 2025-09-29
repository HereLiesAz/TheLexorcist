package com.hereliesaz.lexorcist.utils

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant

data class LocationHistory(
    val locations: List<LocationRecord>
)

data class LocationRecord(
    @SerializedName("latitudeE7")
    val latitudeE7: Long,
    @SerializedName("longitudeE7")
    val longitudeE7: Long,
    @SerializedName("timestamp")
    val timestamp: String
) {
    val latitude: Double
        get() = latitudeE7 / 1E7

    val longitude: Double
        get() = longitudeE7 / 1E7

    val timestampMillis: Long
        get() = Instant.parse(timestamp).toEpochMilli()
}


class LocationHistoryParser {
    fun parse(uri: Uri, contentResolver: ContentResolver): List<LocationRecord> {
        val gson = Gson()
        val type = object : TypeToken<LocationHistory>() {}.type

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val locationHistory: LocationHistory = gson.fromJson(reader, type)
                    return locationHistory.locations
                }
            }
        } catch (e: Exception) {
            // Log error or handle exception
            e.printStackTrace()
        }
        return emptyList()
    }
}