package org.wordpress.android.ui.newstats.mostviewed

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.ClickItemData
import org.wordpress.android.ui.newstats.repository.ClicksResult
import org.wordpress.android.ui.newstats.repository.FileDownloadItemData
import org.wordpress.android.ui.newstats.repository.FileDownloadsResult
import org.wordpress.android.ui.newstats.repository.MostViewedItemData
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.SearchTermItemData
import org.wordpress.android.ui.newstats.repository.SearchTermsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.VideoPlayItemData
import org.wordpress.android.ui.newstats.repository.VideoPlaysResult

@ExperimentalCoroutinesApi
class MostViewedDetailFetcherTest : BaseUnitTest() {
    @Mock
    private lateinit var statsRepository: StatsRepository

    private lateinit var fetcher: MostViewedDetailFetcher

    @Before
    fun setUp() {
        fetcher = MostViewedDetailFetcher(statsRepository)
    }

    @Test
    fun `fetch initializes the repository with the access token`() = test {
        whenever(statsRepository.fetchClicks(any(), any())).thenReturn(emptyClicksSuccess())

        fetcher.fetch(MostViewedDetailSource.CLICKS, SITE_ID, PERIOD, ACCESS_TOKEN)

        verify(statsRepository).init(ACCESS_TOKEN)
    }

