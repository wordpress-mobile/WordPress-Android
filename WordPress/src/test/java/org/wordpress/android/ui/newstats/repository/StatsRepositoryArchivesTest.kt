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
import org.wordpress.android.ui.newstats.datasource.ArchiveEntryDataItem
import org.wordpress.android.ui.newstats.datasource.ArchiveSectionDataItem
import org.wordpress.android.ui.newstats.datasource.ArchivesDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import java.time.Clock

@ExperimentalCoroutinesApi
class StatsRepositoryArchivesTest : BaseUnitTest() {
    @Mock
    private lateinit var statsDataSource: StatsDataSource

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        repository = StatsRepository(
            statsDataSource = statsDataSource,
            statsResultCache = StatsResultCache(),
            appLogWrapper = appLogWrapper,
            clock = Clock.systemDefaultZone(),
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `given successful response, when fetchArchives, then sections keep their entries`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 30L))))

        val result = repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        val success = result as ArchivesResult.Success
        assertThat(success.sections).hasSize(1)
        assertThat(success.sections[0].key).isEqualTo("cat")
        assertThat(success.sections[0].views).isEqualTo(30L)
        assertThat(success.sections[0].entries).hasSize(1)
        assertThat(success.sections[0].entries[0].name).isEqualTo("benefits")
        assertThat(success.sections[0].entries[0].url).isEqualTo(CATEGORY_URL)
    }

    @Test
    fun `given sections out of order, when fetchArchives, then they are ranked by views`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any())).thenReturn(
            ArchivesDataResult.Success(
                listOf(categorySection(views = 30L), searchSection(views = 70L))
            )
        )

        val result = repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        val success = result as ArchivesResult.Success
        assertThat(success.sections.map { it.key }).containsExactly("search", "cat")
        assertThat(success.totalViews).isEqualTo(100L)
    }

    @Test
    fun `given section in both periods, when fetchArchives, then its change is calculated`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 150L))))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 100L))))

        val result = repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        val success = result as ArchivesResult.Success
        assertThat(success.sections[0].previousViews).isEqualTo(100L)
        assertThat(success.sections[0].viewsChange).isEqualTo(50L)
        assertThat(success.sections[0].viewsChangePercent).isEqualTo(50.0)
        assertThat(success.totalViewsChange).isEqualTo(50L)
    }

    @Test
    fun `given previous fetch fails, when fetchArchives, then previousViews defaults to zero`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 30L))))
            .thenReturn(ArchivesDataResult.Error(StatsErrorType.NETWORK_ERROR))

        val result = repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        val success = result as ArchivesResult.Success
        assertThat(success.sections[0].previousViews).isEqualTo(0L)
        assertThat(success.totalViewsChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `when fetchArchives is called, then the card asks for the top entries of both periods`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 30L))))

        repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        verify(statsDataSource, times(2)).fetchArchives(
            siteId = eq(TEST_SITE_ID),
            dateRange = any(),
            max = eq(CARD_MAX)
        )
    }

    @Test
    fun `when fetchArchivesDetail is called, then the data source is asked for every entry`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 30L))))

        repository.fetchArchivesDetail(TEST_SITE_ID, StatsPeriod.Last7Days)

        verify(statsDataSource, times(2)).fetchArchives(
            siteId = eq(TEST_SITE_ID),
            dateRange = any(),
            max = eq(DETAIL_MAX)
        )
    }

    @Test
    fun `given the card was fetched, when fetchArchivesDetail, then the capped result is not reused`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), eq(CARD_MAX)))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 30L))))
        whenever(statsDataSource.fetchArchives(any(), any(), eq(DETAIL_MAX)))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 300L))))

        repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)
        val detail = repository.fetchArchivesDetail(TEST_SITE_ID, StatsPeriod.Last7Days)

        // The two requests differ only in `max`, so sharing one cache bucket would serve the
        // card's truncated list to the detail screen.
        assertThat((detail as ArchivesResult.Success).sections[0].views).isEqualTo(300L)
        verify(statsDataSource, times(2)).fetchArchives(any(), any(), eq(DETAIL_MAX))
    }

    @Test
    fun `given the detail list is cached, when fetchArchives, then the card still asks for its own`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), eq(DETAIL_MAX)))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 300L))))
        whenever(statsDataSource.fetchArchives(any(), any(), eq(CARD_MAX)))
            .thenReturn(ArchivesDataResult.Success(listOf(categorySection(views = 30L))))

        repository.fetchArchivesDetail(TEST_SITE_ID, StatsPeriod.Last7Days)
        val card = repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        assertThat((card as ArchivesResult.Success).sections[0].views).isEqualTo(30L)
    }

    @Test
    fun `given auth error, when fetchArchives, then isAuthError is true`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesDataResult.Error(StatsErrorType.AUTH_ERROR))

        val result = repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        val error = result as ArchivesResult.Error
        assertThat(error.messageResId).isEqualTo(R.string.stats_error_auth)
        assertThat(error.isAuthError).isTrue()
    }

    @Test
    fun `given not available error, when fetchArchives, then isNotAvailable is true`() = test {
        whenever(statsDataSource.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesDataResult.Error(StatsErrorType.NOT_AVAILABLE))

        val result = repository.fetchArchives(TEST_SITE_ID, StatsPeriod.Last7Days)

        val error = result as ArchivesResult.Error
        assertThat(error.isNotAvailable).isTrue()
        assertThat(error.isAuthError).isFalse()
    }

    private fun categorySection(views: Long) = ArchiveSectionDataItem(
        key = "cat",
        views = views,
        entries = listOf(ArchiveEntryDataItem(value = "benefits", href = CATEGORY_URL, views = views))
    )

    private fun searchSection(views: Long) = ArchiveSectionDataItem(
        key = "search",
        views = views,
        entries = listOf(ArchiveEntryDataItem(value = "sabbatical", href = SEARCH_URL, views = views))
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val CARD_MAX = 10
        private const val DETAIL_MAX = 0
        private const val CATEGORY_URL = "https://example.com/category/benefits/"
        private const val SEARCH_URL = "https://example.com/?s=sabbatical"
    }
}
