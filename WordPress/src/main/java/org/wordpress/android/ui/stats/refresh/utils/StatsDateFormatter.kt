package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.util.LocaleManagerWrapper
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val STATS_INPUT_FORMAT = "yyyy-MM-dd"

class StatsDateFormatter
@Inject constructor(localeManagerWrapper: LocaleManagerWrapper) {
    private val inputFormat = SimpleDateFormat(STATS_INPUT_FORMAT, localeManagerWrapper.getLocale())
    private val outputFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, localeManagerWrapper.getLocale())

    fun parseDate(text: String): String {
        try {
            return outputFormat.format(inputFormat.parse(text))
        } catch (e: ParseException) {
            throw RuntimeException("Unexpected date format")
        }
    }

    fun parseGranularDate(date: String, granularity: StatsGranularity): String {
        val parsedDate = when (granularity) {
            DAYS -> inputFormat.parse(date)
            WEEKS -> {
                // first four digits are the year
                // followed by Wxx where xx is the month
                // followed by Wxx where xx is the day of the month
                // ex: 2013W07W22 = July 22, 2013
                val sdf = SimpleDateFormat("yyyy'W'MM'W'dd", Locale.ROOT)
                // Calculate the end of the week
                val parsedDate = sdf.parse(date)
                val calendar: Calendar = Calendar.getInstance()
                calendar.time = parsedDate
                // first day of this week
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                // last day of this week
                calendar.add(Calendar.DAY_OF_WEEK, +6)
                calendar.time
            }
            MONTHS -> {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.ROOT)
                // Calculate the end of the month
                val parsedDate = sdf.parse(date)
                val calendar: Calendar = Calendar.getInstance()
                calendar.time = parsedDate
                // last day of this month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.time
            }
            YEARS -> {
                val sdf = SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT, Locale.ROOT)
                // Calculate the end of the week
                val parsedDate = sdf.parse(date)
                val calendar: Calendar = Calendar.getInstance()
                calendar.time = parsedDate
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.time
            }
        }
        return outputFormat.format(parsedDate)
    }

    fun todaysDateInStatsFormat(): String {
        return inputFormat.format(Date())
    }
}
