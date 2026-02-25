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
import org.wordpress.android.ui.newstats.datasource.SearchTermDataItem
import org.wordpress.android.ui.newstats.datasource.SearchTermsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType

@ExperimentalCoroutinesApi
class StatsRepositorySearchTermsTest : BaseUnitTest() {
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
    fun `given successful response, when fetchSearchTerms, then success result is returned`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(
                    createSearchTermItems()
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Success::class.java)
            val success = result as SearchTermsResult.Success
            assertThat(success.items).hasSize(2)
            assertThat(success.items[0].name)
                .isEqualTo(TEST_TERM_NAME_1)
            assertThat(success.items[0].views)
                .isEqualTo(TEST_TERM_VIEWS_1)
            assertThat(success.items[1].name)
                .isEqualTo(TEST_TERM_NAME_2)
            assertThat(success.items[1].views)
                .isEqualTo(TEST_TERM_VIEWS_2)
        }

    @Test
    fun `given successful response, when fetchSearchTerms, then totalViews is sum of item views`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(
                    createSearchTermItems()
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Success::class.java)
            val success = result as SearchTermsResult.Success
            assertThat(success.totalViews)
                .isEqualTo(TEST_TERM_VIEWS_1 + TEST_TERM_VIEWS_2)
        }

    @Test
    fun `given current and previous data, when fetchSearchTerms, then change is calculated correctly`() =
        test {
            val currentItems = listOf(
                SearchTermDataItem("wordpress", 150),
                SearchTermDataItem("blog", 100)
            )
            val previousItems = listOf(
                SearchTermDataItem("wordpress", 100),
                SearchTermDataItem("blog", 100)
            )

            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(currentItems)
            ).thenReturn(
                SearchTermsDataResult.Success(previousItems)
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Success::class.java)
            val success = result as SearchTermsResult.Success
            // Current total: 250, Previous total: 200, Change: 50
            assertThat(success.totalViews).isEqualTo(250)
            assertThat(success.totalViewsChange).isEqualTo(50)
            assertThat(success.totalViewsChangePercent)
                .isEqualTo(25.0)
        }

    @Test
    fun `given item in both periods, when fetchSearchTerms, then previousViews is set correctly`() =
        test {
            val currentItems = listOf(
                SearchTermDataItem("wordpress", 150)
            )
            val previousItems = listOf(
                SearchTermDataItem("wordpress", 100)
            )

            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(currentItems)
            ).thenReturn(
                SearchTermsDataResult.Success(previousItems)
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Success::class.java)
            val success = result as SearchTermsResult.Success
            assertThat(success.items[0].previousViews)
                .isEqualTo(100)
            assertThat(success.items[0].viewsChange)
                .isEqualTo(50)
            assertThat(success.items[0].viewsChangePercent)
                .isEqualTo(50.0)
        }

    @Test
    fun `given new item not in previous period, when fetchSearchTerms, then previousViews is zero`() =
        test {
            val currentItems = listOf(
                SearchTermDataItem("new term", 100)
            )
            val previousItems = emptyList<SearchTermDataItem>()

            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(currentItems)
            ).thenReturn(
                SearchTermsDataResult.Success(previousItems)
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Success::class.java)
            val success = result as SearchTermsResult.Success
            assertThat(success.items[0].previousViews)
                .isEqualTo(0)
            assertThat(success.items[0].viewsChange)
                .isEqualTo(100)
            assertThat(success.items[0].viewsChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given previous fetch fails, when fetchSearchTerms, then previousViews defaults to zero`() =
        test {
            val currentItems = listOf(
                SearchTermDataItem("wordpress", 100)
            )

            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(currentItems)
            ).thenReturn(
                SearchTermsDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Success::class.java)
            val success = result as SearchTermsResult.Success
            assertThat(success.items[0].previousViews)
                .isEqualTo(0)
            assertThat(success.totalViewsChange).isEqualTo(100)
            assertThat(success.totalViewsChangePercent)
                .isEqualTo(100.0)
        }

    @Test
    fun `given error response, when fetchSearchTerms, then error result is returned`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Error::class.java)
            assertThat(
                (result as SearchTermsResult.Error).messageResId
            ).isEqualTo(R.string.stats_error_network)
        }

    @Test
    fun `given auth error, when fetchSearchTerms, then isAuthError is true`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Error(
                    StatsErrorType.AUTH_ERROR
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Error::class.java)
            val error = result as SearchTermsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_auth)
            assertThat(error.isAuthError).isTrue()
        }

    @Test
    fun `given non-auth error, when fetchSearchTerms, then isAuthError is false`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Error::class.java)
            assertThat(
                (result as SearchTermsResult.Error).isAuthError
            ).isFalse()
        }

    @Test
    fun `given parsing error, when fetchSearchTerms, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Error(
                    StatsErrorType.PARSING_ERROR
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Error::class.java)
            val error = result as SearchTermsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_parsing)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given api error, when fetchSearchTerms, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Error(
                    StatsErrorType.API_ERROR
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Error::class.java)
            val error = result as SearchTermsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_api)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `given unknown error, when fetchSearchTerms, then correct message is returned`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Error(
                    StatsErrorType.UNKNOWN
                )
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Error::class.java)
            val error = result as SearchTermsResult.Error
            assertThat(error.messageResId)
                .isEqualTo(R.string.stats_error_unknown)
            assertThat(error.isAuthError).isFalse()
        }

    @Test
    fun `when fetchSearchTerms is called, then data source is called twice for comparison`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(
                    createSearchTermItems()
                )
            )

            repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            verify(
                statsDataSource, times(2)
            ).fetchSearchTerms(
                siteId = eq(TEST_SITE_ID),
                dateRange = any(),
                max = eq(0)
            )
        }

    @Test
    fun `given empty items list, when fetchSearchTerms, then success with empty list is returned`() =
        test {
            whenever(
                statsDataSource.fetchSearchTerms(any(), any(), any())
            ).thenReturn(
                SearchTermsDataResult.Success(emptyList())
            )

            val result = repository.fetchSearchTerms(
                TEST_SITE_ID, StatsPeriod.Last7Days
            )

            assertThat(result)
                .isInstanceOf(SearchTermsResult.Success::class.java)
            val success = result as SearchTermsResult.Success
            assertThat(success.items).isEmpty()
            assertThat(success.totalViews).isEqualTo(0)
            assertThat(success.totalViewsChange).isEqualTo(0)
            assertThat(success.totalViewsChangePercent)
                .isEqualTo(0.0)
        }

    private fun createSearchTermItems() = listOf(
        SearchTermDataItem(
            name = TEST_TERM_NAME_1,
            views = TEST_TERM_VIEWS_1
        ),
        SearchTermDataItem(
            name = TEST_TERM_NAME_2,
            views = TEST_TERM_VIEWS_2
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L

        private const val TEST_TERM_NAME_1 = "wordpress"
        private const val TEST_TERM_NAME_2 = "blog tips"
        private const val TEST_TERM_VIEWS_1 = 500L
        private const val TEST_TERM_VIEWS_2 = 300L
    }
}
