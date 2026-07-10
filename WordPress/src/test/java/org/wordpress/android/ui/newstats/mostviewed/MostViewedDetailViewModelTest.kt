package org.wordpress.android.ui.newstats.mostviewed

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.MostViewedChildData
import org.wordpress.android.ui.newstats.repository.MostViewedItemData
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class MostViewedDetailViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var detailFetcher: MostViewedDetailFetcher
    private lateinit var viewModel: MostViewedDetailViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
        adminUrl = TEST_ADMIN_URL
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days)).thenReturn(DATE_RANGE)
        whenever(resourceProvider.getString(R.string.stats_error_api)).thenReturn(API_ERROR)
        whenever(resourceProvider.getString(R.string.stats_error_unknown)).thenReturn(UNKNOWN_ERROR)
        detailFetcher = MostViewedDetailFetcher(statsRepository)
        viewModel = MostViewedDetailViewModel(
            selectedSiteRepository,
            accountStore,
            detailFetcher,
            resourceProvider
        )
    }

    @Test
    fun `when load succeeds, then loaded state with items and totals is emitted`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedDetailUiState.Loaded::class.java)
        val loaded = state as MostViewedDetailUiState.Loaded
        assertThat(loaded.items).hasSize(1)
        assertThat(loaded.items[0].title).isEqualTo("Search Engines")
        assertThat(loaded.maxViewsForBar).isEqualTo(TEST_VIEWS)
        assertThat(loaded.totalViews).isEqualTo(TEST_TOTAL_VIEWS)
        assertThat(loaded.totalViewsChange).isEqualTo(TEST_TOTAL_VIEWS_CHANGE)
        assertThat(loaded.totalViewsChangePercent).isEqualTo(TEST_TOTAL_VIEWS_CHANGE_PERCENT)
        assertThat(loaded.dateRange).isEqualTo(DATE_RANGE)
    }

    @Test
    fun `when load succeeds, then child items propagate to the loaded state`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as MostViewedDetailUiState.Loaded
        assertThat(loaded.items[0].children).hasSize(1)
        assertThat(loaded.items[0].children[0].name).isEqualTo("Google Search")
        assertThat(loaded.items[0].children[0].url).isEqualTo(CHILD_URL)
        assertThat(loaded.items[0].children[0].views).isEqualTo(TEST_VIEWS)
    }

    @Test
    fun `when the source is clicks, then the clicks fetch backs the loaded state`() = test {
        whenever(statsRepository.fetchClicks(any(), any())).thenReturn(
            org.wordpress.android.ui.newstats.repository.ClicksResult.Success(
                items = listOf(
                    org.wordpress.android.ui.newstats.repository.ClickItemData(
                        name = "example.com",
                        clicks = TEST_VIEWS,
                        previousClicks = TEST_PREVIOUS_VIEWS
                    )
                ),
                totalClicks = TEST_TOTAL_VIEWS,
                totalClicksChange = TEST_TOTAL_VIEWS_CHANGE,
                totalClicksChangePercent = TEST_TOTAL_VIEWS_CHANGE_PERCENT
            )
        )

        viewModel.load(MostViewedDetailSource.CLICKS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as MostViewedDetailUiState.Loaded
        assertThat(loaded.items).hasSize(1)
        assertThat(loaded.items[0].title).isEqualTo("example.com")
        assertThat(loaded.totalViews).isEqualTo(TEST_TOTAL_VIEWS)
        verify(statsRepository, never()).fetchReferrersDetail(any(), any())
    }

    @Test
    fun `when no site is selected, then error state is emitted and no fetch happens`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedDetailUiState.Error::class.java)
        assertThat((state as MostViewedDetailUiState.Error).message).isEqualTo(API_ERROR)
        verify(statsRepository, never()).fetchReferrersDetail(any(), any())
    }

    @Test
    fun `when access token is empty, then error state is emitted and no fetch happens`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedDetailUiState.Error::class.java)
        assertThat((state as MostViewedDetailUiState.Error).message).isEqualTo(API_ERROR)
        verify(statsRepository, never()).fetchReferrersDetail(any(), any())
    }

    @Test
    fun `when the repository returns an error, then the api error state is emitted`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any()))
            .thenReturn(MostViewedResult.Error("Network error"))

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedDetailUiState.Error::class.java)
        assertThat((state as MostViewedDetailUiState.Error).message).isEqualTo(API_ERROR)
    }

    @Test
    fun `when the fetch returns an auth error, then the error state carries the auth flag`() = test {
        whenever(statsRepository.fetchClicks(any(), any())).thenReturn(
            org.wordpress.android.ui.newstats.repository.ClicksResult.Error(
                R.string.stats_error_api,
                isAuthError = true
            )
        )

        viewModel.load(MostViewedDetailSource.CLICKS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedDetailUiState.Error
        assertThat(state.message).isEqualTo(API_ERROR)
        assertThat(state.isAuthError).isTrue()
    }

    @Test
    fun `when getAdminUrl is called, then the selected site's admin url is returned`() {
        assertThat(viewModel.getAdminUrl()).isEqualTo(TEST_ADMIN_URL)
    }

    @Test
    fun `when the fetch throws, then the generic unknown error state is emitted`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any()))
            .thenThrow(RuntimeException(EXCEPTION_MESSAGE))

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedDetailUiState.Error::class.java)
        assertThat((state as MostViewedDetailUiState.Error).message).isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when load is called twice, then the data is fetched only once`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()
        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()

        verify(statsRepository, times(1)).fetchReferrersDetail(any(), any())
    }

    @Test
    fun `when retry is called after a load, then the data is fetched again`() = test {
        whenever(statsRepository.fetchReferrersDetail(any(), any())).thenReturn(createSuccessResult())

        viewModel.load(MostViewedDetailSource.REFERRERS, StatsPeriod.Last7Days)
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchReferrersDetail(any(), any())
    }

    private fun createSuccessResult() = MostViewedResult.Success(
        items = listOf(
            MostViewedItemData(
                id = 1,
                title = "Search Engines",
                views = TEST_VIEWS,
                previousViews = TEST_PREVIOUS_VIEWS,
                isFirst = true,
                children = listOf(
                    MostViewedChildData(name = "Google Search", url = CHILD_URL, views = TEST_VIEWS)
                )
            )
        ),
        totalViews = TEST_TOTAL_VIEWS,
        totalViewsChange = TEST_TOTAL_VIEWS_CHANGE,
        totalViewsChangePercent = TEST_TOTAL_VIEWS_CHANGE_PERCENT
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_ADMIN_URL = "https://example.com/wp-admin"
        private const val DATE_RANGE = "Last 7 days"
        private const val API_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Something went wrong"
        private const val EXCEPTION_MESSAGE = "boom"
        private const val CHILD_URL = "http://google.com/"

        private const val TEST_VIEWS = 23L
        private const val TEST_PREVIOUS_VIEWS = 10L
        private const val TEST_TOTAL_VIEWS = 23L
        private const val TEST_TOTAL_VIEWS_CHANGE = 13L
        private const val TEST_TOTAL_VIEWS_CHANGE_PERCENT = 130.0
    }
}
