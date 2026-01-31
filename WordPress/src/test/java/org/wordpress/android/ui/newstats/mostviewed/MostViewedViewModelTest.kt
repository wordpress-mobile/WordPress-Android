package org.wordpress.android.ui.newstats.mostviewed

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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.MostViewedItemData
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class MostViewedViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: MostViewedViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
    }

    private fun stubNoSiteSelectedError() {
        whenever(resourceProvider.getString(R.string.stats_todays_stats_no_site_selected))
            .thenReturn(NO_SITE_SELECTED_ERROR)
    }

    private fun stubFailedToLoadError() {
        whenever(resourceProvider.getString(R.string.stats_todays_stats_failed_to_load))
            .thenReturn(FAILED_TO_LOAD_ERROR)
    }

    private fun initViewModel() {
        viewModel = MostViewedViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
    }

    // region Error states
    @Test
    fun `when no site selected, then error state is emitted`() = test {
        stubNoSiteSelectedError()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedCardUiState.Error::class.java)
        assertThat((state as MostViewedCardUiState.Error).message).isEqualTo(NO_SITE_SELECTED_ERROR)
    }

    @Test
    fun `when access token is null, then error state is emitted`() = test {
        stubFailedToLoadError()
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedCardUiState.Error::class.java)
        assertThat((state as MostViewedCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when access token is empty, then error state is emitted`() = test {
        stubFailedToLoadError()
        whenever(accountStore.accessToken).thenReturn("")

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedCardUiState.Error::class.java)
        assertThat((state as MostViewedCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when fetch fails, then error state is emitted`() = test {
        stubFailedToLoadError()
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(MostViewedResult.Error("Network error"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedCardUiState.Error::class.java)
        assertThat((state as MostViewedCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when exception is thrown, then error state with exception message is emitted`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedCardUiState.Error::class.java)
        assertThat((state as MostViewedCardUiState.Error).message).isEqualTo("Test exception")
    }
    // endregion

    // region Success states
    @Test
    fun `when data loads successfully, then loaded state is emitted`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MostViewedCardUiState.Loaded::class.java)
    }

    @Test
    fun `when data loads, then items contain correct values`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items).hasSize(2)
        assertThat(state.items[0].title).isEqualTo(TEST_POST_TITLE_1)
        assertThat(state.items[0].views).isEqualTo(TEST_POST_VIEWS_1)
        assertThat(state.items[1].title).isEqualTo(TEST_POST_TITLE_2)
        assertThat(state.items[1].views).isEqualTo(TEST_POST_VIEWS_2)
    }

    @Test
    fun `when data loads, then first item is highlighted`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items[0].isHighlighted).isTrue()
        assertThat(state.items[1].isHighlighted).isFalse()
    }

    @Test
    fun `when data loads with more than 10 items, then only 10 are shown in card`() = test {
        val manyItems = (1..15).mapIndexed { idx, index ->
            MostViewedItemData(
                id = index.toLong(),
                title = "Post $index",
                views = (100 - index).toLong(),
                previousViews = (90 - index).toLong(),
                isFirst = idx == 0
            )
        }
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(
                MostViewedResult.Success(
                    items = manyItems,
                    totalViews = 1000,
                    totalViewsChange = 100,
                    totalViewsChangePercent = 10.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items).hasSize(10)
    }

    @Test
    fun `when data loads, then selected data source is correct`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.selectedDataSource).isEqualTo(MostViewedDataSource.POSTS_AND_PAGES)
    }
    // endregion

    // region Data source changes
    @Test
    fun `when data source changes, then data is reloaded`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onDataSourceChanged(MostViewedDataSource.REFERRERS)
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchMostViewed(any(), any(), any())
        verify(statsRepository).fetchMostViewed(
            eq(TEST_SITE_ID),
            any(),
            eq(MostViewedDataSource.REFERRERS)
        )
    }

    @Test
    fun `when same data source is selected, then data is not reloaded`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onDataSourceChanged(MostViewedDataSource.POSTS_AND_PAGES)
        advanceUntilIdle()

        // Should only be called once during init
        verify(statsRepository, times(1)).fetchMostViewed(any(), any(), any())
    }
    // endregion

    // region Period changes
    @Test
    fun `when period changes, then data is reloaded`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchMostViewed(any(), any(), any())
        verify(statsRepository).fetchMostViewed(eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days), any())
    }

    @Test
    fun `when same period is selected, then data is not reloaded`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        advanceUntilIdle()

        // Should only be called once during init
        verify(statsRepository, times(1)).fetchMostViewed(any(), any(), any())
    }
    // endregion

    // region Refresh
    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when refresh is called, then data is fetched`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // Called twice: once during init, once during refresh
        verify(statsRepository, times(2)).fetchMostViewed(eq(TEST_SITE_ID), any(), any())
    }
    // endregion

    // region Retry
    @Test
    fun `when onRetry is called, then data is reloaded`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        // Called twice: once during init, once during retry
        verify(statsRepository, times(2)).fetchMostViewed(any(), any(), any())
    }
    // endregion

    // region getDetailData
    @Test
    fun `when getDetailData is called, then returns cached data`() = test {
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(createSuccessResult())
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days))
            .thenReturn("Last 7 days")

        initViewModel()
        advanceUntilIdle()

        val detailData = viewModel.getDetailData()

        assertThat(detailData.dataSource).isEqualTo(MostViewedDataSource.POSTS_AND_PAGES)
        assertThat(detailData.items).hasSize(2)
        assertThat(detailData.totalViews).isEqualTo(TEST_TOTAL_VIEWS)
        assertThat(detailData.totalViewsChange).isEqualTo(TEST_TOTAL_VIEWS_CHANGE)
        assertThat(detailData.totalViewsChangePercent).isEqualTo(TEST_TOTAL_VIEWS_CHANGE_PERCENT)
    }

    @Test
    fun `when getDetailData is called, then all items are returned not just card items`() = test {
        val manyItems = (1..15).mapIndexed { idx, index ->
            MostViewedItemData(
                id = index.toLong(),
                title = "Post $index",
                views = (100 - index).toLong(),
                previousViews = (90 - index).toLong(),
                isFirst = idx == 0
            )
        }
        whenever(resourceProvider.getString(R.string.stats_period_last_7_days))
            .thenReturn("Last 7 days")
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(
                MostViewedResult.Success(
                    items = manyItems,
                    totalViews = 1000,
                    totalViewsChange = 100,
                    totalViewsChangePercent = 10.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val detailData = viewModel.getDetailData()
        // Card shows max 10, but detail data should have all 15
        assertThat(detailData.items).hasSize(15)
    }
    // endregion

    // region Change calculations
    @Test
    fun `when item has positive change, then MostViewedChange_Positive is returned`() = test {
        val items = listOf(
            MostViewedItemData(
                id = 1,
                title = "Post",
                views = 150,
                previousViews = 100,
                isFirst = true
            )
        )
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(
                MostViewedResult.Success(
                    items = items,
                    totalViews = 150,
                    totalViewsChange = 50,
                    totalViewsChangePercent = 50.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items[0].change).isInstanceOf(MostViewedChange.Positive::class.java)
        val change = state.items[0].change as MostViewedChange.Positive
        assertThat(change.value).isEqualTo(50)
        assertThat(change.percentage).isEqualTo(50.0)
    }

    @Test
    fun `when item has negative change, then MostViewedChange_Negative is returned`() = test {
        val items = listOf(
            MostViewedItemData(
                id = 1,
                title = "Post",
                views = 50,
                previousViews = 100,
                isFirst = true
            )
        )
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(
                MostViewedResult.Success(
                    items = items,
                    totalViews = 50,
                    totalViewsChange = -50,
                    totalViewsChangePercent = -50.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items[0].change).isInstanceOf(MostViewedChange.Negative::class.java)
        val change = state.items[0].change as MostViewedChange.Negative
        assertThat(change.value).isEqualTo(50)
        assertThat(change.percentage).isEqualTo(50.0)
    }

    @Test
    fun `when item has no change, then MostViewedChange_NoChange is returned`() = test {
        val items = listOf(
            MostViewedItemData(
                id = 1,
                title = "Post",
                views = 100,
                previousViews = 100,
                isFirst = true
            )
        )
        whenever(statsRepository.fetchMostViewed(any(), any(), any()))
            .thenReturn(
                MostViewedResult.Success(
                    items = items,
                    totalViews = 100,
                    totalViewsChange = 0,
                    totalViewsChangePercent = 0.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items[0].change).isEqualTo(MostViewedChange.NoChange)
    }
    // endregion

    // region Helper functions
    private fun createSuccessResult() = MostViewedResult.Success(
        items = listOf(
            MostViewedItemData(
                id = 1,
                title = TEST_POST_TITLE_1,
                views = TEST_POST_VIEWS_1,
                previousViews = TEST_POST_PREVIOUS_VIEWS_1,
                isFirst = true
            ),
            MostViewedItemData(
                id = 2,
                title = TEST_POST_TITLE_2,
                views = TEST_POST_VIEWS_2,
                previousViews = TEST_POST_PREVIOUS_VIEWS_2,
                isFirst = false
            )
        ),
        totalViews = TEST_TOTAL_VIEWS,
        totalViewsChange = TEST_TOTAL_VIEWS_CHANGE,
        totalViewsChangePercent = TEST_TOTAL_VIEWS_CHANGE_PERCENT
    )
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val NO_SITE_SELECTED_ERROR = "No site selected"
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"

        private const val TEST_POST_TITLE_1 = "Test Post 1"
        private const val TEST_POST_TITLE_2 = "Test Post 2"
        private const val TEST_POST_VIEWS_1 = 500L
        private const val TEST_POST_VIEWS_2 = 300L
        private const val TEST_POST_PREVIOUS_VIEWS_1 = 400L
        private const val TEST_POST_PREVIOUS_VIEWS_2 = 250L

        private const val TEST_TOTAL_VIEWS = 800L
        private const val TEST_TOTAL_VIEWS_CHANGE = 150L
        private const val TEST_TOTAL_VIEWS_CHANGE_PERCENT = 23.1
    }
}
