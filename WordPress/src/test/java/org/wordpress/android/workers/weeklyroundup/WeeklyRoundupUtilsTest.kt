package org.wordpress.android.workers.weeklyroundup

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate

@RunWith(MockitoJUnitRunner::class)
class WeeklyRoundupUtilsTest {
    @Test
    fun `parsing standard date returns correct value`() {
        val date = WeeklyRoundupUtils.parseStandardDate("2021-08-10")
        assertThat(date).isNotNull
        assertThat(date).isEqualTo(LocalDate.of(2021, 8, 10))
    }

    @Test
    fun `parsing week period date returns correct value`() {
        val date = WeeklyRoundupUtils.parseWeekPeriodDate("2021W08W10")
        assertThat(date).isNotNull
        assertThat(date).isEqualTo(LocalDate.of(2021, 8, 10))
    }

    @Test
    fun `parsing invalid date returns null`() {
        val date = WeeklyRoundupUtils.parseWeekPeriodDate("invalid")
        assertThat(date).isNull()
    }
}
