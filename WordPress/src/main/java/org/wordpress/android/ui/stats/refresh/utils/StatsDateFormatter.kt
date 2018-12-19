package org.wordpress.android.ui.stats.refresh.utils

import org.apache.commons.text.WordUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val STATS_INPUT_FORMAT = "yyyy-MM-dd"
private const val MONTH_FORMAT = "MMM, yyyy"
private const val YEAR_FORMAT = "yyyy"
@Suppress("CheckStyle")
private const val REMOVE_YEAR = "([^\\p{Alpha}']|('[\\p{Alpha}]+'))*y+([^\\p{Alpha}']|('[\\p{Alpha}]+'))*"

class StatsDateFormatter
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper, val resourceProvider: ResourceProvider) {
    private val inputFormat: SimpleDateFormat
        get() {
            return SimpleDateFormat(STATS_INPUT_FORMAT, localeManagerWrapper.getLocale())
        }
    private val outputMonthFormat: SimpleDateFormat
        get() {
            return SimpleDateFormat(MONTH_FORMAT, localeManagerWrapper.getLocale())
        }
    private val outputYearFormat: SimpleDateFormat
        get() {
            return SimpleDateFormat(YEAR_FORMAT, localeManagerWrapper.getLocale())
        }
    private val outputFormat: DateFormat
        get() {
            return DateFormat.getDateInstance(DateFormat.MEDIUM, localeManagerWrapper.getLocale())
        }
    private val outputFormatWithoutYear: SimpleDateFormat
        get() {
            val sdf = outputFormat as SimpleDateFormat
            sdf.applyPattern(sdf.toPattern().replace(REMOVE_YEAR.toRegex(), ""))
            return sdf
        }

    fun printDate(text: String): String {
        return printDate(inputFormat.parse(text))
    }

    private fun printDate(date: Date): String {
        try {
            return outputFormat.format(date)
        } catch (e: ParseException) {
            throw RuntimeException("Unexpected date format")
        }
    }

    fun printGranularDate(date: Date, granularity: StatsGranularity): String {
        return when (granularity) {
            DAYS -> outputFormat.format(date)
            WEEKS -> {
                val endCalendar = Calendar.getInstance()
                endCalendar.time = date
                // last day of this week
                val startCalendar = Calendar.getInstance()
                startCalendar.time = endCalendar.time
                startCalendar.add(Calendar.DAY_OF_WEEK, -6)
                return if (startCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)) {
                    resourceProvider.getString(
                            R.string.stats_from_to_dates_in_week_label,
                            outputFormat.format(startCalendar.time),
                            outputFormat.format(endCalendar.time)
                    )
                } else {
                    resourceProvider.getString(
                            R.string.stats_from_to_dates_in_week_label,
                            outputFormatWithoutYear.format(startCalendar.time),
                            outputFormatWithoutYear.format(endCalendar.time)
                    )
                }
            }
            MONTHS -> WordUtils.capitalize(outputMonthFormat.format(date))
            YEARS -> outputYearFormat.format(date)
        }
    }

    fun printGranularDate(date: String, granularity: StatsGranularity): String {
        val parsedDate = parseStatsDate(granularity, date)
        return printGranularDate(parsedDate, granularity)
    }

    fun parseStatsDate(
        granularity: StatsGranularity,
        date: String
    ): Date {
        return when (granularity) {
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
    }

    fun todaysDateInStatsFormat(): String {
        return inputFormat.format(Date())
    }
}
