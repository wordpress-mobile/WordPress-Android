package org.wordpress.android.ui.newstats.todaysstat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.StatsVisitsDataValue
import uniffi.wp_api.WpErrorCode

@ExperimentalCoroutinesApi
class StatsRepositoryTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        whenever(wpComApiClientProvider.getWpComApiClient(TEST_ACCESS_TOKEN))
            .thenReturn(wpComApiClient)

        repository = StatsRepository(
            wpComApiClientProvider = wpComApiClientProvider,
            appLogWrapper = appLogWrapper,
            ioDispatcher = testDispatcher()
        )
    }

    // region init tests
    @Test
    fun `init sets access token`() {
        repository.init(TEST_ACCESS_TOKEN)
        // If we get here without exception, the test passes
    }
    // endregion

    // region fetchTodayAggregates tests
    @Test
    fun `fetchTodayAggregates returns error when not initialized`() = runTest {
        // Given - repository not initialized

        // When
        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(TodayAggregatesResult.Error::class.java)
        assertThat((result as TodayAggregatesResult.Error).message).isEqualTo("Repository not initialized")
        verify(appLogWrapper).e(AppLog.T.STATS, "Cannot fetch stats: repository not initialized")
    }

    @Test
    fun `fetchTodayAggregates returns success when API returns valid data`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        val mockResponse = createMockDailyAggregatesResponse(
            views = TEST_VIEWS,
            visitors = TEST_VISITORS,
            likes = TEST_LIKES,
            comments = TEST_COMMENTS
        )
        setupApiClientToReturnSuccess(mockResponse)

        // When
        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(TodayAggregatesResult.Success::class.java)
        val success = result as TodayAggregatesResult.Success
        assertThat(success.aggregates.views).isEqualTo(TEST_VIEWS)
        assertThat(success.aggregates.visitors).isEqualTo(TEST_VISITORS)
        assertThat(success.aggregates.likes).isEqualTo(TEST_LIKES)
        assertThat(success.aggregates.comments).isEqualTo(TEST_COMMENTS)
    }

    @Test
    fun `fetchTodayAggregates returns error when API returns WpError`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        setupApiClientToReturnWpError(API_ERROR_MESSAGE)

        // When
        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(TodayAggregatesResult.Error::class.java)
        assertThat((result as TodayAggregatesResult.Error).message).isEqualTo(API_ERROR_MESSAGE)
    }

    @Test
    fun `fetchTodayAggregates returns error when API returns UnknownError`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        setupApiClientToReturnUnknownError()

        // When
        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(TodayAggregatesResult.Error::class.java)
        assertThat((result as TodayAggregatesResult.Error).message).isEqualTo("Unknown error")
    }

    @Test
    fun `fetchTodayAggregates returns error when API returns empty data`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        val mockResponse = createMockEmptyResponse()
        setupApiClientToReturnSuccess(mockResponse)

        // When
        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(TodayAggregatesResult.Error::class.java)
        assertThat((result as TodayAggregatesResult.Error).message).isEqualTo("No data available")
    }

    @Test
    fun `fetchTodayAggregates returns success with zero values when data has non-numeric values`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        val mockResponse = createMockDailyAggregatesResponseWithStringValues()
        setupApiClientToReturnSuccess(mockResponse)

        // When
        val result = repository.fetchTodayAggregates(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(TodayAggregatesResult.Success::class.java)
        val success = result as TodayAggregatesResult.Success
        assertThat(success.aggregates.views).isEqualTo(0L)
        assertThat(success.aggregates.visitors).isEqualTo(0L)
        assertThat(success.aggregates.likes).isEqualTo(0L)
        assertThat(success.aggregates.comments).isEqualTo(0L)
    }
    // endregion

    // region fetchHourlyViews tests
    @Test
    fun `fetchHourlyViews returns error when not initialized`() = runTest {
        // Given - repository not initialized

        // When
        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(HourlyViewsResult.Error::class.java)
        assertThat((result as HourlyViewsResult.Error).message).isEqualTo("Repository not initialized")
        verify(appLogWrapper).e(AppLog.T.STATS, "Cannot fetch stats: repository not initialized")
    }

    @Test
    fun `fetchHourlyViews returns success when API returns valid data`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        val mockResponse = createMockHourlyViewsResponse(
            listOf(
                HourlyDataPoint(TEST_PERIOD_1, TEST_HOURLY_VIEWS_1),
                HourlyDataPoint(TEST_PERIOD_2, TEST_HOURLY_VIEWS_2)
            )
        )
        setupApiClientToReturnSuccess(mockResponse)

        // When
        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(HourlyViewsResult.Success::class.java)
        val success = result as HourlyViewsResult.Success
        assertThat(success.dataPoints).hasSize(2)
        assertThat(success.dataPoints[0].period).isEqualTo(TEST_PERIOD_1)
        assertThat(success.dataPoints[0].views).isEqualTo(TEST_HOURLY_VIEWS_1)
        assertThat(success.dataPoints[1].period).isEqualTo(TEST_PERIOD_2)
        assertThat(success.dataPoints[1].views).isEqualTo(TEST_HOURLY_VIEWS_2)
    }

    @Test
    fun `fetchHourlyViews returns success with empty list when API returns empty data`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        val mockResponse = createMockEmptyResponse()
        setupApiClientToReturnSuccess(mockResponse)

        // When
        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(HourlyViewsResult.Success::class.java)
        assertThat((result as HourlyViewsResult.Success).dataPoints).isEmpty()
    }

    @Test
    fun `fetchHourlyViews returns error when API returns WpError`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        setupApiClientToReturnWpError(API_ERROR_MESSAGE)

        // When
        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(HourlyViewsResult.Error::class.java)
        assertThat((result as HourlyViewsResult.Error).message).isEqualTo(API_ERROR_MESSAGE)
    }

    @Test
    fun `fetchHourlyViews returns error when API returns UnknownError`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        setupApiClientToReturnUnknownError()

        // When
        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(HourlyViewsResult.Error::class.java)
        assertThat((result as HourlyViewsResult.Error).message).isEqualTo("Unknown error")
    }

    @Test
    fun `fetchHourlyViews with offsetDays parameter works correctly`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        val mockResponse = createMockHourlyViewsResponse(
            listOf(HourlyDataPoint(TEST_PERIOD_1, TEST_HOURLY_VIEWS_1))
        )
        setupApiClientToReturnSuccess(mockResponse)

        // When - fetch yesterday's data
        val result = repository.fetchHourlyViews(TEST_SITE_ID, offsetDays = 1)

        // Then
        assertThat(result).isInstanceOf(HourlyViewsResult.Success::class.java)
        assertThat((result as HourlyViewsResult.Success).dataPoints).hasSize(1)
    }

    @Test
    fun `fetchHourlyViews returns zero views when views value is not a number`() = runTest {
        // Given
        repository.init(TEST_ACCESS_TOKEN)

        val mockResponse = createMockHourlyViewsResponseWithNonNumericViews()
        setupApiClientToReturnSuccess(mockResponse)

        // When
        val result = repository.fetchHourlyViews(TEST_SITE_ID)

        // Then
        assertThat(result).isInstanceOf(HourlyViewsResult.Success::class.java)
        val success = result as HourlyViewsResult.Success
        assertThat(success.dataPoints).hasSize(1)
        assertThat(success.dataPoints[0].views).isEqualTo(0L)
    }
    // endregion

    // region Helper methods
    @Suppress("UNCHECKED_CAST")
    private suspend fun setupApiClientToReturnSuccess(response: MockStatsResponse) {
        val mockHeaderMap = mock<uniffi.wp_api.WpNetworkHeaderMap>()
        val responseObject = uniffi.wp_api.StatsVisitsRequestGetStatsVisitsResponse(
            data = response.toStatsVisitsResponse(),
            headerMap = mockHeaderMap
        )

        val successResponse = WpRequestResult.Success(responseObject)

        whenever(
            wpComApiClient.request<uniffi.wp_api.StatsVisitsResponse>(any())
        ).thenReturn(successResponse as WpRequestResult<uniffi.wp_api.StatsVisitsResponse>)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun setupApiClientToReturnWpError(errorMessage: String) {
        val errorResponse = WpRequestResult.WpError<uniffi.wp_api.StatsVisitsResponse>(
            errorCode = WpErrorCode.Forbidden(),
            errorMessage = errorMessage,
            statusCode = 403.toUShort(),
            response = ""
        )
        whenever(
            wpComApiClient.request<uniffi.wp_api.StatsVisitsResponse>(any())
        ).thenReturn(errorResponse)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun setupApiClientToReturnUnknownError() {
        val errorResponse = WpRequestResult.UnknownError<uniffi.wp_api.StatsVisitsResponse>(
            statusCode = 500.toUShort(),
            response = "Internal Server Error"
        )
        whenever(
            wpComApiClient.request<uniffi.wp_api.StatsVisitsResponse>(any())
        ).thenReturn(errorResponse)
    }

    private fun createMockDailyAggregatesResponse(
        views: Long,
        visitors: Long,
        likes: Long,
        comments: Long
    ): MockStatsResponse {
        // Response fields order: period, views, visitors, likes, reblogs, comments, posts
        val row = listOf(
            StatsVisitsDataValue.String("2024-01-16"),
            StatsVisitsDataValue.Number(views.toULong()),
            StatsVisitsDataValue.Number(visitors.toULong()),
            StatsVisitsDataValue.Number(likes.toULong()),
            StatsVisitsDataValue.Number(0.toULong()), // reblogs
            StatsVisitsDataValue.Number(comments.toULong()),
            StatsVisitsDataValue.Number(0.toULong())  // posts
        )
        return MockStatsResponse(listOf(row))
    }

    private fun createMockDailyAggregatesResponseWithStringValues(): MockStatsResponse {
        // Row with period but string values for metrics (should return 0 for all)
        val row = listOf(
            StatsVisitsDataValue.String("2024-01-16"),
            StatsVisitsDataValue.String("not a number"), // views
            StatsVisitsDataValue.String("not a number"), // visitors
            StatsVisitsDataValue.String("not a number"), // likes
            StatsVisitsDataValue.String("not a number"), // reblogs
            StatsVisitsDataValue.String("not a number"), // comments
            StatsVisitsDataValue.String("not a number")  // posts
        )
        return MockStatsResponse(listOf(row))
    }

    private fun createMockHourlyViewsResponse(dataPoints: List<HourlyDataPoint>): MockStatsResponse {
        val rows = dataPoints.map { dataPoint ->
            listOf(
                StatsVisitsDataValue.String(dataPoint.period),
                StatsVisitsDataValue.Number(dataPoint.views.toULong())
            )
        }
        return MockStatsResponse(rows)
    }

    private fun createMockHourlyViewsResponseWithNonNumericViews(): MockStatsResponse {
        val row = listOf(
            StatsVisitsDataValue.String(TEST_PERIOD_1),
            StatsVisitsDataValue.String("not a number") // views as string instead of number
        )
        return MockStatsResponse(listOf(row))
    }

    private fun createMockEmptyResponse(): MockStatsResponse {
        return MockStatsResponse(emptyList())
    }

    private data class HourlyDataPoint(val period: String, val views: Long)

    private data class MockStatsResponse(val data: List<List<StatsVisitsDataValue>>) {
        fun toStatsVisitsResponse(): uniffi.wp_api.StatsVisitsResponse {
            return uniffi.wp_api.StatsVisitsResponse(
                date = "2024-01-16",
                unit = "day",
                fields = listOf("period", "views", "visitors", "likes", "reblogs", "comments", "posts"),
                data = data
            )
        }
    }
    // endregion

    companion object {
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_SITE_ID = 123L
        private const val TEST_VIEWS = 500L
        private const val TEST_VISITORS = 100L
        private const val TEST_LIKES = 50L
        private const val TEST_COMMENTS = 25L
        private const val TEST_PERIOD_1 = "2024-01-16 14:00:00"
        private const val TEST_PERIOD_2 = "2024-01-16 15:00:00"
        private const val TEST_HOURLY_VIEWS_1 = 100L
        private const val TEST_HOURLY_VIEWS_2 = 150L
        private const val API_ERROR_MESSAGE = "API Error"
    }
}
