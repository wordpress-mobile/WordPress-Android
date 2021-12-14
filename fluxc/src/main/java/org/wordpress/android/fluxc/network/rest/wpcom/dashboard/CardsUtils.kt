package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CardsUtils {
    private const val INSERT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"

    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    private const val TIMEZONE = "GMT"

    val GSON: Gson by lazy {
        val builder = GsonBuilder()
        builder.registerTypeAdapter(Date::class.java, GsonDateAdapter())
        builder.create()
    }

    fun getInsertDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat(INSERT_DATE_FORMAT, Locale.ROOT)
        return dateFormat.format(calendar.time)
    }

    fun fromDate(date: String): Date {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ROOT)
        dateFormat.timeZone = TimeZone.getTimeZone(TIMEZONE)
        return dateFormat.parse(date) ?: Date()
    }

    /* GSON ADAPTER */

    private class GsonDateAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
        private val dateFormat: DateFormat

        @Synchronized
        override fun serialize(
            date: Date,
            type: Type?,
            jsonSerializationContext: JsonSerializationContext?
        ): JsonElement {
            return JsonPrimitive(dateFormat.format(date))
        }

        @Synchronized
        override fun deserialize(
            jsonElement: JsonElement,
            type: Type?,
            jsonDeserializationContext: JsonDeserializationContext?
        ): Date {
            return try {
                dateFormat.parse(jsonElement.asString) ?: Date()
            } catch (e: ParseException) {
                throw JsonParseException(e)
            }
        }

        init {
            dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ROOT)
            dateFormat.timeZone = TimeZone.getTimeZone(TIMEZONE)
        }
    }
}
