package org.wordpress.android.ui.stats.refresh.utils

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class DateUtils @Inject constructor(private val context: Context) {
    fun getWeekDay(dayOfTheWeek: Int): String {
        val c = Calendar.getInstance()
        c.firstDayOfWeek = Calendar.MONDAY
        c.timeInMillis = System.currentTimeMillis()
        when (dayOfTheWeek) {
            0 -> c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            1 -> c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            2 -> c.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            3 -> c.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
            4 -> c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            5 -> c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            6 -> c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        val formatter = SimpleDateFormat("EEEE", Locale.getDefault())
        return formatter.format(c.time).capitalize()
    }

    fun getHour(hour: Int): String {
        val formatter = DateFormat.getTimeFormat(context)
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, 0)
        return formatter.format(c.time)
    }
}
