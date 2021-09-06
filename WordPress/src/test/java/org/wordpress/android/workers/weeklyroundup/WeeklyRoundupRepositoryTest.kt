package org.wordpress.android.workers.weeklyroundup

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.test

@RunWith(MockitoJUnitRunner::class)
class WeeklyRoundupRepositoryTest : BaseUnitTest() {
    private lateinit var weeklyRoundupRepository: WeeklyRoundupRepository

    private val visitsAndViewsStore: VisitsAndViewsStore = mock()

    private val site: SiteModel = mock()

    private val first: PeriodData = mock {
        on { period } doReturn "2021W08W09"
    }

    private val second: PeriodData = mock {
        on { period } doReturn "2021W08W02"
    }

    private val third: PeriodData = mock {
        on { period } doReturn "2021W07W26"
    }

    @Before
    fun setUp() {
        weeklyRoundupRepository = WeeklyRoundupRepository(visitsAndViewsStore)
    }

    @Test
    fun `fetch returns only last week model`() = test {
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any())).thenReturn(
                OnStatsFetched(VisitsAndViewsModel(period = "2021-08-04", dates = listOf(first, second, third)))
        )

        val result = weeklyRoundupRepository.fetchWeeklyRoundupData(site)

        assertThat(result).isNotNull
        assertThat(result?.period).isEqualTo(third.period)
    }

    @Test
    fun `fetch returns null when period is invalid`() = test {
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any())).thenReturn(
                OnStatsFetched(VisitsAndViewsModel(period = "invalid", dates = listOf(first, second, third)))
        )

        val result = weeklyRoundupRepository.fetchWeeklyRoundupData(site)

        assertThat(result).isNull()
    }

    @Test
    fun `fetch returns null on error`() = test {
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any())).thenReturn(
                OnStatsFetched(StatsError(GENERIC_ERROR, "Generic error!"))
        )

        val result = weeklyRoundupRepository.fetchWeeklyRoundupData(site)

        assertThat(result).isNull()
    }
}
