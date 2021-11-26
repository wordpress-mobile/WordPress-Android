package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CardsUtils {
    private const val INSERT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"
    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

    val GSON: Gson by lazy {
        val builder = GsonBuilder()
        builder.setDateFormat(DATE_FORMAT)
        builder.create()
    }

    fun getInsertDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat(INSERT_DATE_FORMAT, Locale.ROOT)
        return dateFormat.format(calendar.time)
    }

    fun fromDate(date: String): Date {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ROOT)
        return dateFormat.parse(date) ?: Date()
    }
}
