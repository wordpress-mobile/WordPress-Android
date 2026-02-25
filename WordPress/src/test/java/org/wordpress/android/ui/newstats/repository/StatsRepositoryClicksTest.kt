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
import org.wordpress.android.ui.newstats.datasource.ClickDataItem
import org.wordpress.android.ui.newstats.datasource.ClicksDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType

@ExperimentalCoroutinesApi
class StatsRepositoryClicksTest : BaseUnitTest() {
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
    fun `given successful response, when fetchClicks, then success result is returned`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Success(createClickItems())
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Success::class.java)
            val success = result as ClicksResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].name)
                .isEqualTo(TEST_CLICK_NAME_1)
            assertThat(success.items[0].clicks)
                .isEqualTo(TEST_CLICK_CLICKS_1)
            assertThat(success.items[1].name)
                .isEqualTo(TEST_CLICK_NAME_2)
            assertThat(success.items[1].clicks)
                .isEqualTo(TEST_CLICK_CLICKS_2)
        }

    @Test
    fun `given successful response, when fetchClicks, then totalClicks is sum of item clicks`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Success(createClickItems())
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Success::class.java)
            val success = result as ClicksResult.Success
            assertThat(success.totalClicks)
                .isEqualTo(TEST_CLICK_CLICKS_1 + TEST_CLICK_CLICKS_2)
        }

    @Test
    fun `given current and previous data, when fetchClicks, then change is calculated correctly`() =
        test {
            val currentItems = listOf(
                ClickDataItem("Link 1", 150),
                ClickDataItem("Link 2", 100)
            )
            val previousItems = listOf(
                ClickDataItem("Link 1", 100),
                ClickDataItem("Link 2", 100)
            )

            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(ClicksDataResult.Success(currentItems))
                .thenReturn(ClicksDataResult.Success(previousItems))

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Success::class.java)
            val success = result as ClicksResult.Success
            // Current total: 250, Previous total: 200, Change: 50
            assertThat(success.totalClicks).isEqualTo(250)
            assertThat(success.totalClicksChange).isEqualTo(50)
            assertThat(success.totalClicksChangePercent)
                .isEqualTo(25.0)
        }

    @Test
    fun `given item in both periods, when fetchClicks, then previousClicks is set correctly`() =
        test {
            val currentItems = listOf(
                ClickDataItem("Link 1", 150)
            )
            val previousItems = listOf(
                ClickDataItem("Link 1", 100)
            )

            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(ClicksDataResult.Success(currentItems))
                .thenReturn(ClicksDataResult.Success(previousItems))

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Success::class.java)
            val success = result as ClicksResult.Success
            assertThat(success.items[0].previousClicks)
                .isEqualTo(100)
            assertThat(success.items[0].clicksChange)
                .isEqualTo(50)
            assertThat(success.items[0].clicksChangePercent)
                .isEqualTo(50.0)
        }

    @Test
    fun `given new item not in previous period, when fetchClicks, then previousClicks is zero`() =
        test {
            val currentItems = listOf(
                ClickDataItem("New Link", 100)
            )
            val previousItems = emptyList<ClickDataItem>()

            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(ClicksDataResult.Success(currentItems))
                .thenReturn(
                    ClicksDataResult.Success(previousItems)
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Success::class.java)
            val success = result as ClicksResult.Success
            assertThat(success.items[0].previousClicks)
                .isEqualTo(0)
            assertThat(success.items[0].clicksChange)
                .isEqualTo(100)
            assertThat(success.items[0].clicksChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given previous fetch fails, when fetchClicks, then previousClicks defaults to zero`() =
        test {
            val currentItems = listOf(
                ClickDataItem("Link 1", 100)
            )

            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(ClicksDataResult.Success(currentItems))
                .thenReturn(
                    ClicksDataResult.Error(
                        StatsErrorType.NETWORK_ERROR
                    )
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Success::class.java)
            val success = result as ClicksResult.Success
            assertThat(success.items[0].previousClicks)
                .isEqualTo(0)
            assertThat(success.totalClicksChange).isEqualTo(100)
            assertThat(success.totalClicksChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given error response, when fetchClicks, then error result is returned`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Error(
                        StatsErrorType.NETWORK_ERROR
                    )
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Error::class.java)
            assertThat(
                (result as ClicksResult.Error).messageResId
            ).isEqualTo(R.string.stats_error_network)
        }

    @Test
    fun `given auth error, when fetchClicks, then isAuthError is true`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Error(
                        StatsErrorType.AUTH_ERROR
                    )
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Error::class.java)
            val error = result as ClicksResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_auth)
            assertThat(error.isAuthError).isTrue()
        }

    @Test
    fun `given non-auth error, when fetchClicks, then isAuthError is false`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Error(
                        StatsErrorType.NETWORK_ERROR
                    )
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Error::class.java)
            assertThat(
                (result as ClicksResult.Error).isAuthError
            ).isFalse()
        }

    @Test
    fun `given parsing error, when fetchClicks, then correct message is returned`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Error(
                        StatsErrorType.PARSING_ERROR
                    )
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Error::class.java)
            val error = result as ClicksResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_parsing)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given api error, when fetchClicks, then correct message is returned`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Error(StatsErrorType.API_ERROR)
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Error::class.java)
            val error = result as ClicksResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_api)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given unknown error, when fetchClicks, then correct message is returned`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Error(StatsErrorType.UNKNOWN)
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Error::class.java)
            val error = result as ClicksResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_unknown)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `when fetchClicks is called, then data source is called twice for comparison`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Success(createClickItems())
                )

            repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            verify(statsDataSource, times(2)).fetchClicks(
                siteId = eq(TEST_SITE_ID),
                dateRange = any(),
                max = eq(0)
            )
        }

    @Test
    fun `given empty items list, when fetchClicks, then success with empty list is returned`() =
        test {
            whenever(statsDataSource.fetchClicks(any(), any(), any()))
                .thenReturn(
                    ClicksDataResult.Success(emptyList())
                )

            val result = repository.fetchClicks(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(ClicksResult.Success::class.java)
            val success = result as ClicksResult.Success
            assertThat(success.items).isEmpty()
            assertThat(success.totalClicks).isEqualTo(0)
            assertThat(success.totalClicksChange).isEqualTo(0)
            assertThat(success.totalClicksChangePercent)
                .isEqualTo(0.0)
        }

    private fun createClickItems() = listOf(
        ClickDataItem(
            name = TEST_CLICK_NAME_1,
            clicks = TEST_CLICK_CLICKS_1
        ),
        ClickDataItem(
            name = TEST_CLICK_NAME_2,
            clicks = TEST_CLICK_CLICKS_2
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L

        private const val TEST_CLICK_NAME_1 = "example.com"
        private const val TEST_CLICK_NAME_2 = "wordpress.org"
        private const val TEST_CLICK_CLICKS_1 = 500L
        private const val TEST_CLICK_CLICKS_2 = 300L
    }
}
