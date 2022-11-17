package org.wordpress.android.ui.bloggingreminders

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.util.LocaleManagerWrapper
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class DaysProviderTest {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    private lateinit var daysProvider: DaysProvider

    @Before
    fun setUp() {
        daysProvider = DaysProvider(localeManagerWrapper)
    }

    @Test
    fun `returns list starting with sunday`() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)

        val days = daysProvider.getDaysOfWeekByLocale()

        assertThat(days).containsExactly(
                SUNDAY,
                MONDAY,
                TUESDAY,
                WEDNESDAY,
                THURSDAY,
                FRIDAY,
                SATURDAY
        )
    }

    @Test
    fun `returns list starting with monday`() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.GERMANY)

        val days = daysProvider.getDaysOfWeekByLocale()

        assertThat(days).containsExactly(
                MONDAY,
                TUESDAY,
                WEDNESDAY,
                THURSDAY,
                FRIDAY,
                SATURDAY,
                SUNDAY
        )
    }
}
