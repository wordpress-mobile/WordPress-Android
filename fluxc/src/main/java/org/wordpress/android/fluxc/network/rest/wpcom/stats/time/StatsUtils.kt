package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import org.wordpress.android.fluxc.network.utils.CurrentDateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

const val DATE_FORMAT_DAY = "yyyy-MM-dd"

class StatsUtils
@Inject constructor(private val currentDateUtils: CurrentDateUtils) {
    fun getFormattedDate(date: Date? = null): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT_DAY, Locale.ROOT)
        return dateFormat.format(date ?: currentDateUtils.getCurrentDate())
    }
}
