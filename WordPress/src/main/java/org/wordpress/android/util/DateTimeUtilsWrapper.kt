package org.wordpress.android.util

import android.icu.util.Calendar
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ContextProvider
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import android.text.format.DateUtils
import java.time.Instant

class DateTimeUtilsWrapper @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val contextProvider: ContextProvider
) {
    fun currentTimeInIso8601(): String =
        DateTimeUtils.iso8601FromTimestamp(localeManagerWrapper.getCurrentCalendar().timeInMillis / 1000)

    fun javaDateToTimeSpan(date: Date?): String = DateTimeUtils.javaDateToTimeSpan(date, contextProvider.getContext())

    fun dateFromIso8601(date: String) = DateTimeUtils.dateFromIso8601(date)

    fun daysBetween(start: Date, end: Date) = DateTimeUtils.daysBetween(start, end)

    fun dateFromTimestamp(timestamp: Long) = DateTimeUtils.dateFromTimestamp(timestamp)

    fun getTodaysDate() = Date(System.currentTimeMillis())

    fun parseDateString(dateString: String, pattern: String): Date? {
        return runCatching { SimpleDateFormat(pattern, Locale.ROOT).parse(dateString) }
            .onFailure { AppLog.e(T.UTILS, "Error parsing date: $dateString", it) }
            .getOrNull()
    }

    fun convertDateFormat(date: String, originalFormatPattern: String, targetDateFormatPattern: String): String? {
        // Format 2020-12-22
        val originalFormat = DateTimeFormatter.ofPattern(originalFormatPattern)
        // Format Decemeber 22, 2020
        val targetFormat = DateTimeFormatter.ofPattern(targetDateFormatPattern)
        return runCatching { LocalDate.parse(date, originalFormat).format(targetFormat) }
            .onFailure { AppLog.e(T.UTILS, "Couldn't parse date: $date", it) }
            .getOrNull()
    }

    /*
    * Returns a string describing 'time' as a time relative to the current time.
    * Use javaDateToTimeSpan function if the date is in the past.
    * Use getRelativeTimeSpanString function if the date is in the future.
    * Time spans in future are formatted like "In 42 minutes | In 2 days".
    * */
    fun getRelativeTimeSpanString(date: Date): String {
        return DateUtils.getRelativeTimeSpanString(date.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
            .toString()
    }

    fun getCalendarInstance(): Calendar {
        return Calendar.getInstance()
    }

    fun getInstantNow(): Instant = Instant.now()

    fun timestampFromIso8601Millis(date: String) = DateTimeUtils.timestampFromIso8601Millis(date)

    fun dateStringFromIso8601MinusMillis(date: String, millisecondsToSubtract: Long) =
        runCatching {
            DateTimeUtils.iso8601FromTimestamp((dateFromIso8601(date).time - millisecondsToSubtract) / 1000)
        }.getOrNull()
}
