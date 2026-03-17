package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.datasource.EmailSummaryItem
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsEmailsSummaryDataResult
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.ui.newstats.datasource.StatsSubscribersData
import org.wordpress.android.ui.newstats.datasource.StatsSubscribersDataResult
import org.wordpress.android.ui.newstats.datasource.SubscriberItem
import org.wordpress.android.ui.newstats.datasource.SubscribersByUserTypeDataResult
import org.wordpress.android.ui.newstats.datasource.SubscribersDataPoint

@ExperimentalCoroutinesApi
class StatsRepositorySubscribersTest : BaseUnitTest() {
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

    // region fetchSubscribersAllTime
    @Test
    fun `given all calls succeed, when fetchSubscribersAllTime, then counts are extracted`() =
        test {
            whenever(statsDataSource.fetchStatsSubscribers(any(), any(), anyOrNull(), anyOrNull()))
                .thenReturn(
                    StatsSubscribersDataResult.Success(
                        StatsSubscribersData(
                            subscribersData = listOf(
                                SubscribersDataPoint(
                                    date = "2024-01",
                                    count = 500L
                                )
                            )
                        )
                    )
                )

            val result = repository.fetchSubscribersAllTime(TEST_SITE_ID)

            assertThat(result).isInstanceOf(SubscribersAllTimeResult.Success::class.java)
            val success = result as SubscribersAllTimeResult.Success
            assertThat(success.currentCount).isEqualTo(500L)
            assertThat(success.count30DaysAgo).isEqualTo(500L)
            assertThat(success.count60DaysAgo).isEqualTo(500L)
            assertThat(success.count90DaysAgo).isEqualTo(500L)
        }

    @Test
    fun `given empty subscribers data, when fetchSubscribersAllTime, then counts are zero`() =
        test {
            whenever(statsDataSource.fetchStatsSubscribers(any(), any(), anyOrNull(), anyOrNull()))
                .thenReturn(
                    StatsSubscribersDataResult.Success(
                        StatsSubscribersData(subscribersData = emptyList())
                    )
                )

            val result = repository.fetchSubscribersAllTime(TEST_SITE_ID)

            val success = result as SubscribersAllTimeResult.Success
            assertThat(success.currentCount).isEqualTo(0L)
        }

    @Test
    fun `given one call fails, when fetchSubscribersAllTime, then error is returned`() =
        test {
            whenever(statsDataSource.fetchStatsSubscribers(any(), any(), anyOrNull(), anyOrNull()))
                .thenReturn(
                    StatsSubscribersDataResult.Error(StatsErrorType.API_ERROR)
                )

            val result = repository.fetchSubscribersAllTime(TEST_SITE_ID)

            assertThat(result).isInstanceOf(SubscribersAllTimeResult.Error::class.java)
            val error = result as SubscribersAllTimeResult.Error
            assertThat(error.messageResId).isEqualTo(R.string.stats_error_api)
        }

    @Test
    fun `given auth error, when fetchSubscribersAllTime, then isAuthError is true`() =
        test {
            whenever(statsDataSource.fetchStatsSubscribers(any(), any(), anyOrNull(), anyOrNull()))
                .thenReturn(
                    StatsSubscribersDataResult.Error(StatsErrorType.AUTH_ERROR)
                )

            val result = repository.fetchSubscribersAllTime(TEST_SITE_ID)

            val error = result as SubscribersAllTimeResult.Error
            assertThat(error.isAuthError).isTrue()
        }
    // endregion

    // region fetchSubscribersList
    @Test
    fun `given success, when fetchSubscribersList, then items are mapped correctly`() =
        test {
            whenever(statsDataSource.fetchSubscribersByUserType(any(), any(), any()))
                .thenReturn(
                    SubscribersByUserTypeDataResult.Success(
                        listOf(
                            SubscriberItem(
                                displayName = "John",
                                subscribedSince = "2024-01-15"
                            ),
                            SubscriberItem(
                                displayName = "Jane",
                                subscribedSince = "2024-02-20"
                            )
                        )
                    )
                )

            val result = repository.fetchSubscribersList(TEST_SITE_ID)

            assertThat(result).isInstanceOf(SubscribersListResult.Success::class.java)
            val success = result as SubscribersListResult.Success
            assertThat(success.subscribers).hasSize(2)
            assertThat(success.subscribers[0].displayName).isEqualTo("John")
            assertThat(success.subscribers[0].subscribedSince).isEqualTo("2024-01-15")
            assertThat(success.subscribers[1].displayName).isEqualTo("Jane")
        }

