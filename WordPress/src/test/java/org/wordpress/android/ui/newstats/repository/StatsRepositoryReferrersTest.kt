package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.ReferrerChildDataItem
import org.wordpress.android.ui.newstats.datasource.ReferrerDataItem
import org.wordpress.android.ui.newstats.datasource.ReferrersDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource

@ExperimentalCoroutinesApi
class StatsRepositoryReferrersTest : BaseUnitTest() {
    @Mock
    private lateinit var statsDataSource: StatsDataSource

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        repository = StatsRepository(
            statsDataSource = statsDataSource,
            appLogWrapper = appLogWrapper,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `given successful response, when fetchMostViewed with REFERRERS, then success result is returned`() = test {
        whenever(statsDataSource.fetchReferrers(any(), any(), any()))
            .thenReturn(ReferrersDataResult.Success(createReferrersData()))

        val result = repository.fetchMostViewed(
            TEST_SITE_ID,
            StatsPeriod.Last7Days,
            MostViewedDataSource.REFERRERS
        )

        assertThat(result).isInstanceOf(MostViewedResult.Success::class.java)
        val success = result as MostViewedResult.Success
        assertThat(success.items).hasSize(2)
        assertThat(success.items[0].title).isEqualTo(TEST_REFERRER_NAME_1)
        assertThat(success.items[0].views).isEqualTo(TEST_REFERRER_VIEWS_1)
        assertThat(success.items[1].title).isEqualTo(TEST_REFERRER_NAME_2)
        assertThat(success.items[1].views).isEqualTo(TEST_REFERRER_VIEWS_2)
    }

    @Test
    fun `given referrer group with children, when fetchMostViewed with REFERRERS, then children propagate`() = test {
        val referrersData = listOf(
            ReferrerDataItem(
                name = "Search Engines",
                url = null,
                views = 23,
                children = listOf(
                    ReferrerChildDataItem(name = "Google Search", url = "http://google.com/", views = 23)
                )
            )
        )
        whenever(statsDataSource.fetchReferrers(any(), any(), any()))
            .thenReturn(ReferrersDataResult.Success(referrersData))

        val result = repository.fetchMostViewed(
            TEST_SITE_ID,
            StatsPeriod.Last7Days,
            MostViewedDataSource.REFERRERS
        )

        assertThat(result).isInstanceOf(MostViewedResult.Success::class.java)
        val success = result as MostViewedResult.Success
        assertThat(success.items[0].children).hasSize(1)
        assertThat(success.items[0].children[0].name).isEqualTo("Google Search")
        assertThat(success.items[0].children[0].url).isEqualTo("http://google.com/")
        assertThat(success.items[0].children[0].views).isEqualTo(23)
    }

    @Test
    fun `given referrer with a url, when fetchMostViewed with REFERRERS, then the url propagates`() = test {
        whenever(statsDataSource.fetchReferrers(any(), any(), any()))
            .thenReturn(ReferrersDataResult.Success(createReferrersData()))

        val result = repository.fetchMostViewed(
            TEST_SITE_ID,
            StatsPeriod.Last7Days,
            MostViewedDataSource.REFERRERS
        )

        val success = result as MostViewedResult.Success
        assertThat(success.items[0].url).isEqualTo(TEST_REFERRER_URL_1)
        assertThat(success.items[1].url).isEqualTo(TEST_REFERRER_URL_2)
    }

    @Test
    fun `when fetchMostViewed with REFERRERS, then card-sized max is requested`() = test {
        whenever(statsDataSource.fetchReferrers(any(), any(), any()))
            .thenReturn(ReferrersDataResult.Success(createReferrersData()))

        repository.fetchMostViewed(TEST_SITE_ID, StatsPeriod.Last7Days, MostViewedDataSource.REFERRERS)

        // Card request is bounded (current + previous period).
        verify(statsDataSource, times(2)).fetchReferrers(any(), any(), eq(REFERRERS_CARD_MAX))
    }

    @Test
    fun `when fetchReferrersDetail, then unlimited max is requested`() = test {
        whenever(statsDataSource.fetchReferrers(any(), any(), any()))
            .thenReturn(ReferrersDataResult.Success(createReferrersData()))

        repository.fetchReferrersDetail(TEST_SITE_ID, StatsPeriod.Last7Days)

        // Detail request asks for all referrers (max = 0), for current + previous period.
        verify(statsDataSource, times(2)).fetchReferrers(any(), any(), eq(REFERRERS_DETAIL_MAX))
    }

    @Test
    fun `given error response, when fetchMostViewed with REFERRERS, then error result is returned`() = test {
        whenever(statsDataSource.fetchReferrers(any(), any(), any()))
            .thenReturn(ReferrersDataResult.Error(TEST_ERROR_TYPE))

        val result = repository.fetchMostViewed(
            TEST_SITE_ID,
            StatsPeriod.Last7Days,
            MostViewedDataSource.REFERRERS
        )

        assertThat(result).isInstanceOf(MostViewedResult.Error::class.java)
        assertThat((result as MostViewedResult.Error).message).isEqualTo(TEST_ERROR_TYPE.name)
    }

    private fun createReferrersData() = listOf(
        ReferrerDataItem(name = TEST_REFERRER_NAME_1, url = TEST_REFERRER_URL_1, views = TEST_REFERRER_VIEWS_1),
        ReferrerDataItem(name = TEST_REFERRER_NAME_2, url = TEST_REFERRER_URL_2, views = TEST_REFERRER_VIEWS_2)
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private val TEST_ERROR_TYPE = StatsErrorType.NETWORK_ERROR

        // Mirrors StatsRepository's private referrer max values (card vs. detail).
        private const val REFERRERS_CARD_MAX = 10
        private const val REFERRERS_DETAIL_MAX = 0

        private const val TEST_REFERRER_NAME_1 = "google.com"
        private const val TEST_REFERRER_NAME_2 = "twitter.com"
        private const val TEST_REFERRER_URL_1 = "https://google.com/"
        private const val TEST_REFERRER_URL_2 = "https://twitter.com/"
        private const val TEST_REFERRER_VIEWS_1 = 200L
        private const val TEST_REFERRER_VIEWS_2 = 150L
    }
}
