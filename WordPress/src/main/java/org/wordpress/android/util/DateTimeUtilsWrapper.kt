package org.wordpress.android.util

import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ContextProvider
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject

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

    fun dateFromPattern(dateString: String, pattern: String): Date? {
        return runCatching { SimpleDateFormat(pattern).parse(dateString) }
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
}
