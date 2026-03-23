package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.StatsInsightsDataResult
import org.wordpress.android.ui.newstats.datasource.YearInsightsData

@ExperimentalCoroutinesApi
class StatsRepositoryInsightsTest : BaseUnitTest() {
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
    fun `given successful response, when fetchInsights, then success result is returned`() =
        test {
            whenever(statsDataSource.fetchStatsInsights(any()))
                .thenReturn(
                    StatsInsightsDataResult.Success(
                        createTestInsightsData()
                    )
                )

            val result = repository.fetchInsights(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                InsightsResult.Success::class.java
            )
            val success = result as InsightsResult.Success
            assertThat(success.data.years).hasSize(2)
            assertThat(success.data.years[0].year).isEqualTo("2025")
            assertThat(success.data.years[0].totalPosts)
                .isEqualTo(TEST_TOTAL_POSTS)
            assertThat(success.data.years[0].totalWords)
                .isEqualTo(TEST_TOTAL_WORDS)
            assertThat(success.data.years[0].totalLikes)
                .isEqualTo(TEST_TOTAL_LIKES)
            assertThat(success.data.years[0].totalComments)
                .isEqualTo(TEST_TOTAL_COMMENTS)
        }

    @Test
    fun `given error response, when fetchInsights, then error result is returned`() =
        test {
            whenever(statsDataSource.fetchStatsInsights(any()))
                .thenReturn(
                    StatsInsightsDataResult.Error(
                        StatsErrorType.NETWORK_ERROR
                    )
                )

            val result = repository.fetchInsights(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                InsightsResult.Error::class.java
            )
        }

    @Test
    fun `given empty years list, when fetchInsights, then success with empty list`() =
        test {
            val emptyData = StatsInsightsData(
                highestHour = 0,
                highestHourPercent = 0.0,
                highestDayOfWeek = 0,
                highestDayPercent = 0.0,
                years = emptyList()
            )
            whenever(statsDataSource.fetchStatsInsights(any()))
                .thenReturn(
                    StatsInsightsDataResult.Success(emptyData)
                )

            val result = repository.fetchInsights(TEST_SITE_ID)

            assertThat(result).isInstanceOf(
                InsightsResult.Success::class.java
            )
            val success = result as InsightsResult.Success
            assertThat(success.data.years).isEmpty()
        }

    @Test
    fun `when fetchInsights is called, then data source is called with correct site id`() =
        test {
            whenever(statsDataSource.fetchStatsInsights(any()))
                .thenReturn(
                    StatsInsightsDataResult.Success(
                        createTestInsightsData()
                    )
                )

            repository.fetchInsights(TEST_SITE_ID)

            verify(statsDataSource).fetchStatsInsights(
                TEST_SITE_ID
            )
        }

    @Test
    fun `given multiple years, when fetchInsights, then all years are mapped correctly`() =
        test {
            whenever(statsDataSource.fetchStatsInsights(any()))
                .thenReturn(
                    StatsInsightsDataResult.Success(
                        createTestInsightsData()
                    )
                )

            val result = repository.fetchInsights(TEST_SITE_ID)

            val success = result as InsightsResult.Success
            assertThat(success.data.years[0].year).isEqualTo("2025")
            assertThat(success.data.years[1].year).isEqualTo("2024")
            assertThat(success.data.years[1].totalPosts).isEqualTo(38L)
        }

    private fun createTestInsightsData() = StatsInsightsData(
        highestHour = 14,
        highestHourPercent = 15.5,
        highestDayOfWeek = 3,
        highestDayPercent = 25.0,
        years = listOf(
            YearInsightsData(
                year = "2025",
                totalPosts = TEST_TOTAL_POSTS,
                totalWords = TEST_TOTAL_WORDS,
                avgWords = 357.1,
                totalLikes = TEST_TOTAL_LIKES,
                avgLikes = 5.5,
                totalComments = TEST_TOTAL_COMMENTS,
                avgComments = 2.0
            ),
            YearInsightsData(
                year = "2024",
                totalPosts = 38L,
                totalWords = 12500L,
                avgWords = 328.9,
                totalLikes = 180L,
                avgLikes = 4.7,
                totalComments = 60L,
                avgComments = 1.6
            )
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_TOTAL_POSTS = 42L
        private const val TEST_TOTAL_WORDS = 15000L
        private const val TEST_TOTAL_LIKES = 230L
        private const val TEST_TOTAL_COMMENTS = 85L
    }
}
