package org.wordpress.android.workers.weeklyroundup

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData

@RunWith(MockitoJUnitRunner::class)
class WeeklyRoundupDataTest {
    private val site: SiteModel = mock()
    private val periodData: PeriodData = mock()

    @Test
    fun `create maps properties correctly`() {
        whenever(periodData.period).thenReturn(TEST_PERIOD_STRING)
        whenever(periodData.views).thenReturn(10)
        whenever(periodData.likes).thenReturn(5)
        whenever(periodData.comments).thenReturn(1)

        val data = WeeklyRoundupData.create(site, periodData)

        with(data) {
            assertThat(period).isEqualTo(TEST_PERIOD_STRING)
            assertThat(views).isEqualTo(10)
            assertThat(likes).isEqualTo(5)
            assertThat(comments).isEqualTo(1)
        }
    }

    @Test
    fun `score is calculated correctly`() {
        val first = WeeklyRoundupData(site, TEST_PERIOD_STRING, 10, 0, 0)
        val second = WeeklyRoundupData(site, TEST_PERIOD_STRING, 9, 8, 8)

        assertThat(first.score).isEqualTo(10.0)
        assertThat(second.score).isEqualTo(17.0)
    }

    companion object {
        private const val TEST_PERIOD_STRING = "2021W08W10"
    }
}
