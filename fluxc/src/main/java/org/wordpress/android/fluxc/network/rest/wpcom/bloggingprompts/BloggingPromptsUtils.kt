package org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BloggingPromptsUtils {
    private val DATE_FORMAT: ThreadLocal<DateFormat> = object : ThreadLocal<DateFormat>() {
        override fun initialValue(): DateFormat {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US)
        }
    }

    private val DATE_FORMAT_IGNORES_YEAR: ThreadLocal<DateFormat> = object : ThreadLocal<DateFormat>() {
        override fun initialValue(): DateFormat {
            return SimpleDateFormat("--MM-dd", Locale.US)
        }
    }

    fun dateToString(date: Date, ignoresYear: Boolean = false): String {
        val formatter = if (ignoresYear) {
            DATE_FORMAT_IGNORES_YEAR.get()
        } else {
            DATE_FORMAT.get()
        } as DateFormat
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

    @Suppress("MagicNumber")
    fun yearForDate(date: Date): String = dateToString(date).substring(0, 4)
}
