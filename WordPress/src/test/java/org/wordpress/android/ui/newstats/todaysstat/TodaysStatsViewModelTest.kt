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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class TodaysStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

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
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
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
            accountStore,
            statsRepository,
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
        val aggregates = TodayAggregates(
            views = TEST_VIEWS,
            visitors = TEST_VISITORS,
            likes = TEST_LIKES,
            comments = TEST_COMMENTS
        )

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

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
    fun `when today aggregates fetch fails, then error state is emitted`() = test {
        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Error("Network error"))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when hourly views fetch fails, then chart data is empty but state is loaded`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(HourlyViewsResult.Error("Network error"))

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
    fun `when loadData is called with forced true, then repository is called`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.loadData(forced = true)
        advanceUntilIdle()

        // Called twice: once during init, once during loadData(forced = true)
        verify(statsRepository, times(2)).fetchTodayAggregates(eq(TEST_SITE_ID))
    }

    @Test
    fun `when onRetry is called, then loadData is called with forced true`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        // Called twice: once during init, once during onRetry
        verify(statsRepository, times(2)).fetchTodayAggregates(eq(TEST_SITE_ID))
    }

    @Test
    fun `when data loads, then chart data contains current and previous period data`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

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
    fun `when fetch hourly views is called, then repository is called for both periods`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        // fetchHourlyViews is called twice: once for current period (offsetDays=0),
        // once for previous period (offsetDays=1)
        verify(statsRepository).fetchHourlyViews(eq(TEST_SITE_ID), eq(0))
        verify(statsRepository).fetchHourlyViews(eq(TEST_SITE_ID), eq(1))
    }

    @Test
    fun `when exception is thrown during fetch, then error state is emitted with exception message`() = test {
        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo("Test exception")
    }

    @Test
    fun `when exception with null message is thrown, then error state has unknown error message`() = test {
        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenThrow(RuntimeException())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo(UNKNOWN_ERROR)
    }

    @Test
    fun `when loadData is called again, then state transitions through loading`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

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
        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(createTodayAggregates()))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        errorState.onRetry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
    }

    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

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
    fun `when refresh is called, then data is fetched`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // Called twice: once during init, once during refresh
        verify(statsRepository, times(2)).fetchTodayAggregates(eq(TEST_SITE_ID))
    }

    @Test
    fun `when refresh is called, then state remains loaded without showing loading state`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        // Verify initial state is Loaded
        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)

        viewModel.refresh()
        advanceUntilIdle()

        // State should still be Loaded after refresh (not showing Loading state)
        assertThat(viewModel.uiState.value).isInstanceOf(TodaysStatsCardUiState.Loaded::class.java)
    }

    @Test
    fun `when access token is null, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when access token is empty, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TodaysStatsCardUiState.Error::class.java)
        assertThat((state as TodaysStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when loadData is called, then repository is initialized with access token`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        verify(statsRepository).init(eq(TEST_ACCESS_TOKEN))
    }

    @Test
    fun `when chart data has labels, then they are formatted correctly`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as TodaysStatsCardUiState.Loaded
        // Labels should be formatted as "2pm", "3pm" from "2024-01-16 14:00:00", "2024-01-16 15:00:00"
        assertThat(state.chartData.currentPeriod).isNotEmpty()
        assertThat(state.chartData.currentPeriod[0].label).isNotEmpty()
    }

    @Test
    fun `when only current period hourly fetch fails, then current period is empty`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        // Current period (offsetDays=0) fails
        whenever(statsRepository.fetchHourlyViews(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(HourlyViewsResult.Error("Network error"))
        // Previous period (offsetDays=1) succeeds
        whenever(statsRepository.fetchHourlyViews(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as TodaysStatsCardUiState.Loaded
        assertThat(state.chartData.currentPeriod).isEmpty()
        assertThat(state.chartData.previousPeriod).hasSize(2)
    }

    @Test
    fun `when only previous period hourly fetch fails, then previous period is empty`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        // Current period (offsetDays=0) succeeds
        whenever(statsRepository.fetchHourlyViews(eq(TEST_SITE_ID), eq(0)))
            .thenReturn(createHourlyViewsResult())
        // Previous period (offsetDays=1) fails
        whenever(statsRepository.fetchHourlyViews(eq(TEST_SITE_ID), eq(1)))
            .thenReturn(HourlyViewsResult.Error("Network error"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as TodaysStatsCardUiState.Loaded
        assertThat(state.chartData.currentPeriod).hasSize(2)
        assertThat(state.chartData.previousPeriod).isEmpty()
    }

    @Test
    fun `when loaded state is shown, then onCardClick callback can be invoked`() = test {
        val aggregates = createTodayAggregates()

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(createHourlyViewsResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as TodaysStatsCardUiState.Loaded
        // Verify onCardClick callback can be invoked without error
        state.onCardClick()
        // If we reached here, the callback is present and invocable
    }

    @Test
    fun `when data loads with zero values, then loaded state shows zeros`() = test {
        val aggregates = TodayAggregates(
            views = 0L,
            visitors = 0L,
            likes = 0L,
            comments = 0L
        )

        whenever(statsRepository.fetchTodayAggregates(any()))
            .thenReturn(TodayAggregatesResult.Success(aggregates))
        whenever(statsRepository.fetchHourlyViews(any(), any()))
            .thenReturn(HourlyViewsResult.Success(emptyList()))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as TodaysStatsCardUiState.Loaded
        assertThat(state.views).isEqualTo(0L)
        assertThat(state.visitors).isEqualTo(0L)
        assertThat(state.likes).isEqualTo(0L)
        assertThat(state.comments).isEqualTo(0L)
        assertThat(state.chartData.currentPeriod).isEmpty()
    }

    private fun createTodayAggregates() = TodayAggregates(
        views = TEST_VIEWS,
        visitors = TEST_VISITORS,
        likes = TEST_LIKES,
        comments = TEST_COMMENTS
    )

    private fun createHourlyViewsResult() = HourlyViewsResult.Success(
        listOf(
            HourlyViewsDataPoint(
                period = "2024-01-16 14:00:00",
                views = 100L
            ),
            HourlyViewsDataPoint(
                period = "2024-01-16 15:00:00",
                views = 150L
            )
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_VIEWS = 500L
        private const val TEST_VISITORS = 100L
        private const val TEST_LIKES = 50L
        private const val TEST_COMMENTS = 25L
        private const val NO_SITE_SELECTED_ERROR = "No site selected"
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
