package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.wordpress.android.ui.newstats.datasource.CommentsDataPoint
import org.wordpress.android.ui.newstats.datasource.LikesDataPoint
import org.wordpress.android.ui.newstats.datasource.PostsDataPoint
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.datasource.StatsVisitsData
import org.wordpress.android.ui.newstats.datasource.StatsVisitsDataResult
import org.wordpress.android.ui.newstats.datasource.VisitorsDataPoint
import org.wordpress.android.ui.newstats.datasource.VisitsDataPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper

@ExperimentalCoroutinesApi
class StatsRepositoryTest : BaseUnitTest() {
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

    // region init
    @Test
    fun `when init is called, then data source is initialized with access token`() {
        repository.init(TEST_ACCESS_TOKEN)

        verify(statsDataSource).init(eq(TEST_ACCESS_TOKEN))
    }
    // endregion

    // region fetchTodayAggregates
    @Test
    fun `given successful response, when fetchTodayAggregates is called, then success result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Success(createStatsVisitsData()))

        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        assertThat(result).isInstanceOf(TodayAggregatesResult.Success::class.java)
        val success = result as TodayAggregatesResult.Success
        assertThat(success.aggregates.views).isEqualTo(TEST_VIEWS)
        assertThat(success.aggregates.visitors).isEqualTo(TEST_VISITORS)
        assertThat(success.aggregates.likes).isEqualTo(TEST_LIKES)
        assertThat(success.aggregates.comments).isEqualTo(TEST_COMMENTS)
    }

    @Test
    fun `given successful response, when fetchTodayAggregates is called, then data source is called with DAY unit`() =
        test {
            whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
                .thenReturn(StatsVisitsDataResult.Success(createStatsVisitsData()))

            repository.fetchTodayAggregates(TEST_SITE_ID)

            verify(statsDataSource).fetchStatsVisits(
                siteId = eq(TEST_SITE_ID),
                unit = eq(StatsUnit.DAY),
                quantity = eq(1),
                endDate = any()
            )
        }

    @Test
    fun `given empty data, when fetchTodayAggregates is called, then zeros are returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Success(createEmptyStatsVisitsData()))

        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        assertThat(result).isInstanceOf(TodayAggregatesResult.Success::class.java)
        val success = result as TodayAggregatesResult.Success
        assertThat(success.aggregates.views).isEqualTo(0L)
        assertThat(success.aggregates.visitors).isEqualTo(0L)
        assertThat(success.aggregates.likes).isEqualTo(0L)
        assertThat(success.aggregates.comments).isEqualTo(0L)
    }

    @Test
    fun `given error response, when fetchTodayAggregates is called, then error result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Error(ERROR_MESSAGE))

        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        assertThat(result).isInstanceOf(TodayAggregatesResult.Error::class.java)
        assertThat((result as TodayAggregatesResult.Error).message).isEqualTo(ERROR_MESSAGE)
    }
    // endregion

    // region fetchHourlyViews
    @Test
    fun `given successful response, when fetchHourlyViews is called, then success result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Success(createHourlyStatsVisitsData()))

        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        assertThat(result).isInstanceOf(HourlyViewsResult.Success::class.java)
        val success = result as HourlyViewsResult.Success
        assertThat(success.dataPoints).hasSize(2)
        assertThat(success.dataPoints[0].period).isEqualTo(TEST_PERIOD_1)
        assertThat(success.dataPoints[0].views).isEqualTo(TEST_VIEWS_1)
        assertThat(success.dataPoints[1].period).isEqualTo(TEST_PERIOD_2)
        assertThat(success.dataPoints[1].views).isEqualTo(TEST_VIEWS_2)
    }

    @Test
    fun `given successful response, when fetchHourlyViews is called, then data source is called with HOUR unit`() =
        test {
            whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
                .thenReturn(StatsVisitsDataResult.Success(createHourlyStatsVisitsData()))

            repository.fetchHourlyViews(TEST_SITE_ID)

            verify(statsDataSource).fetchStatsVisits(
                siteId = eq(TEST_SITE_ID),
                unit = eq(StatsUnit.HOUR),
                quantity = eq(24),
                endDate = any()
            )
        }

    @Test
    fun `given error response, when fetchHourlyViews is called, then error result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Error(ERROR_MESSAGE))

        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        assertThat(result).isInstanceOf(HourlyViewsResult.Error::class.java)
        assertThat((result as HourlyViewsResult.Error).message).isEqualTo(ERROR_MESSAGE)
    }

    @Test
    fun `given offset days, when fetchHourlyViews is called, then data source is called`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Success(createHourlyStatsVisitsData()))

        repository.fetchHourlyViews(TEST_SITE_ID, offsetDays = 1)

        verify(statsDataSource).fetchStatsVisits(
            siteId = eq(TEST_SITE_ID),
            unit = eq(StatsUnit.HOUR),
            quantity = eq(24),
            endDate = any()
        )
    }
    // endregion

    // region fetchWeeklyStats
    @Test
    fun `given successful response, when fetchWeeklyStats is called, then success result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

        val result = repository.fetchWeeklyStats(TEST_SITE_ID)

        assertThat(result).isInstanceOf(WeeklyStatsResult.Success::class.java)
        val success = result as WeeklyStatsResult.Success
        assertThat(success.aggregates.views).isEqualTo(TEST_VIEWS_1 + TEST_VIEWS_2)
        assertThat(success.aggregates.visitors).isEqualTo(TEST_VISITORS_1 + TEST_VISITORS_2)
        assertThat(success.aggregates.likes).isEqualTo(TEST_LIKES_1 + TEST_LIKES_2)
        assertThat(success.aggregates.comments).isEqualTo(TEST_COMMENTS_1 + TEST_COMMENTS_2)
        assertThat(success.aggregates.posts).isEqualTo(TEST_POSTS_1 + TEST_POSTS_2)
    }

    @Test
    fun `given successful response, when fetchWeeklyStats is called, then data source is called with DAY unit`() =
        test {
            whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
                .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

            repository.fetchWeeklyStats(TEST_SITE_ID)

            verify(statsDataSource).fetchStatsVisits(
                siteId = eq(TEST_SITE_ID),
                unit = eq(StatsUnit.DAY),
                quantity = eq(7),
                endDate = any()
            )
        }

    @Test
    fun `given error response, when fetchWeeklyStats is called, then error result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Error(ERROR_MESSAGE))

        val result = repository.fetchWeeklyStats(TEST_SITE_ID)

        assertThat(result).isInstanceOf(WeeklyStatsResult.Error::class.java)
        assertThat((result as WeeklyStatsResult.Error).message).isEqualTo(ERROR_MESSAGE)
    }

    @Test
    fun `given weeks ago parameter, when fetchWeeklyStats is called, then data source is called`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

        repository.fetchWeeklyStats(TEST_SITE_ID, weeksAgo = 1)

        verify(statsDataSource).fetchStatsVisits(
            siteId = eq(TEST_SITE_ID),
            unit = eq(StatsUnit.DAY),
            quantity = eq(7),
            endDate = any()
        )
    }
    // endregion

    // region fetchDailyViewsForWeek
    @Test
    fun `given successful response, when fetchDailyViewsForWeek is called, then success result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

        val result = repository.fetchDailyViewsForWeek(TEST_SITE_ID)

        assertThat(result).isInstanceOf(DailyViewsResult.Success::class.java)
        val success = result as DailyViewsResult.Success
        assertThat(success.dataPoints).hasSize(2)
        assertThat(success.dataPoints[0].period).isEqualTo(TEST_PERIOD_1)
        assertThat(success.dataPoints[0].views).isEqualTo(TEST_VIEWS_1)
        assertThat(success.dataPoints[1].period).isEqualTo(TEST_PERIOD_2)
        assertThat(success.dataPoints[1].views).isEqualTo(TEST_VIEWS_2)
    }

    @Test
    fun `given successful response, when fetchDailyViewsForWeek is called, then data source is called with DAY unit`() =
        test {
            whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
                .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

            repository.fetchDailyViewsForWeek(TEST_SITE_ID)

            verify(statsDataSource).fetchStatsVisits(
                siteId = eq(TEST_SITE_ID),
                unit = eq(StatsUnit.DAY),
                quantity = eq(7),
                endDate = any()
            )
        }

    @Test
    fun `given error response, when fetchDailyViewsForWeek is called, then error result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Error(ERROR_MESSAGE))

        val result = repository.fetchDailyViewsForWeek(TEST_SITE_ID)

        assertThat(result).isInstanceOf(DailyViewsResult.Error::class.java)
        assertThat((result as DailyViewsResult.Error).message).isEqualTo(ERROR_MESSAGE)
    }
    // endregion

    // region fetchWeeklyStatsWithDailyData
    @Test
    fun `given successful response, when fetchWeeklyStatsWithDailyData is called, then success result is returned`() =
        test {
            whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
                .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

            val result = repository.fetchWeeklyStatsWithDailyData(TEST_SITE_ID)

            assertThat(result).isInstanceOf(WeeklyStatsWithDailyDataResult.Success::class.java)
            val success = result as WeeklyStatsWithDailyDataResult.Success

            // Verify aggregates
            assertThat(success.aggregates.views).isEqualTo(TEST_VIEWS_1 + TEST_VIEWS_2)
            assertThat(success.aggregates.visitors).isEqualTo(TEST_VISITORS_1 + TEST_VISITORS_2)
            assertThat(success.aggregates.likes).isEqualTo(TEST_LIKES_1 + TEST_LIKES_2)
            assertThat(success.aggregates.comments).isEqualTo(TEST_COMMENTS_1 + TEST_COMMENTS_2)
            assertThat(success.aggregates.posts).isEqualTo(TEST_POSTS_1 + TEST_POSTS_2)

            // Verify daily data points
            assertThat(success.dailyDataPoints).hasSize(2)
            assertThat(success.dailyDataPoints[0].period).isEqualTo(TEST_PERIOD_1)
            assertThat(success.dailyDataPoints[0].views).isEqualTo(TEST_VIEWS_1)
        }

    @Test
    fun `given successful response, when fetchWeeklyStatsWithDailyData is called, data source is called correctly`() =
        test {
            whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
                .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

            repository.fetchWeeklyStatsWithDailyData(TEST_SITE_ID)

            verify(statsDataSource).fetchStatsVisits(
                siteId = eq(TEST_SITE_ID),
                unit = eq(StatsUnit.DAY),
                quantity = eq(7),
                endDate = any()
            )
        }

    @Test
    fun `given error response, when fetchWeeklyStatsWithDailyData is called, then error result is returned`() = test {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
            .thenReturn(StatsVisitsDataResult.Error(ERROR_MESSAGE))

        val result = repository.fetchWeeklyStatsWithDailyData(TEST_SITE_ID)

        assertThat(result).isInstanceOf(WeeklyStatsWithDailyDataResult.Error::class.java)
        assertThat((result as WeeklyStatsWithDailyDataResult.Error).message).isEqualTo(ERROR_MESSAGE)
    }

    @Test
    fun `given weeks ago parameter, when fetchWeeklyStatsWithDailyData is called, then data source is called`() =
        test {
            whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any()))
                .thenReturn(StatsVisitsDataResult.Success(createWeeklyStatsVisitsData()))

            repository.fetchWeeklyStatsWithDailyData(TEST_SITE_ID, weeksAgo = 2)

            verify(statsDataSource).fetchStatsVisits(
                siteId = eq(TEST_SITE_ID),
                unit = eq(StatsUnit.DAY),
                quantity = eq(7),
                endDate = any()
            )
        }
    // endregion

    // region Helper functions
    private fun createStatsVisitsData() = StatsVisitsData(
        visits = listOf(VisitsDataPoint(TEST_PERIOD_1, TEST_VIEWS)),
        visitors = listOf(VisitorsDataPoint(TEST_PERIOD_1, TEST_VISITORS)),
        likes = listOf(LikesDataPoint(TEST_PERIOD_1, TEST_LIKES)),
        comments = listOf(CommentsDataPoint(TEST_PERIOD_1, TEST_COMMENTS)),
        posts = listOf(PostsDataPoint(TEST_PERIOD_1, TEST_POSTS))
    )

    private fun createEmptyStatsVisitsData() = StatsVisitsData(
        visits = emptyList(),
        visitors = emptyList(),
        likes = emptyList(),
        comments = emptyList(),
        posts = emptyList()
    )

    private fun createHourlyStatsVisitsData() = StatsVisitsData(
        visits = listOf(
            VisitsDataPoint(TEST_PERIOD_1, TEST_VIEWS_1),
            VisitsDataPoint(TEST_PERIOD_2, TEST_VIEWS_2)
        ),
        visitors = listOf(
            VisitorsDataPoint(TEST_PERIOD_1, TEST_VISITORS_1),
            VisitorsDataPoint(TEST_PERIOD_2, TEST_VISITORS_2)
        ),
        likes = emptyList(),
        comments = emptyList(),
        posts = emptyList()
    )

    private fun createWeeklyStatsVisitsData() = StatsVisitsData(
        visits = listOf(
            VisitsDataPoint(TEST_PERIOD_1, TEST_VIEWS_1),
            VisitsDataPoint(TEST_PERIOD_2, TEST_VIEWS_2)
        ),
        visitors = listOf(
            VisitorsDataPoint(TEST_PERIOD_1, TEST_VISITORS_1),
            VisitorsDataPoint(TEST_PERIOD_2, TEST_VISITORS_2)
        ),
        likes = listOf(
            LikesDataPoint(TEST_PERIOD_1, TEST_LIKES_1),
            LikesDataPoint(TEST_PERIOD_2, TEST_LIKES_2)
        ),
        comments = listOf(
            CommentsDataPoint(TEST_PERIOD_1, TEST_COMMENTS_1),
            CommentsDataPoint(TEST_PERIOD_2, TEST_COMMENTS_2)
        ),
        posts = listOf(
            PostsDataPoint(TEST_PERIOD_1, TEST_POSTS_1),
            PostsDataPoint(TEST_PERIOD_2, TEST_POSTS_2)
        )
    )
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val ERROR_MESSAGE = "Test error message"

        private const val TEST_PERIOD_1 = "2024-01-15"
        private const val TEST_PERIOD_2 = "2024-01-16"

        private const val TEST_VIEWS = 500L
        private const val TEST_VISITORS = 100L
        private const val TEST_LIKES = 50L
        private const val TEST_COMMENTS = 25L
        private const val TEST_POSTS = 5L

        private const val TEST_VIEWS_1 = 100L
        private const val TEST_VIEWS_2 = 150L
        private const val TEST_VISITORS_1 = 50L
        private const val TEST_VISITORS_2 = 75L
        private const val TEST_LIKES_1 = 10L
        private const val TEST_LIKES_2 = 15L
        private const val TEST_COMMENTS_1 = 5L
        private const val TEST_COMMENTS_2 = 8L
        private const val TEST_POSTS_1 = 2L
        private const val TEST_POSTS_2 = 3L
    }
}
