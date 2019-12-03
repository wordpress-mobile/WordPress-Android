package org.wordpress.android.util

import java.util.Calendar
import javax.inject.Inject

class DateTimeUtilsWrapper
@Inject constructor() {
    fun iso8601UTCFromCalendar(calendar: Calendar): String =
            DateTimeUtils.iso8601UTCFromTimestamp(calendar.timeInMillis / 1000)
}
