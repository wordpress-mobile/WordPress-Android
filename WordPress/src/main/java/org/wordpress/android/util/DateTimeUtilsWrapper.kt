package org.wordpress.android.util

import javax.inject.Inject

class DateTimeUtilsWrapper
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper) {
    fun currentTimeInIso8601UTC(): String =
            DateTimeUtils.iso8601UTCFromTimestamp(localeManagerWrapper.getCurrentCalendar().timeInMillis / 1000)
}