    // region Referrers
    @Test
    fun `fetch referrers maps the success result into the common shape`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any())).thenReturn(
            MostViewedResult.Success(
                items = listOf(
                    MostViewedItemData(
                        id = 7,
                        title = "Search Engines",
                        views = 30L,
                        previousViews = 10L,
                        isFirst = true
                    )
                ),
                totalViews = 100L,
                totalViewsChange = 40L,
                totalViewsChangePercent = 66.6
            )
        )

        val result = fetcher.fetch(MostViewedDetailSource.REFERRERS, SITE_ID, PERIOD, ACCESS_TOKEN)

        val success = result as StatsCardFetchResult.Success
        assertThat(success.items).hasSize(1)
        assertThat(success.items[0].id).isEqualTo(7L)
        assertThat(success.items[0].title).isEqualTo("Search Engines")
        assertThat(success.items[0].views).isEqualTo(30L)
        assertThat(success.totalValue).isEqualTo(100L)
        assertThat(success.totalValueChange).isEqualTo(40L)
        assertThat(success.totalValueChangePercent).isEqualTo(66.6)
    }

    @Test
    fun `fetch referrers maps the error result to the generic api error`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any()))
            .thenReturn(MostViewedResult.Error("network"))

        val result = fetcher.fetch(MostViewedDetailSource.REFERRERS, SITE_ID, PERIOD, ACCESS_TOKEN)

        val error = result as StatsCardFetchResult.Error
        assertThat(error.messageResId).isEqualTo(R.string.stats_error_api)
        assertThat(error.isAuthError).isFalse()
    }
    // endregion

    // region Clicks
    @Test
    fun `fetch clicks maps items with positional ids and clicks totals`() = test {
        whenever(statsRepository.fetchClicks(any(), any())).thenReturn(
            ClicksResult.Success(
                items = listOf(
                    ClickItemData(name = "example.com", clicks = 50L, previousClicks = 20L),
                    ClickItemData(name = "other.com", clicks = 10L, previousClicks = 10L)
                ),
                totalClicks = 60L,
                totalClicksChange = 30L,
                totalClicksChangePercent = 100.0
            )
        )

        val result = fetcher.fetch(MostViewedDetailSource.CLICKS, SITE_ID, PERIOD, ACCESS_TOKEN)

        val success = result as StatsCardFetchResult.Success
        assertThat(success.items).hasSize(2)
        assertThat(success.items[0].id).isEqualTo(0L)
        assertThat(success.items[0].title).isEqualTo("example.com")
        assertThat(success.items[0].views).isEqualTo(50L)
        assertThat(success.items[1].id).isEqualTo(1L)
        assertThat(success.totalValue).isEqualTo(60L)
        assertThat(success.totalValueChange).isEqualTo(30L)
        assertThat(success.totalValueChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `fetch clicks passes through the error message resId and auth flag`() = test {
        whenever(statsRepository.fetchClicks(any(), any()))
            .thenReturn(ClicksResult.Error(R.string.stats_error_unknown, isAuthError = true))

        val result = fetcher.fetch(MostViewedDetailSource.CLICKS, SITE_ID, PERIOD, ACCESS_TOKEN)

        val error = result as StatsCardFetchResult.Error
        assertThat(error.messageResId).isEqualTo(R.string.stats_error_unknown)
        assertThat(error.isAuthError).isTrue()
    }
    // endregion

    // region Search terms
    @Test
    fun `fetch search terms maps items and totals`() = test {
        whenever(statsRepository.fetchSearchTerms(any(), any())).thenReturn(
            SearchTermsResult.Success(
                items = listOf(SearchTermItemData(name = "kotlin", views = 12L, previousViews = 4L)),
                totalViews = 12L,
                totalViewsChange = 8L,
                totalViewsChangePercent = 200.0
            )
        )

        val result = fetcher.fetch(MostViewedDetailSource.SEARCH_TERMS, SITE_ID, PERIOD, ACCESS_TOKEN)

        val success = result as StatsCardFetchResult.Success
        assertThat(success.items[0].id).isEqualTo(0L)
        assertThat(success.items[0].title).isEqualTo("kotlin")
        assertThat(success.items[0].views).isEqualTo(12L)
        assertThat(success.totalValue).isEqualTo(12L)
    }

    @Test
    fun `fetch search terms passes through the error`() = test {
        whenever(statsRepository.fetchSearchTerms(any(), any()))
            .thenReturn(SearchTermsResult.Error(R.string.stats_error_api))

        val error = fetcher.fetch(MostViewedDetailSource.SEARCH_TERMS, SITE_ID, PERIOD, ACCESS_TOKEN)
            as StatsCardFetchResult.Error
        assertThat(error.messageResId).isEqualTo(R.string.stats_error_api)
    }
    // endregion

    // region Video plays
    @Test
    fun `fetch video plays maps the title field and totals`() = test {
        whenever(statsRepository.fetchVideoPlays(any(), any())).thenReturn(
            VideoPlaysResult.Success(
                items = listOf(VideoPlayItemData(title = "Intro.mp4", views = 9L, previousViews = 3L)),
                totalViews = 9L,
                totalViewsChange = 6L,
                totalViewsChangePercent = 200.0
            )
        )

        val result = fetcher.fetch(MostViewedDetailSource.VIDEO_PLAYS, SITE_ID, PERIOD, ACCESS_TOKEN)

        val success = result as StatsCardFetchResult.Success
        assertThat(success.items[0].title).isEqualTo("Intro.mp4")
        assertThat(success.items[0].views).isEqualTo(9L)
        assertThat(success.totalValue).isEqualTo(9L)
    }

    @Test
    fun `fetch video plays passes through the error`() = test {
        whenever(statsRepository.fetchVideoPlays(any(), any()))
            .thenReturn(VideoPlaysResult.Error(R.string.stats_error_api, isAuthError = true))

        val error = fetcher.fetch(MostViewedDetailSource.VIDEO_PLAYS, SITE_ID, PERIOD, ACCESS_TOKEN)
            as StatsCardFetchResult.Error
        assertThat(error.messageResId).isEqualTo(R.string.stats_error_api)
        assertThat(error.isAuthError).isTrue()
    }
    // endregion

    // region File downloads
    @Test
    fun `fetch file downloads maps downloads into views and totals`() = test {
        whenever(statsRepository.fetchFileDownloads(any(), any())).thenReturn(
            FileDownloadsResult.Success(
                items = listOf(FileDownloadItemData(name = "guide.pdf", downloads = 15L, previousDownloads = 5L)),
                totalDownloads = 15L,
                totalDownloadsChange = 10L,
                totalDownloadsChangePercent = 200.0
            )
        )

        val result = fetcher.fetch(MostViewedDetailSource.FILE_DOWNLOADS, SITE_ID, PERIOD, ACCESS_TOKEN)

        val success = result as StatsCardFetchResult.Success
        assertThat(success.items[0].title).isEqualTo("guide.pdf")
        assertThat(success.items[0].views).isEqualTo(15L)
        assertThat(success.totalValue).isEqualTo(15L)
        assertThat(success.totalValueChange).isEqualTo(10L)
    }

    @Test
    fun `fetch file downloads passes through the error`() = test {
        whenever(statsRepository.fetchFileDownloads(any(), any()))
            .thenReturn(FileDownloadsResult.Error(R.string.stats_error_unknown))

        val error = fetcher.fetch(MostViewedDetailSource.FILE_DOWNLOADS, SITE_ID, PERIOD, ACCESS_TOKEN)
            as StatsCardFetchResult.Error
        assertThat(error.messageResId).isEqualTo(R.string.stats_error_unknown)
    }
    // endregion

    private fun emptyClicksSuccess() = ClicksResult.Success(
        items = emptyList(),
        totalClicks = 0L,
        totalClicksChange = 0L,
        totalClicksChangePercent = 0.0
    )

    companion object {
        private const val SITE_ID = 123L
        private const val ACCESS_TOKEN = "test_access_token"
        private val PERIOD = StatsPeriod.Last7Days
    }
}
