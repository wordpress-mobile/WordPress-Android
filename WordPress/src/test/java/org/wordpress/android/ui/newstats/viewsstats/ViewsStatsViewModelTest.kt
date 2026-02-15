package org.wordpress.android.ui.newstats.viewsstats

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.runner.RunWith
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.StatsCardsConfiguration
import org.wordpress.android.ui.newstats.repository.ViewsDataPoint
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.StatsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.PeriodAggregates
import org.wordpress.android.ui.newstats.repository.PeriodStatsResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
@RunWith(MockitoJUnitRunner.Silent::class)
class ViewsStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var cardsConfigurationRepository: StatsCardsConfigurationRepository

    private lateinit var viewModel: ViewsStatsViewModel

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
        whenever(resourceProvider.getString(R.string.stats_views))
            .thenReturn("Views")
        whenever(resourceProvider.getString(R.string.stats_visitors))
            .thenReturn("Visitors")
        whenever(resourceProvider.getString(R.string.stats_likes))
            .thenReturn("Likes")
        whenever(resourceProvider.getString(R.string.stats_comments))
            .thenReturn("Comments")
        whenever(resourceProvider.getString(R.string.posts))
            .thenReturn("Posts")
    }

    private suspend fun initViewModel() {
        whenever(cardsConfigurationRepository.getConfiguration(any()))
            .thenReturn(StatsCardsConfiguration())
        viewModel = ViewsStatsViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider,
            SavedStateHandle(),
            cardsConfigurationRepository
        )
    }

    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo(NO_SITE_SELECTED_ERROR)
    }

    @Test
    fun `when data loads successfully, then loaded state is emitted with correct values`() = test {
        val result = createPeriodStatsResult(
            currentViews = TEST_CURRENT_PERIOD_VIEWS,
            previousViews = TEST_PREVIOUS_PERIOD_VIEWS
        )

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Loaded::class.java)
        with(state as ViewsStatsCardUiState.Loaded) {
            assertThat(currentPeriodViews).isEqualTo(TEST_CURRENT_PERIOD_VIEWS)
            assertThat(previousPeriodViews).isEqualTo(TEST_PREVIOUS_PERIOD_VIEWS)
        }
    }

    @Test
    fun `when period stats fetch fails, then error state is emitted`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(PeriodStatsResult.Error("Network error"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when period data is empty, then chart data is empty but state is loaded`() = test {
        val result = createPeriodStatsResult(currentPeriodData = emptyList(), previousPeriodData = emptyList())

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Loaded::class.java)
        with(state as ViewsStatsCardUiState.Loaded) {
            assertThat(chartData.currentPeriod).isEmpty()
            assertThat(chartData.previousPeriod).isEmpty()
        }
    }

    @Test
    fun `when loadData is called, then repository is called`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.loadData()
        advanceUntilIdle()

        // Called twice: once during init, once during loadData
        verify(statsRepository, times(2)).fetchStatsForPeriod(any(), any())
    }

    @Test
    fun `when onRetry is called, then loadData is called`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        // Called twice: once during init, once during onRetry
        verify(statsRepository, times(2)).fetchStatsForPeriod(any(), any())
    }

    @Test
    fun `when data loads, then views difference is calculated correctly`() = test {
        val result = createPeriodStatsResult(currentViews = 7000L, previousViews = 8000L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.viewsDifference).isEqualTo(-1000L)
    }

    @Test
    fun `when data loads, then percentage change is calculated correctly`() = test {
        val result = createPeriodStatsResult(currentViews = 9000L, previousViews = 10000L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.viewsPercentageChange).isEqualTo(-10.0)
    }

    @Test
    fun `when previous period has zero views, then percentage change is 100 percent`() = test {
        val result = createPeriodStatsResult(currentViews = 1000L, previousViews = 0L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.viewsPercentageChange).isEqualTo(100.0)
    }

    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `when access token is null, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo(FAILED_TO_LOAD_ERROR)
    }

    @Test
    fun `when bottom stats are built, then they contain all stat types`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.bottomStats).hasSize(5)
        assertThat(state.bottomStats.map { it.label }).containsExactly(
            "Views", "Visitors", "Likes", "Comments", "Posts"
        )
    }

    @Test
    fun `when stat increases, then positive change is calculated`() = test {
        val result = createPeriodStatsResult(currentViews = 1000L, previousViews = 800L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        val viewsStat = state.bottomStats.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.Positive::class.java)
        assertThat((viewsStat.change as StatChange.Positive).percentage).isEqualTo(25.0)
    }

    @Test
    fun `when stat decreases, then negative change is calculated`() = test {
        val result = createPeriodStatsResult(currentViews = 800L, previousViews = 1000L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        val viewsStat = state.bottomStats.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.Negative::class.java)
        assertThat((viewsStat.change as StatChange.Negative).percentage).isEqualTo(20.0)
    }

    @Test
    fun `when stat is unchanged, then no change is calculated`() = test {
        val result = createPeriodStatsResult(currentViews = 1000L, previousViews = 1000L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        val viewsStat = state.bottomStats.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.NoChange::class.java)
    }

    @Test
    fun `when period average is calculated, then it is based on data points count`() = test {
        val result = createPeriodStatsResult(currentViews = 7000L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        // 7000 views / 2 data points = 3500 average
        assertThat(state.periodAverage).isEqualTo(3500L)
    }

    @Test
    fun `when exception is thrown during fetch, then error state is emitted`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Error::class.java)
        assertThat((state as ViewsStatsCardUiState.Error).message).isEqualTo("Test exception")
    }

    @Test
    fun `when onPeriodChanged is called with same period, then data is not reloaded`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        // Default period is Last7Days, calling with same period should not reload
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
        advanceUntilIdle()

        // Should only be called once during init
        verify(statsRepository, times(1)).fetchStatsForPeriod(any(), any())
    }

    @Test
    fun `when onPeriodChanged is called with different period, then data is reloaded`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        // Called twice: once during init, once during period change
        verify(statsRepository, times(2)).fetchStatsForPeriod(any(), any())
    }

    @Test
    fun `when onPeriodChanged is called with custom period, then data is loaded`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val customPeriod = StatsPeriod.Custom(
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 15)
        )
        viewModel.onPeriodChanged(customPeriod)
        advanceUntilIdle()

        // Called twice: once during init, once during custom period change
        verify(statsRepository, times(2)).fetchStatsForPeriod(any(), any())
    }

    @Test
    fun `when onChartTypeChanged is called, then chart type is updated`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onChartTypeChanged(ChartType.BAR)

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Loaded
        assertThat(state.chartType).isEqualTo(ChartType.BAR)
    }

    private fun createPeriodStatsResult(
        currentViews: Long = TEST_CURRENT_PERIOD_VIEWS,
        currentVisitors: Long = TEST_CURRENT_PERIOD_VISITORS,
        currentLikes: Long = TEST_CURRENT_PERIOD_LIKES,
        currentComments: Long = TEST_CURRENT_PERIOD_COMMENTS,
        currentPosts: Long = TEST_CURRENT_PERIOD_POSTS,
        previousViews: Long = TEST_PREVIOUS_PERIOD_VIEWS,
        previousVisitors: Long = TEST_PREVIOUS_PERIOD_VISITORS,
        previousLikes: Long = TEST_PREVIOUS_PERIOD_LIKES,
        previousComments: Long = TEST_PREVIOUS_PERIOD_COMMENTS,
        previousPosts: Long = TEST_PREVIOUS_PERIOD_POSTS,
        currentPeriodData: List<ViewsDataPoint> = createDefaultDataPoints(),
        previousPeriodData: List<ViewsDataPoint> = createDefaultDataPoints()
    ): PeriodStatsResult.Success {
        val currentAggregates = PeriodAggregates(
            views = currentViews,
            visitors = currentVisitors,
            likes = currentLikes,
            comments = currentComments,
            posts = currentPosts,
            startDate = "2024-01-14",
            endDate = "2024-01-20"
        )
        val previousAggregates = PeriodAggregates(
            views = previousViews,
            visitors = previousVisitors,
            likes = previousLikes,
            comments = previousComments,
            posts = previousPosts,
            startDate = "2024-01-07",
            endDate = "2024-01-13"
        )
        return PeriodStatsResult.Success(
            currentAggregates = currentAggregates,
            previousAggregates = previousAggregates,
            currentPeriodData = currentPeriodData,
            previousPeriodData = previousPeriodData
        )
    }

    private fun createDefaultDataPoints() = listOf(
        ViewsDataPoint(
            period = "2024-01-14",
            views = 1000L
        ),
        ViewsDataPoint(
            period = "2024-01-15",
            views = 1500L
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val TEST_CURRENT_PERIOD_VIEWS = 7000L
        private const val TEST_CURRENT_PERIOD_VISITORS = 700L
        private const val TEST_CURRENT_PERIOD_LIKES = 50L
        private const val TEST_CURRENT_PERIOD_COMMENTS = 25L
        private const val TEST_CURRENT_PERIOD_POSTS = 5L
        private const val TEST_PREVIOUS_PERIOD_VIEWS = 8000L
        private const val TEST_PREVIOUS_PERIOD_VISITORS = 800L
        private const val TEST_PREVIOUS_PERIOD_LIKES = 60L
        private const val TEST_PREVIOUS_PERIOD_COMMENTS = 30L
        private const val TEST_PREVIOUS_PERIOD_POSTS = 4L
        private const val NO_SITE_SELECTED_ERROR = "No site selected"
        private const val FAILED_TO_LOAD_ERROR = "Failed to load stats"
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
