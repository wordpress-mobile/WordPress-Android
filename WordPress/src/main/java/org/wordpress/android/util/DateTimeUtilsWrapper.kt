package org.wordpress.android.util

import javax.inject.Inject

class DateTimeUtilsWrapper
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper) {
    fun currentTimeInIso8601(): String =
            DateTimeUtils.iso8601FromTimestamp(localeManagerWrapper.getCurrentCalendar().timeInMillis / 1000)
}
