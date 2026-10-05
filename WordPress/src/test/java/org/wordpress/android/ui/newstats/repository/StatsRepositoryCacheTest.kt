package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.ClickDataItem
import org.wordpress.android.ui.newstats.datasource.ClicksDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.CommentsDataPoint
import org.wordpress.android.ui.newstats.datasource.LikesDataPoint
import org.wordpress.android.ui.newstats.datasource.PostsDataPoint
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.ui.newstats.datasource.StatsVisitsData
import org.wordpress.android.ui.newstats.datasource.StatsVisitsDataResult
import org.wordpress.android.ui.newstats.datasource.VisitorsDataPoint
import org.wordpress.android.ui.newstats.datasource.VisitsDataPoint
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.StatsInsightsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.datasource.StatsSummaryDataResult
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import org.wordpress.android.ui.newstats.datasource.StatsTagsDataResult
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Covers the in-memory cache [StatsRepository] serves period results from (CMM-2473). One repository
 * call to a comparison endpoint issues two data source calls (the period and the one before it), so
 * the counts asserted here are per window, not per card.
 */
@ExperimentalCoroutinesApi
class StatsRepositoryCacheTest : BaseUnitTest() {
    @Mock
    private lateinit var statsDataSource: StatsDataSource

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var cache: StatsResultCache
    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        cache = StatsResultCache()
        repository = repositoryAt(TODAY)
    }

    private fun repositoryAt(today: LocalDate) = StatsRepository(
        statsDataSource = statsDataSource,
        statsResultCache = cache,
        appLogWrapper = appLogWrapper,
        clock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()),
        ioDispatcher = testDispatcher()
    )

    @Test
    fun `given a period was fetched, when it is fetched again, then no new request is made`() = test {
        stubClicks()

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)
        val second = repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        verify(statsDataSource, times(WINDOWS_PER_FETCH)).fetchClicks(any(), any(), any())
        assertThat(second).isInstanceOf(ClicksResult.Success::class.java)
    }

    @Test
    fun `given a cached period, when fetched with forceRefresh, then a new request is made`() = test {
        stubClicks()
        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days, forceRefresh = true)

        verify(statsDataSource, times(2 * WINDOWS_PER_FETCH)).fetchClicks(any(), any(), any())
    }

    @Test
    fun `given the request failed, when it is retried, then the error is not served from the cache`() = test {
        whenever(statsDataSource.fetchClicks(any(), any(), any()))
            .thenReturn(ClicksDataResult.Error(StatsErrorType.API_ERROR))

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)
        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        verify(statsDataSource, times(2 * WINDOWS_PER_FETCH)).fetchClicks(any(), any(), any())
        assertThat(cache.isCached(clicksKey(TODAY))).isFalse()
    }

    @Test
    fun `given one period was fetched, when another is fetched, then it is requested`() = test {
        stubClicks()

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)
        repository.fetchClicks(SITE_ID, StatsPeriod.Last30Days)

        verify(statsDataSource, times(2 * WINDOWS_PER_FETCH)).fetchClicks(any(), any(), any())
    }

    @Test
    fun `given another site, when the same period is fetched, then it is requested`() = test {
        stubClicks()

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)
        repository.fetchClicks(OTHER_SITE_ID, StatsPeriod.Last7Days)

        verify(statsDataSource, times(2 * WINDOWS_PER_FETCH)).fetchClicks(any(), any(), any())
    }

    @Test
    fun `given a preset cached yesterday, when fetched today, then it is requested again`() = test {
        stubClicks()
        repositoryAt(TODAY.minusDays(1)).fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        // "Last 7 days" means a different week after a day rollover, so yesterday's entry must not
        // answer today's request.
        verify(statsDataSource, times(2 * WINDOWS_PER_FETCH)).fetchClicks(any(), any(), any())
    }

    @Test
    fun `given one endpoint was cached, then another endpoint for the same period is not`() = test {
        stubClicks()

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        assertThat(repository.isCached(StatsCacheBucket.CLICKS, SITE_ID, StatsPeriod.Last7Days)).isTrue()
        assertThat(repository.isCached(StatsCacheBucket.SEARCH_TERMS, SITE_ID, StatsPeriod.Last7Days)).isFalse()
    }

    @Test
    fun `given a cached period, when it has not been marked stale, then it needs no revalidation`() = test {
        stubClicks()

        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        assertThat(repository.needsRevalidation(StatsCacheBucket.CLICKS, SITE_ID, StatsPeriod.Last7Days))
            .isFalse()
    }

    @Test
    fun `given the cache was marked stale, when a cached period is read, then it needs revalidation`() = test {
        stubClicks()
        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        cache.markAllStale()

        assertThat(repository.isCached(StatsCacheBucket.CLICKS, SITE_ID, StatsPeriod.Last7Days)).isTrue()
        assertThat(repository.needsRevalidation(StatsCacheBucket.CLICKS, SITE_ID, StatsPeriod.Last7Days))
            .isTrue()
        // The revalidating fetch clears the flag, so the period is only refreshed once per visit.
        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days, forceRefresh = true)
        assertThat(repository.needsRevalidation(StatsCacheBucket.CLICKS, SITE_ID, StatsPeriod.Last7Days))
            .isFalse()
    }

    @Test
    fun `given a cached period, when the cache is cleared, then it is requested again`() = test {
        stubClicks()
        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        cache.clear()
        repository.fetchClicks(SITE_ID, StatsPeriod.Last7Days)

        verify(statsDataSource, times(2 * WINDOWS_PER_FETCH)).fetchClicks(any(), any(), any())
    }

    @Test
    fun `given the chart was fetched, when the same period is charted again, then no new request is made`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(StatsVisitsDataResult.Success(visitsData()))

        repository.fetchStatsForPeriod(SITE_ID, StatsPeriod.ThisMonth)
        val second = repository.fetchStatsForPeriod(SITE_ID, StatsPeriod.ThisMonth)

        verify(statsDataSource, times(WINDOWS_PER_FETCH))
            .fetchStatsVisits(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        assertThat(second).isInstanceOf(PeriodStatsResult.Success::class.java)
    }

    // region endpoints that must stay uncached
    // The insights tab caches these in its own use cases, which InsightsViewModel clears on every
    // screen entry to force a fresh fetch. Caching them here as well would silently defeat that.
    @Test
    fun `given insights were fetched, when fetched again, then a new request is made`() = test {
        whenever(statsDataSource.fetchStatsInsights(any()))
            .thenReturn(StatsInsightsDataResult.Success(StatsInsightsData(0, 0.0, 0, 0.0, emptyList())))

        repository.fetchInsights(SITE_ID)
        repository.fetchInsights(SITE_ID)

        verify(statsDataSource, times(2)).fetchStatsInsights(SITE_ID)
    }

    @Test
    fun `given the summary was fetched, when fetched again, then a new request is made`() = test {
        whenever(statsDataSource.fetchStatsSummary(any()))
            .thenReturn(StatsSummaryDataResult.Success(StatsSummaryData(0, 0, 0, 0, "", 0)))

        repository.fetchStatsSummary(SITE_ID)
        repository.fetchStatsSummary(SITE_ID)

        verify(statsDataSource, times(2)).fetchStatsSummary(SITE_ID)
    }

    @Test
    fun `given tags were fetched, when fetched again, then a new request is made`() = test {
        whenever(statsDataSource.fetchStatsTags(any(), any()))
            .thenReturn(StatsTagsDataResult.Success(StatsTagsData(emptyList())))

        repository.fetchTags(SITE_ID)
        repository.fetchTags(SITE_ID)

        verify(statsDataSource, times(2)).fetchStatsTags(any(), any())
    }
    // endregion

    private suspend fun stubClicks() {
        whenever(statsDataSource.fetchClicks(any(), any(), any()))
            .thenReturn(ClicksDataResult.Success(listOf(clickItem())))
    }

    private fun visitsData() = StatsVisitsData(
        visits = listOf(VisitsDataPoint(PERIOD_LABEL, 10)),
        visitors = listOf(VisitorsDataPoint(PERIOD_LABEL, 5)),
        likes = listOf(LikesDataPoint(PERIOD_LABEL, 1)),
        comments = listOf(CommentsDataPoint(PERIOD_LABEL, 2)),
        posts = listOf(PostsDataPoint(PERIOD_LABEL, 3))
    )

    private fun clickItem() = ClickDataItem(
        name = "wordpress.org",
        url = null,
        clicks = 10
    )

    private fun clicksKey(today: LocalDate) =
        StatsCacheKey(StatsCacheBucket.CLICKS, SITE_ID, today, StatsPeriod.Last7Days)

    companion object {
        private const val SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L

        // Every comparison endpoint fetches the selected window and the one before it.
        private const val WINDOWS_PER_FETCH = 2
        private const val PERIOD_LABEL = "2026-10-01"
        private val TODAY = LocalDate.of(2026, 10, 5)
    }
}
