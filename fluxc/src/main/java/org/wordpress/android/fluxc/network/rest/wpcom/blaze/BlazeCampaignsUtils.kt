package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BlazeCampaignsUtils {
    private val DATE_FORMAT: ThreadLocal<DateFormat> = object : ThreadLocal<DateFormat>() {
        override fun initialValue(): DateFormat {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
        }
    }

    fun dateToString(date: Date): String {
        val formatter = DATE_FORMAT.get() as DateFormat
        return formatter.format(date)
    }

    fun stringToDate(date: String): Date {
        return try {
            val formatter = DATE_FORMAT.get() as DateFormat
            return formatter.parse(date) ?: Date()
        } catch (exception: ParseException) {
            Date()
        }
    }
}
