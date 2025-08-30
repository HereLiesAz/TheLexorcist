package com.hereliesaz.lexorcist.data

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromString(value: String): List<String> {
        return value.split(",").map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromIntListString(value: String?): List<Int> {
        return value?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    @TypeConverter
    fun toIntListString(list: List<Int>?): String {
        return list?.joinToString(",") ?: ""
    }
}
