package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.util.LocaleManagerWrapper
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import javax.inject.Inject

class DaysProvider
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper) {
    fun getDaysOfWeekByLocale(): List<DayOfWeek> {
        val firstDayOfTheWeek = WeekFields.of(localeManagerWrapper.getLocale()).firstDayOfWeek
        return (0..6L).map { firstDayOfTheWeek.plus(it) }
    }
}
