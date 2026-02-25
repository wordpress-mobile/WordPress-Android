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
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.ui.newstats.datasource.VideoPlayDataItem
import org.wordpress.android.ui.newstats.datasource.VideoPlaysDataResult

@ExperimentalCoroutinesApi
class StatsRepositoryVideoPlaysTest : BaseUnitTest() {
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
    fun `given successful response, when fetchVideoPlays, then success result is returned`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(
                    createVideoPlayItems()
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Success::class.java)
            val success = result as VideoPlaysResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].title)
                .isEqualTo(TEST_VIDEO_TITLE_1)
            assertThat(success.items[0].views)
                .isEqualTo(TEST_VIDEO_VIEWS_1)
            assertThat(success.items[1].title)
                .isEqualTo(TEST_VIDEO_TITLE_2)
            assertThat(success.items[1].views)
                .isEqualTo(TEST_VIDEO_VIEWS_2)
        }

    @Test
    fun `given successful response, when fetchVideoPlays, then totalViews is sum of item views`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(
                    createVideoPlayItems()
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Success::class.java)
            val success = result as VideoPlaysResult.Success
            assertThat(success.totalViews)
                .isEqualTo(TEST_VIDEO_VIEWS_1 + TEST_VIDEO_VIEWS_2)
        }

    @Test
    fun `given current and previous data, when fetchVideoPlays, then change is calculated correctly`() =
        test {
            val currentItems = listOf(
                VideoPlayDataItem("Video 1", 150),
                VideoPlayDataItem("Video 2", 100)
            )
            val previousItems = listOf(
                VideoPlayDataItem("Video 1", 100),
                VideoPlayDataItem("Video 2", 100)
            )

            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(currentItems)
            ).thenReturn(
                VideoPlaysDataResult.Success(previousItems)
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Success::class.java)
            val success = result as VideoPlaysResult.Success
            // Current total: 250, Previous total: 200, Change: 50
            assertThat(success.totalViews).isEqualTo(250)
            assertThat(success.totalViewsChange).isEqualTo(50)
            assertThat(success.totalViewsChangePercent)
                .isEqualTo(25.0)
        }

    @Test
    fun `given item in both periods, when fetchVideoPlays, then previousViews is set correctly`() =
        test {
            val currentItems = listOf(
                VideoPlayDataItem("Video 1", 150)
            )
            val previousItems = listOf(
                VideoPlayDataItem("Video 1", 100)
            )

            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(currentItems)
            ).thenReturn(
                VideoPlaysDataResult.Success(previousItems)
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Success::class.java)
            val success = result as VideoPlaysResult.Success
            assertThat(success.items[0].previousViews)
                .isEqualTo(100)
            assertThat(success.items[0].viewsChange)
                .isEqualTo(50)
            assertThat(success.items[0].viewsChangePercent)
                .isEqualTo(50.0)
        }

    @Test
    fun `given new item not in previous period, when fetchVideoPlays, then previousViews is zero`() =
        test {
            val currentItems = listOf(
                VideoPlayDataItem("New Video", 100)
            )
            val previousItems = emptyList<VideoPlayDataItem>()

            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(currentItems)
            ).thenReturn(
                VideoPlaysDataResult.Success(previousItems)
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Success::class.java)
            val success = result as VideoPlaysResult.Success
            assertThat(success.items[0].previousViews)
                .isEqualTo(0)
            assertThat(success.items[0].viewsChange)
                .isEqualTo(100)
            assertThat(success.items[0].viewsChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given previous fetch fails, when fetchVideoPlays, then previousViews defaults to zero`() =
        test {
            val currentItems = listOf(
                VideoPlayDataItem("Video 1", 100)
            )

            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(currentItems)
            ).thenReturn(
                VideoPlaysDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Success::class.java)
            val success = result as VideoPlaysResult.Success
            assertThat(success.items[0].previousViews)
                .isEqualTo(0)
            assertThat(success.totalViewsChange).isEqualTo(100)
            assertThat(success.totalViewsChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given error response, when fetchVideoPlays, then error result is returned`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Error::class.java)
            assertThat(
                (result as VideoPlaysResult.Error).messageResId
            ).isEqualTo(R.string.stats_error_network)
        }

    @Test
    fun `given auth error, when fetchVideoPlays, then isAuthError is true`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Error(
                    StatsErrorType.AUTH_ERROR
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Error::class.java)
            val error = result as VideoPlaysResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_auth)
            assertThat(error.isAuthError).isTrue()
        }

    @Test
    fun `given non-auth error, when fetchVideoPlays, then isAuthError is false`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Error::class.java)
            assertThat(
                (result as VideoPlaysResult.Error).isAuthError
            ).isFalse()
        }

    @Test
    fun `given parsing error, when fetchVideoPlays, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Error(
                    StatsErrorType.PARSING_ERROR
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Error::class.java)
            val error = result as VideoPlaysResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_parsing)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given api error, when fetchVideoPlays, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Error(
                    StatsErrorType.API_ERROR
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Error::class.java)
            val error = result as VideoPlaysResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_api)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given unknown error, when fetchVideoPlays, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Error(
                    StatsErrorType.UNKNOWN
                )
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Error::class.java)
            val error = result as VideoPlaysResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_unknown)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `when fetchVideoPlays is called, then data source is called twice for comparison`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(
                    createVideoPlayItems()
                )
            )

            repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            verify(
                statsDataSource, times(2)
            ).fetchVideoPlays(
                siteId = eq(TEST_SITE_ID),
                dateRange = any(),
                max = eq(0)
            )
        }

    @Test
    fun `given empty items list, when fetchVideoPlays, then success with empty list is returned`() =
        test {
            whenever(
                statsDataSource.fetchVideoPlays(any(), any(), any())
            ).thenReturn(
                VideoPlaysDataResult.Success(emptyList())
            )

            val result = repository.fetchVideoPlays(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(VideoPlaysResult.Success::class.java)
            val success = result as VideoPlaysResult.Success
            assertThat(success.items).isEmpty()
            assertThat(success.totalViews).isEqualTo(0)
            assertThat(success.totalViewsChange).isEqualTo(0)
            assertThat(success.totalViewsChangePercent)
                .isEqualTo(0.0)
        }

    private fun createVideoPlayItems() = listOf(
        VideoPlayDataItem(
            title = TEST_VIDEO_TITLE_1,
            views = TEST_VIDEO_VIEWS_1
        ),
        VideoPlayDataItem(
            title = TEST_VIDEO_TITLE_2,
            views = TEST_VIDEO_VIEWS_2
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L

        private const val TEST_VIDEO_TITLE_1 = "My First Video"
        private const val TEST_VIDEO_TITLE_2 = "Tutorial Video"
        private const val TEST_VIDEO_VIEWS_1 = 500L
        private const val TEST_VIDEO_VIEWS_2 = 300L
    }
}
