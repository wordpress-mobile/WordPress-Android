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
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.TopAuthorItem
import org.wordpress.android.ui.newstats.datasource.TopAuthorsData
import org.wordpress.android.ui.newstats.datasource.TopAuthorsDataResult

@ExperimentalCoroutinesApi
class StatsRepositoryAuthorsTest : BaseUnitTest() {
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
    fun `given successful response, when fetchTopAuthors, then success result is returned`() = test {
        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(createTopAuthorsData()))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Success::class.java)
        val success = result as TopAuthorsResult.Success
        assertThat(success.authors).hasSize(2)
        assertThat(success.authors[0].name).isEqualTo(TEST_AUTHOR_NAME_1)
        assertThat(success.authors[0].avatarUrl).isEqualTo(TEST_AUTHOR_AVATAR_1)
        assertThat(success.authors[0].views).isEqualTo(TEST_AUTHOR_VIEWS_1)
        assertThat(success.authors[1].name).isEqualTo(TEST_AUTHOR_NAME_2)
        assertThat(success.authors[1].views).isEqualTo(TEST_AUTHOR_VIEWS_2)
    }

    @Test
    fun `given successful response, when fetchTopAuthors, then totalViews is sum of author views`() = test {
        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(createTopAuthorsData()))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Success::class.java)
        val success = result as TopAuthorsResult.Success
        assertThat(success.totalViews).isEqualTo(TEST_AUTHOR_VIEWS_1 + TEST_AUTHOR_VIEWS_2)
    }

    @Test
    fun `given current and previous data, when fetchTopAuthors, then change is calculated correctly`() = test {
        val currentData = TopAuthorsData(
            authors = listOf(
                TopAuthorItem("Author 1", "https://example.com/a1.jpg", 150),
                TopAuthorItem("Author 2", "https://example.com/a2.jpg", 100)
            ),
            totalViews = 250L
        )
        val previousData = TopAuthorsData(
            authors = listOf(
                TopAuthorItem("Author 1", "https://example.com/a1.jpg", 100),
                TopAuthorItem("Author 2", "https://example.com/a2.jpg", 100)
            ),
            totalViews = 200L
        )

        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(currentData))
            .thenReturn(TopAuthorsDataResult.Success(previousData))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Success::class.java)
        val success = result as TopAuthorsResult.Success
        // Current total: 250, Previous total: 200, Change: 50
        assertThat(success.totalViews).isEqualTo(250)
        assertThat(success.totalViewsChange).isEqualTo(50)
        assertThat(success.totalViewsChangePercent).isEqualTo(25.0)
    }

    @Test
    fun `given author in both periods, when fetchTopAuthors, then previousViews is set correctly`() = test {
        val currentData = TopAuthorsData(
            authors = listOf(TopAuthorItem("Author 1", null, 150)),
            totalViews = 150L
        )
        val previousData = TopAuthorsData(
            authors = listOf(TopAuthorItem("Author 1", null, 100)),
            totalViews = 100L
        )

        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(currentData))
            .thenReturn(TopAuthorsDataResult.Success(previousData))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Success::class.java)
        val success = result as TopAuthorsResult.Success
        assertThat(success.authors[0].previousViews).isEqualTo(100)
        assertThat(success.authors[0].viewsChange).isEqualTo(50)
        assertThat(success.authors[0].viewsChangePercent).isEqualTo(50.0)
    }

    @Test
    fun `given new author not in previous period, when fetchTopAuthors, then previousViews is zero`() = test {
        val currentData = TopAuthorsData(
            authors = listOf(TopAuthorItem("New Author", null, 100)),
            totalViews = 100L
        )
        val previousData = TopAuthorsData(
            authors = emptyList(),
            totalViews = 0L
        )

        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(currentData))
            .thenReturn(TopAuthorsDataResult.Success(previousData))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Success::class.java)
        val success = result as TopAuthorsResult.Success
        assertThat(success.authors[0].previousViews).isEqualTo(0)
        assertThat(success.authors[0].viewsChange).isEqualTo(100)
        assertThat(success.authors[0].viewsChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `given previous fetch fails, when fetchTopAuthors, then previousViews defaults to zero`() = test {
        val currentData = TopAuthorsData(
            authors = listOf(TopAuthorItem("Author 1", null, 100)),
            totalViews = 100L
        )

        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(currentData))
            .thenReturn(TopAuthorsDataResult.Error("Network error"))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Success::class.java)
        val success = result as TopAuthorsResult.Success
        assertThat(success.authors[0].previousViews).isEqualTo(0)
        assertThat(success.totalViewsChange).isEqualTo(100)
        assertThat(success.totalViewsChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `given error response, when fetchTopAuthors, then error result is returned`() = test {
        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Error(ERROR_MESSAGE))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Error::class.java)
        assertThat((result as TopAuthorsResult.Error).message).isEqualTo(ERROR_MESSAGE)
    }

    @Test
    fun `when fetchTopAuthors is called, then data source is called twice for comparison`() = test {
        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(createTopAuthorsData()))

        repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        // Verify data source is called twice (current and previous period) with no limit
        verify(statsDataSource, times(2)).fetchTopAuthors(
            siteId = eq(TEST_SITE_ID),
            dateRange = any(),
            max = eq(0)
        )
    }

    @Test
    fun `given empty authors list, when fetchTopAuthors, then success with empty list is returned`() = test {
        val emptyData = TopAuthorsData(
            authors = emptyList(),
            totalViews = 0L
        )
        whenever(statsDataSource.fetchTopAuthors(any(), any(), any()))
            .thenReturn(TopAuthorsDataResult.Success(emptyData))

        val result = repository.fetchTopAuthors(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat(result).isInstanceOf(TopAuthorsResult.Success::class.java)
        val success = result as TopAuthorsResult.Success
        assertThat(success.authors).isEmpty()
        assertThat(success.totalViews).isEqualTo(0)
        assertThat(success.totalViewsChange).isEqualTo(0)
        assertThat(success.totalViewsChangePercent).isEqualTo(0.0)
    }

    private fun createTopAuthorsData() = TopAuthorsData(
        authors = listOf(
            TopAuthorItem(
                name = TEST_AUTHOR_NAME_1,
                avatarUrl = TEST_AUTHOR_AVATAR_1,
                views = TEST_AUTHOR_VIEWS_1
            ),
            TopAuthorItem(
                name = TEST_AUTHOR_NAME_2,
                avatarUrl = TEST_AUTHOR_AVATAR_2,
                views = TEST_AUTHOR_VIEWS_2
            )
        ),
        totalViews = TEST_AUTHOR_VIEWS_1 + TEST_AUTHOR_VIEWS_2
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val ERROR_MESSAGE = "Test error message"

        private const val TEST_AUTHOR_NAME_1 = "John Doe"
        private const val TEST_AUTHOR_NAME_2 = "Jane Smith"
        private const val TEST_AUTHOR_AVATAR_1 = "https://example.com/avatar1.jpg"
        private const val TEST_AUTHOR_AVATAR_2 = "https://example.com/avatar2.jpg"
        private const val TEST_AUTHOR_VIEWS_1 = 500L
        private const val TEST_AUTHOR_VIEWS_2 = 300L
    }
}