    @Test
    fun `given page param, when fetchSubscribersList, then page is forwarded`() =
        test {
            whenever(statsDataSource.fetchSubscribersByUserType(any(), any(), any()))
                .thenReturn(SubscribersByUserTypeDataResult.Success(emptyList()))

            repository.fetchSubscribersList(TEST_SITE_ID, perPage = 20, page = 3)

            verify(statsDataSource).fetchSubscribersByUserType(
                eq(TEST_SITE_ID), eq(20), eq(3)
            )
        }

    @Test
    fun `given empty response, when fetchSubscribersList, then empty list is returned`() =
        test {
            whenever(statsDataSource.fetchSubscribersByUserType(any(), any(), any()))
                .thenReturn(SubscribersByUserTypeDataResult.Success(emptyList()))

            val result = repository.fetchSubscribersList(TEST_SITE_ID)

            val success = result as SubscribersListResult.Success
            assertThat(success.subscribers).isEmpty()
        }

    @Test
    fun `given error, when fetchSubscribersList, then error result is returned`() =
        test {
            whenever(statsDataSource.fetchSubscribersByUserType(any(), any(), any()))
                .thenReturn(
                    SubscribersByUserTypeDataResult.Error(StatsErrorType.API_ERROR)
                )

            val result = repository.fetchSubscribersList(TEST_SITE_ID)

            assertThat(result).isInstanceOf(SubscribersListResult.Error::class.java)
            val error = result as SubscribersListResult.Error
            assertThat(error.messageResId).isEqualTo(R.string.stats_error_api)
        }

    @Test
    fun `given auth error, when fetchSubscribersList, then isAuthError is true`() =
        test {
            whenever(statsDataSource.fetchSubscribersByUserType(any(), any(), any()))
                .thenReturn(
                    SubscribersByUserTypeDataResult.Error(StatsErrorType.AUTH_ERROR)
                )

            val result = repository.fetchSubscribersList(TEST_SITE_ID)

            val error = result as SubscribersListResult.Error
            assertThat(error.isAuthError).isTrue()
        }
    // endregion

    // region fetchSubscribersGraph
    @Test
    fun `given success, when fetchSubscribersGraph, then data points are mapped`() =
        test {
            whenever(
                statsDataSource.fetchStatsSubscribers(
                    any(), any(), anyOrNull(), anyOrNull()
                )
            ).thenReturn(
                StatsSubscribersDataResult.Success(
                    StatsSubscribersData(
                        subscribersData = listOf(
                            SubscribersDataPoint(
                                date = "2026-02-25",
                                count = 100L
                            ),
                            SubscribersDataPoint(
                                date = "2026-02-26",
                                count = 150L
                            )
                        )
                    )
                )
            )

            val result = repository.fetchSubscribersGraph(
                TEST_SITE_ID, "day", 30, "2026-02-27"
            )

            assertThat(result).isInstanceOf(
                SubscribersGraphResult.Success::class.java
            )
            val success =
                result as SubscribersGraphResult.Success
            assertThat(success.dataPoints).hasSize(2)
            assertThat(success.dataPoints[0].date)
                .isEqualTo("2026-02-25")
            assertThat(success.dataPoints[0].count)
                .isEqualTo(100L)
            assertThat(success.dataPoints[1].count)
                .isEqualTo(150L)
        }

    @Test
    fun `given empty data, when fetchSubscribersGraph, then empty list returned`() =
        test {
            whenever(
                statsDataSource.fetchStatsSubscribers(
                    any(), any(), anyOrNull(), anyOrNull()
                )
            ).thenReturn(
                StatsSubscribersDataResult.Success(
                    StatsSubscribersData(
                        subscribersData = emptyList()
                    )
                )
            )

            val result = repository.fetchSubscribersGraph(
                TEST_SITE_ID, "day", 30, "2026-02-27"
            )

            val success =
                result as SubscribersGraphResult.Success
            assertThat(success.dataPoints).isEmpty()
        }

