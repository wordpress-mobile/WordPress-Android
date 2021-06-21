package org.wordpress.android.ui.reader.utils

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class DateProvider @Inject constructor() {
    fun getCurrentDate() = Date()
    fun getFirstDayOfTheWeek() = Calendar.getInstance().firstDayOfWeek
    fun getShortWeekdays() = DateFormatSymbols(Locale.getDefault()).shortWeekdays
}
