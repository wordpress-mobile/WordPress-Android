package org.wordpress.android.ui.newstats.todaysstat

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
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class TodaysStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var todayInsightsStore: TodayInsightsStore

    @Mock
    private lateinit var visitsAndViewsStore: VisitsAndViewsStore

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: TodaysStatsViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_no_site_selected))
            .thenReturn(NO_SITE_SELECTED_ERROR)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_failed_to_load))
            .thenReturn(FAILED_TO_LOAD_ERROR)
        whenever(resourceProvider.getString(R.string.stats_todays_stats_unknown_error))
            .thenReturn(UNKNOWN_ERROR)
    }

    private fun initViewModel() {
        viewModel = TodaysStatsViewModel(
            selectedSiteRepository,
            todayInsightsStore,
            visitsAndViewsStore,
            resourceProvider
        )
    }

    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo(NO_SITE_SELECTED_ERROR)
    }

    @Test
    fun `when data loads successfully, then loaded state is emitted with correct values`() = test {
        val visitsModel = VisitsModel(
            period = "2024-01-16",
            views = TEST_VIEWS,
            visitors = TEST_VISITORS,
            likes = TEST_LIKES,
            reblogs = 0,
            comments = TEST_COMMENTS,
            posts = 0
        )
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
        with(state as TodaysStatsCardUiState.Loaded) {
            assertThat(views).isEqualTo(TEST_VIEWS)
            assertThat(visitors).isEqualTo(TEST_VISITORS)
            assertThat(likes).isEqualTo(TEST_LIKES)
            assertThat(comments).isEqualTo(TEST_COMMENTS)
        }
    }

    @Test
    fun `when today insights fetch fails, then error state is emitted`() = test {
        val error = StatsError(StatsErrorType.GENERIC_ERROR, "Network error")

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(error))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(createVisitsAndViewsModel()))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when today insights returns null model, then error state is emitted`() = test {
        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(model = null))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(createVisitsAndViewsModel()))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
    }

    @Test
    fun `when visits and views fetch fails, then chart data is empty but state is loaded`() = test {
        val visitsModel = createVisitsModel()
        val error = StatsError(StatsErrorType.GENERIC_ERROR, "Network error")

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(error))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
        with(state as TodaysStatsCardUiState.Loaded) {
            assertThat(chartData.currentPeriod).isEmpty()
            assertThat(chartData.previousPeriod).isEmpty()
        }
    }

    @Test
    fun `when loadData is called with forced true, then stores are called with forced true`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        viewModel.loadData(forced = true)
        advanceUntilIdle()

        verify(todayInsightsStore).fetchTodayInsights(eq(testSite), eq(true))
    }

    @Test
    fun `when onRetry is called, then loadData is called with forced true`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        verify(todayInsightsStore).fetchTodayInsights(eq(testSite), eq(true))
    }

    @Test
    fun `when data loads, then chart data contains current and previous period data`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
        with(state as TodaysStatsCardUiState.Loaded) {
            assertThat(chartData.currentPeriod).hasSize(2)
            assertThat(chartData.previousPeriod).hasSize(2)
        }
    }

    @Test
    fun `when fetch visits is called, then hourly granularity is used for both periods`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        // fetchVisits is called twice: once for current period, once for previous period
        verify(visitsAndViewsStore, times(2)).fetchVisits(
            site = eq(testSite),
            granularity = eq(StatsGranularity.HOURS),
            limitMode = any<LimitMode.Top>(),
            date = any(),
            forced = any(),
            applySiteTimezone = any()
        )
    }

    @Test
    fun `when exception is thrown during fetch, then error state is emitted with exception message`() = test {
        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo("Test exception")
    }

    @Test
    fun `when exception with null message is thrown, then error state has unknown error message`() = test {
        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenThrow(RuntimeException())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when loadData is called again, then state transitions through loading`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        // Verify we're in Loaded state first
        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)

        // Call loadData again and verify the final state is still Loaded
        viewModel.loadData(forced = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
    }

    @Test
    fun `when error state retry is clicked, then data is reloaded`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val errorState = viewModel.uiState.value as TodaysStatsCardUiState.Error

        // Now set up for successful reload
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(createVisitsModel()))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(createVisitsAndViewsModel()))

        errorState.onRetry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
    }

    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        // Verify initial state is not refreshing
        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        // After refresh completes, isRefreshing should be false
        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when refresh is called, then data is fetched with forced true`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // Verify that fetchTodayInsights was called with forced = true during refresh
        // (called twice: once during init, once during refresh)
        verify(todayInsightsStore, times(2)).fetchTodayInsights(eq(testSite), any())
        verify(todayInsightsStore).fetchTodayInsights(eq(testSite), eq(true))
    }

    @Test
    fun `when refresh is called, then state remains loaded without showing loading state`() = test {
        val visitsModel = createVisitsModel()
        val visitsAndViewsModel = createVisitsAndViewsModel()

        whenever(todayInsightsStore.fetchTodayInsights(any(), any()))
            .thenReturn(OnStatsFetched(visitsModel))
        whenever(visitsAndViewsStore.fetchVisits(any(), any(), any(), any(), any(), any()))
            .thenReturn(OnStatsFetched(visitsAndViewsModel))

        initViewModel()
        advanceUntilIdle()

        // Verify initial state is Loaded
        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)

        viewModel.refresh()
        advanceUntilIdle()

        // State should still be Loaded after refresh (not showing Loading state)
        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
    }

    private fun createVisitsModel() = VisitsModel(
        period = "2024-01-16",
        views = TEST_VIEWS,
        visitors = TEST_VISITORS,
        likes = TEST_LIKES,
        reblogs = 0,
        comments = TEST_COMMENTS,
        posts = 0
    )

    private fun createVisitsAndViewsModel() = VisitsAndViewsModel(
        period = "hour",
        dates = listOf(
            VisitsAndViewsModel.PeriodData(
                period = "2024-01-16 14:00:00",
                views = 100L,
                visitors = 50L,
                likes = 10L,
                reblogs = 0L,
                comments = 5L,
                posts = 0L
            ),
            VisitsAndViewsModel.PeriodData(
                period = "2024-01-16 15:00:00",
                views = 150L,
                visitors = 75L,
                likes = 15L,
                reblogs = 0L,
                comments = 8L,
                posts = 0L
            )
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_VIEWS = 500
        private const val TEST_VISITORS = 100
        private const val TEST_LIKES = 50
        private const val TEST_COMMENTS = 25
        private const val NO_SITE_SELECTED_ERROR = "No site selected"
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
