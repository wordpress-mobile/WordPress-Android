package org.wordpress.android.fluxc.persistence.coverters

import androidx.room.TypeConverter

class StringListConverter {
    companion object {
        private const val SEPARATOR = ","
    }

    @TypeConverter
    fun listToString(value: List<String>): String = value.joinToString(separator = SEPARATOR)

    @TypeConverter
    fun stringToList(value: String): List<String> = if (value.isEmpty()) {
        emptyList()
    } else {
        value.split(SEPARATOR).toList()
    }
}
