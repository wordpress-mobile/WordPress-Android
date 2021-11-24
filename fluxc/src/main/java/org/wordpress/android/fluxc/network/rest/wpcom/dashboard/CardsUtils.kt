package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CardsUtils {
    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

    fun fromDate(date: String): Date {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ROOT)
        return dateFormat.parse(date) ?: Date()
    }
}
