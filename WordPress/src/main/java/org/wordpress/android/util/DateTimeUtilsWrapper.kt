package org.wordpress.android.util

import org.wordpress.android.viewmodel.ContextProvider
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
}
