package org.wordpress.android.util

import java.util.Calendar
import java.util.Date

object DateUtils {
    @JvmStatic
    fun addMonths(date: Date?, amount: Int) = add(date, Calendar.MONTH, amount)

    @JvmStatic
    fun addWeeks(date: Date?, amount: Int) = add(date, Calendar.WEEK_OF_YEAR, amount)

    @JvmStatic
    fun addDays(date: Date?, amount: Int) = add(date, Calendar.DAY_OF_MONTH, amount)

    private fun add(date: Date?, calendarField: Int, amount: Int): Date? {
        requireNotNull(date) { "The date must not be null" }
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(calendarField, amount)
        return calendar.time
    }

    @JvmStatic
    fun isSameDay(date1: Date?, date2: Date?): Boolean {
        require(!(date1 == null || date2 == null)) { "The date must not be null" }
        val calendar1 = Calendar.getInstance()
        calendar1.time = date1
        val calendar2 = Calendar.getInstance()
        calendar2.time = date2
        return isSameDay(calendar1, calendar2)
    }

    private fun isSameDay(calendar1: Calendar?, calendar2: Calendar?): Boolean {
        require(!(calendar1 == null || calendar2 == null)) { "The calendar must not be null" }
        return calendar1[Calendar.ERA] == calendar2[Calendar.ERA] &&
                calendar1[Calendar.YEAR] == calendar2[Calendar.YEAR] &&
                calendar1[Calendar.DAY_OF_YEAR] == calendar2[Calendar.DAY_OF_YEAR]
    }
}
