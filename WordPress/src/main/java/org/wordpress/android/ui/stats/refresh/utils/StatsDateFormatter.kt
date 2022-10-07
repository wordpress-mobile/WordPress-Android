package org.wordpress.android.ui.stats.refresh.utils

import org.apache.commons.text.WordUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.math.abs

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

    /**
     * Parses the stats date and prints it in localizes readable format.
     * @param period in this format yyyy-MM-dd
     * @return localized date in the medium format, in English - Jan 5, 2019
     */
    @Suppress("TooGenericExceptionThrown")
    fun printDate(period: String): String {
        try {
            return inputFormat.parse(period)?.let { outputFormat.format(it) }
                    ?: throw RuntimeException("Unexpected date format")
        } catch (e: ParseException) {
            throw RuntimeException("Unexpected date format")
        }
    }

    /**
     * Prints a date in the stats format - yyyy-MM-dd
     * @param date
     * @return date in stats string format
     */
    fun printStatsDate(date: Date): String {
        return inputFormat.format(date)
    }

    /**
     * Prints the given date in a localized format according to the StatsGranularity:
     * DAYS - returns Jan 1, 2019
     * WEEKS - returns Jan 1 - Jan 8
     * MONTHS - returns Jan 2019
     * YEARS - returns 2019
     * @param date to be printed
     * @param granularity defines the output format
     * @return printed date
     */
    fun printGranularDate(date: Date, granularity: StatsGranularity): String {
        return when (granularity) {
            DAYS -> outputFormat.format(date)
            WEEKS -> {
                val endCalendar = Calendar.getInstance()
                endCalendar.time = date
                if (endCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    endCalendar.time = dateToWeekDate(date)
                }
                val startCalendar = Calendar.getInstance()
                startCalendar.time = endCalendar.time
                startCalendar.add(Calendar.DAY_OF_WEEK, -6)
                return printWeek(startCalendar, endCalendar)
            }
            MONTHS -> WordUtils.capitalize(outputMonthFormat.format(date))
            YEARS -> outputYearFormat.format(date)
        }
    }

    /**
     * Prints a date in the Medium format but strips the year. For example prints only Jan 1 instead of Jan 1, 2019
     * @param date
     * @return printed date
     */
    fun printDayWithoutYear(date: Date): String {
        return outputFormatWithoutYear.format(date)
    }

    /**
     * Prints a week with a start and end date in the week format - Jan 1 - Jan 8, 2019.
     * It also adds both years when the week is overlapping - Dec 31, 2018 - Jan 7, 2019
     * @param startPeriod First day of the week
     * @param endPeriod Last day of the week
     * @return printed week
     */
    fun printWeek(startPeriod: Date, endPeriod: Date): String {
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startPeriod
        val endCalendar = Calendar.getInstance()
        endCalendar.time = endPeriod
        return printWeek(startCalendar, endCalendar, showSecondYear = true)
    }

    private fun printWeek(startCalendar: Calendar, endCalendar: Calendar, showSecondYear: Boolean = false): String {
        // Always show both years when the first and the last day of a week are in different years
        return if (startCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)) {
            printWeek(startCalendar, endCalendar, showFirstYear = true, showSecondYear = true)
        } else {
            printWeek(startCalendar, endCalendar, showFirstYear = false, showSecondYear = showSecondYear)
        }
    }

    private fun printWeek(
        startCalendar: Calendar,
        endCalendar: Calendar,
        showFirstYear: Boolean,
        showSecondYear: Boolean
    ): String {
        return resourceProvider.getString(
                R.string.stats_from_to_dates_in_week_label,
                if (showFirstYear) outputFormat.format(startCalendar.time) else outputFormatWithoutYear.format(
                        startCalendar.time
                ),
                if (showSecondYear) outputFormat.format(endCalendar.time) else outputFormatWithoutYear.format(
                        endCalendar.time
                )
        )
    }

    /**
     * Parses the date coming from an endpoint and print the granular result.
     */
    fun printGranularDate(date: String, granularity: StatsGranularity): String {
        val parsedDate = parseStatsDate(granularity, date)
        return printGranularDate(parsedDate, granularity)
    }

    /**
     * Parses date coming from the endpoint in format specific for the stats granularity
     * DAYS -> the input format is yyyy-MM-dd, output is the selected date
     * WEEKS -> the input format is yyyy'W'MM'W'dd, output is the last day of the week
     * MONTHS -> the input format is yyyy-MM, output is the last day of the month
     * YEARS -> the input format is yyyy-MM-dd, output is the last day of the year
     * @param granularity selected granularity
     * @param date string date coming from the endpoints
     * @return parsed Date
     */
    @Suppress("TooGenericExceptionThrown", "ThrowsCount")
    fun parseStatsDate(
        granularity: StatsGranularity,
        date: String
    ): Date {
        return when (granularity) {
            DAYS -> inputFormat.parse(date) ?: throw RuntimeException("Unexpected date format")
            WEEKS -> {
                // first four digits are the year
                // followed by Wxx where xx is the month
                // followed by Wxx where xx is the day of the month
                // ex: 2013W07W22 = July 22, 2013
                val sdf = SimpleDateFormat("yyyy'W'MM'W'dd", Locale.ROOT)
                // Calculate the end of the week
                val parsedDate = sdf.parse(date) ?: throw RuntimeException("Unexpected date format")
                dateToWeekDate(parsedDate)
            }
            MONTHS -> {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.ROOT)
                // Calculate the end of the month
                val parsedDate = sdf.parse(date) ?: throw RuntimeException("Unexpected date format")
                val calendar: Calendar = Calendar.getInstance()
                calendar.time = parsedDate
                // last day of this month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE))
                calendar.time
            }
            YEARS -> {
                val sdf = SimpleDateFormat(STATS_INPUT_FORMAT, Locale.ROOT)
                // Calculate the end of the week
                val parsedDate = sdf.parse(date) ?: throw RuntimeException("Unexpected date format")
                val calendar: Calendar = Calendar.getInstance()
                calendar.time = parsedDate
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.time
            }
        }
    }

    private fun dateToWeekDate(parsedDate: Date): Date {
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = parsedDate
        // first day of this week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        // last day of this week
        calendar.add(Calendar.DAY_OF_WEEK, +6)
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE))
        return calendar.time
    }

    fun printTimeZone(site: SiteModel): String? {
        val siteTimeZone = SiteUtils.getNormalizedTimezone(site.timezone)
        val currentTimeZone = localeManagerWrapper.getTimeZone()
        val currentDate = Calendar.getInstance(localeManagerWrapper.getLocale())
        val siteOffset = siteTimeZone.getOffset(currentDate.timeInMillis)
        val currentTimeZoneOffset = currentTimeZone.getOffset(currentDate.timeInMillis)
        return if (siteOffset != currentTimeZoneOffset) {
            val hourOffset = MILLISECONDS.toHours(siteOffset.toLong())
            val minuteOffset = MILLISECONDS.toMinutes(siteOffset.toLong())
            val timeZoneResource = when {
                minuteOffset > 0L -> R.string.stats_site_positive_utc
                minuteOffset < 0L -> R.string.stats_site_negative_utc
                else -> R.string.stats_site_neutral_utc
            }
            val minuteRemain = minuteOffset % 60
            val utcTime = if (minuteRemain == 0L) {
                "${abs(hourOffset)}"
            } else {
                "${abs(hourOffset)}:${abs(minuteRemain)}"
            }
            resourceProvider.getString(
                    timeZoneResource,
                    utcTime
            )
        } else null
    }
}