    @Test
    fun `given error, when fetchSubscribersGraph, then error result returned`() =
        test {
            whenever(
                statsDataSource.fetchStatsSubscribers(
                    any(), any(), anyOrNull(), anyOrNull()
                )
            ).thenReturn(
                StatsSubscribersDataResult.Error(
                    StatsErrorType.API_ERROR
                )
            )

            val result = repository.fetchSubscribersGraph(
                TEST_SITE_ID, "week", 12, "2026-02-27"
            )

            assertThat(result).isInstanceOf(
                SubscribersGraphResult.Error::class.java
            )
            val error =
                result as SubscribersGraphResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_api)
        }

    @Test
    fun `given auth error, when fetchSubscribersGraph, then isAuthError is true`() =
        test {
            whenever(
                statsDataSource.fetchStatsSubscribers(
                    any(), any(), anyOrNull(), anyOrNull()
                )
            ).thenReturn(
                StatsSubscribersDataResult.Error(
                    StatsErrorType.AUTH_ERROR
                )
            )

            val result = repository.fetchSubscribersGraph(
                TEST_SITE_ID, "month", 6, "2026-02-27"
            )

            val error =
                result as SubscribersGraphResult.Error
            assertThat(error.isAuthError).isTrue()
        }
    // endregion

    // region fetchEmailsSummary
    @Test
    fun `given success, when fetchEmailsSummary, then items are mapped correctly`() =
        test {
            whenever(statsDataSource.fetchStatsEmailsSummary(any(), any()))
                .thenReturn(
                    StatsEmailsSummaryDataResult.Success(
                        listOf(
                            EmailSummaryItem(
                                title = "Newsletter #1",
                                opens = 500L,
                                clicks = 42L
                            ),
                            EmailSummaryItem(
                                title = "Newsletter #2",
                                opens = 300L,
                                clicks = 25L
                            )
                        )
                    )
                )

            val result = repository.fetchEmailsSummary(TEST_SITE_ID)

            assertThat(result).isInstanceOf(EmailsStatsResult.Success::class.java)
            val success = result as EmailsStatsResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].title).isEqualTo("Newsletter #1")
            assertThat(success.items[0].opens).isEqualTo(500L)
            assertThat(success.items[0].clicks).isEqualTo(42L)
        }

    @Test
    fun `given empty response, when fetchEmailsSummary, then empty list is returned`() =
        test {
            whenever(statsDataSource.fetchStatsEmailsSummary(any(), any()))
                .thenReturn(StatsEmailsSummaryDataResult.Success(emptyList()))

            val result = repository.fetchEmailsSummary(TEST_SITE_ID)

            val success = result as EmailsStatsResult.Success
            assertThat(success.items).isEmpty()
        }

    @Test
    fun `given error, when fetchEmailsSummary, then error result is returned`() =
        test {
            whenever(statsDataSource.fetchStatsEmailsSummary(any(), any()))
                .thenReturn(
                    StatsEmailsSummaryDataResult.Error(StatsErrorType.API_ERROR)
                )

            val result = repository.fetchEmailsSummary(TEST_SITE_ID)

            assertThat(result).isInstanceOf(EmailsStatsResult.Error::class.java)
            val error = result as EmailsStatsResult.Error
            assertThat(error.messageResId).isEqualTo(R.string.stats_error_api)
        }

    @Test
    fun `given auth error, when fetchEmailsSummary, then isAuthError is true`() =
        test {
            whenever(statsDataSource.fetchStatsEmailsSummary(any(), any()))
                .thenReturn(
                    StatsEmailsSummaryDataResult.Error(StatsErrorType.AUTH_ERROR)
                )

            val result = repository.fetchEmailsSummary(TEST_SITE_ID)

            val error = result as EmailsStatsResult.Error
            assertThat(error.isAuthError).isTrue()
        }
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
