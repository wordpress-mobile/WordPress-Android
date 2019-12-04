package org.wordpress.android.util

import java.util.Calendar
import javax.inject.Inject

class DateTimeUtilsWrapper
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper) {
    fun currentTimeInIso8601UTC(): String =
            DateTimeUtils.iso8601UTCFromTimestamp(localeManagerWrapper.getCurrentCalendar().timeInMillis / 1000)
}
