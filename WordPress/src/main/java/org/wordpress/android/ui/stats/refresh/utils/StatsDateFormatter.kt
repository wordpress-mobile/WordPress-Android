package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.util.LocaleManagerWrapper
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
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

    fun todaysDateInStatsFormat(): String {
        return inputFormat.format(Date())
    }
}
