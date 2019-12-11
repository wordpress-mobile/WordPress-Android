package org.wordpress.android.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class LocaleManagerWrapper
@Inject constructor() {
    fun getLocale(): Locale = Locale.getDefault()
    fun getTimeZone(): TimeZone = TimeZone.getDefault()
    fun getCurrentCalendar(): Calendar = Calendar.getInstance(getLocale())
}
