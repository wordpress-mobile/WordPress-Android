package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.util.LocaleManagerWrapper
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import javax.inject.Inject

class DaysProvider
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper) {
    fun getDaysOfWeekByLocale(): List<DayOfWeek> {
        val firstDayOfTheWeek = WeekFields.of(localeManagerWrapper.getLocale()).firstDayOfWeek
        return (START_OFFSET..END_OFFSET).map { firstDayOfTheWeek.plus(it) }
    }

    companion object {
        private const val START_OFFSET = 0
        private const val END_OFFSET = 6L
    }
}
