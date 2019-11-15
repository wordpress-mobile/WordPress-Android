package org.wordpress.android.util

import java.util.Calendar
import javax.inject.Inject

class DateTimeUtilsWrapper
@Inject constructor() {
    fun iso8601FromCalendar(calendar: Calendar) =
            DateTimeUtils.iso8601FromTimestamp(calendar.timeInMillis / 1000)
}
