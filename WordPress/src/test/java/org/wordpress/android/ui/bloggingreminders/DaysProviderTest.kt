package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.FRIDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SATURDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.THURSDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.TUESDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.WEDNESDAY
import org.wordpress.android.ui.reader.utils.DateProvider
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class DaysProviderTest {
    @Mock lateinit var dateProvider: DateProvider
    private lateinit var daysProvider: DaysProvider

    @Before
    fun setUp() {
        daysProvider = DaysProvider(dateProvider)
        whenever(dateProvider.getShortWeekdays()).thenReturn(
                arrayOf(
                        "",
                        SUNDAY_TEXT,
                        MONDAY_TEXT,
                        TUESDAY_TEXT,
                        WEDNESDAY_TEXT,
                        THURSDAY_TEXT,
                        FRIDAY_TEXT,
                        SATURDAY_TEXT
                )
        )
    }

    @Test
    fun `returns list starting with sunday`() {
        whenever(dateProvider.getFirstDayOfTheWeek()).thenReturn(Calendar.SUNDAY)

        val days = daysProvider.getDays()

        assertThat(days).containsExactly(
                SUNDAY_TEXT to SUNDAY,
                MONDAY_TEXT to MONDAY,
                TUESDAY_TEXT to TUESDAY,
                WEDNESDAY_TEXT to WEDNESDAY,
                THURSDAY_TEXT to THURSDAY,
                FRIDAY_TEXT to FRIDAY,
                SATURDAY_TEXT to SATURDAY
        )
    }

    @Test
    fun `returns list starting with monday`() {
        whenever(dateProvider.getFirstDayOfTheWeek()).thenReturn(Calendar.MONDAY)

        val days = daysProvider.getDays()

        assertThat(days).containsExactly(
                MONDAY_TEXT to MONDAY,
                TUESDAY_TEXT to TUESDAY,
                WEDNESDAY_TEXT to WEDNESDAY,
                THURSDAY_TEXT to THURSDAY,
                FRIDAY_TEXT to FRIDAY,
                SATURDAY_TEXT to SATURDAY,
                SUNDAY_TEXT to SUNDAY
        )
    }

    @Test
    fun `returns list starting with saturday`() {
        whenever(dateProvider.getFirstDayOfTheWeek()).thenReturn(Calendar.SATURDAY)

        val days = daysProvider.getDays()

        assertThat(days).containsExactly(
                SATURDAY_TEXT to SATURDAY,
                SUNDAY_TEXT to SUNDAY,
                MONDAY_TEXT to MONDAY,
                TUESDAY_TEXT to TUESDAY,
                WEDNESDAY_TEXT to WEDNESDAY,
                THURSDAY_TEXT to THURSDAY,
                FRIDAY_TEXT to FRIDAY
        )
    }
    companion object {
        private const val SUNDAY_TEXT = "Sun"
        private const val MONDAY_TEXT = "Mon"
        private const val TUESDAY_TEXT = "Tue"
        private const val WEDNESDAY_TEXT = "Wed"
        private const val THURSDAY_TEXT = "Thu"
        private const val FRIDAY_TEXT = "Fri"
        private const val SATURDAY_TEXT = "Sat"
    }
}
