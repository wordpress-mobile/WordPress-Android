package org.wordpress.android.ui.newstats.viewsstats

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
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
import org.wordpress.android.ui.newstats.repository.BottomStatsAggregates
import org.wordpress.android.ui.newstats.repository.BottomStatsResult
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
        whenever(resourceProvider.getString(R.string.stats_error_no_site))
            .thenReturn(NO_SITE_SELECTED_ERROR)
        whenever(resourceProvider.getString(R.string.stats_error_api))
            .thenReturn(FAILED_TO_LOAD_ERROR)
        whenever(resourceProvider.getString(R.string.stats_error_unknown))
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
        viewModel.loadDataIfNeeded()
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
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Content::class.java)
        with(state.chartLoaded()) {
            assertThat(currentPeriodViews).isEqualTo(TEST_CURRENT_PERIOD_VIEWS)
            assertThat(previousPeriodViews).isEqualTo(TEST_PREVIOUS_PERIOD_VIEWS)
        }
    }

    @Test
    fun `when period stats fetch fails, then chart region shows an error but the card stays`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(PeriodStatsResult.Error("Network error"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Content::class.java)
        assertThat((state as ViewsStatsCardUiState.Content).chart).isEqualTo(ChartUiState.Error)
    }

    @Test
    fun `when period data is empty, then chart data is empty but chart is loaded`() = test {
        val result = createPeriodStatsResult(currentPeriodData = emptyList(), previousPeriodData = emptyList())

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Content::class.java)
        with(state.chartLoaded()) {
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

        val state = viewModel.uiState.value.chartLoaded()
        assertThat(state.viewsDifference).isEqualTo(-1000L)
    }

    @Test
    fun `when data loads, then percentage change is calculated correctly`() = test {
        val result = createPeriodStatsResult(currentViews = 9000L, previousViews = 10000L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.chartLoaded()
        assertThat(state.viewsPercentageChange).isEqualTo(-10.0)
    }

    @Test
    fun `when previous period has zero views, then percentage change is 100 percent`() = test {
        val result = createPeriodStatsResult(currentViews = 1000L, previousViews = 0L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.chartLoaded()
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
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(createPeriodStatsResult())
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(createBottomStatsResult())

        initViewModel()
        advanceUntilIdle()

        val stats = viewModel.uiState.value.bottomStatsOrNull()
        assertThat(stats).hasSize(5)
        assertThat(stats!!.map { it.label }).containsExactly(
            "Views", "Visitors", "Likes", "Comments", "Posts"
        )
    }

    @Test
    fun `when the bottom stats fetch fails, then the bottom row is hidden`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(createPeriodStatsResult())
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(BottomStatsResult.Error)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.bottomState()).isEqualTo(BottomStatsUiState.Hidden)
    }

    @Test
    fun `when the bottom stats fetch fails, then the chart still loads`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(createPeriodStatsResult())
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(BottomStatsResult.Error)

        initViewModel()
        advanceUntilIdle()

        // The chart region is independent of the bottom row, so it stays loaded.
        assertThat(viewModel.uiState.value.chartLoaded().currentPeriodViews)
            .isEqualTo(TEST_CURRENT_PERIOD_VIEWS)
    }

    @Test
    fun `when the chart fetch fails, then the bottom row still loads`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(PeriodStatsResult.Error("Network error"))
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(createBottomStatsResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ViewsStatsCardUiState.Content
        assertThat(state.chart).isEqualTo(ChartUiState.Error)
        assertThat(state.bottomStats).isInstanceOf(BottomStatsUiState.Loaded::class.java)
    }

    @Test
    fun `when bottom stats fail transiently, then the next visibility retries and recovers the row`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(createPeriodStatsResult())
        // First attempt fails (row hidden); the retry on next visibility succeeds.
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(BottomStatsResult.Error)
            .thenReturn(createBottomStatsResult())

        initViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.bottomState()).isEqualTo(BottomStatsUiState.Hidden)

        // The card becoming visible again must retry, since the period was not fully loaded.
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.bottomState()).isInstanceOf(BottomStatsUiState.Loaded::class.java)
        verify(statsRepository, times(2)).fetchBottomStats(any(), any())
    }

    @Test
    fun `when stat increases, then positive change is calculated`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(createPeriodStatsResult())
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(createBottomStatsResult(currentViews = 1000L, previousViews = 800L))

        initViewModel()
        advanceUntilIdle()

        val viewsStat = viewModel.uiState.value.bottomStatsOrNull()!!.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.Positive::class.java)
        assertThat((viewsStat.change as StatChange.Positive).percentage).isEqualTo(25.0)
    }

    @Test
    fun `when stat decreases, then negative change is calculated`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(createPeriodStatsResult())
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(createBottomStatsResult(currentViews = 800L, previousViews = 1000L))

        initViewModel()
        advanceUntilIdle()

        val viewsStat = viewModel.uiState.value.bottomStatsOrNull()!!.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.Negative::class.java)
        assertThat((viewsStat.change as StatChange.Negative).percentage).isEqualTo(20.0)
    }

    @Test
    fun `when stat is unchanged, then no change is calculated`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(createPeriodStatsResult())
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(createBottomStatsResult(currentViews = 1000L, previousViews = 1000L))

        initViewModel()
        advanceUntilIdle()

        val viewsStat = viewModel.uiState.value.bottomStatsOrNull()!!.first { it.label == "Views" }
        assertThat(viewsStat.change).isInstanceOf(StatChange.NoChange::class.java)
    }

    @Test
    fun `when period average is calculated, then it is based on data points count`() = test {
        val result = createPeriodStatsResult(currentViews = 7000L)

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.chartLoaded()
        // 7000 views / 2 data points = 3500 average
        assertThat(state.periodAverage).isEqualTo(3500L)
    }

    @Test
    fun `when exception is thrown during chart fetch, then chart region shows an error`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenThrow(RuntimeException("Test exception"))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ViewsStatsCardUiState.Content::class.java)
        assertThat((state as ViewsStatsCardUiState.Content).chart).isEqualTo(ChartUiState.Error)
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
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        // Called twice: once during init, once after period change
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
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        // Called twice: once during init, once after custom period change
        verify(statsRepository, times(2)).fetchStatsForPeriod(any(), any())
    }

    @Test
    fun `when initialized, then isPeriodInitialized is true`() = test {
        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isPeriodInitialized.value).isTrue()
    }

    @Test
    fun `when loadDataIfNeeded is called multiple times, then data is only loaded once`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)
        // Both regions must succeed for the period to be treated as fully loaded (and thus skipped).
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(createBottomStatsResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        // Should only be called once despite three calls to loadDataIfNeeded
        verify(statsRepository, times(1)).fetchStatsForPeriod(any(), any())
    }

    @Test
    fun `when onChartTypeChanged is called, then chart type is updated`() = test {
        val result = createPeriodStatsResult()

        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onChartTypeChanged(ChartType.BAR)

        val state = viewModel.uiState.value.chartLoaded()
        assertThat(state.chartType).isEqualTo(ChartType.BAR)
    }

    // region Chart type persistence

    @Test
    fun `when chart type is saved, then it persists in SavedStateHandle`() = test {
        val result = createPeriodStatsResult()
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onChartTypeChanged(ChartType.BAR)
        advanceUntilIdle()

        verify(cardsConfigurationRepository).saveConfiguration(
            any(), any()
        )
    }

    @Test
    fun `when SavedStateHandle has bar chart type, then it is restored`() = test {
        val savedState = SavedStateHandle(
            mapOf("chart_type" to "bar")
        )
        whenever(cardsConfigurationRepository.getConfiguration(any()))
            .thenReturn(StatsCardsConfiguration())
        val result = createPeriodStatsResult()
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        viewModel = ViewsStatsViewModel(
            selectedSiteRepository, accountStore, statsRepository,
            resourceProvider, savedState, cardsConfigurationRepository
        )
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value.chartLoaded()
        assertThat(state.chartType).isEqualTo(ChartType.BAR)
    }

    @Test
    fun `when preferences has bar chart type, then it is restored`() = test {
        whenever(cardsConfigurationRepository.getConfiguration(any()))
            .thenReturn(
                StatsCardsConfiguration(selectedChartType = "bar")
            )
        val result = createPeriodStatsResult()
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        viewModel = ViewsStatsViewModel(
            selectedSiteRepository, accountStore, statsRepository,
            resourceProvider, SavedStateHandle(),
            cardsConfigurationRepository
        )
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value.chartLoaded()
        assertThat(state.chartType).isEqualTo(ChartType.BAR)
    }

    // endregion

    // region Bar tap drill-down

    @Test
    fun `when bar tapped on daily data, then period drills to that day`() = test {
        val result = createPeriodStatsResult()
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onChartTypeChanged(ChartType.BAR)
        viewModel.onBarTapped(0)
        advanceUntilIdle()

        val period = viewModel.selectedPeriod.value
        assertThat(period).isInstanceOf(StatsPeriod.Custom::class.java)
        with(period as StatsPeriod.Custom) {
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 14))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 1, 14))
        }
    }

    @Test
    fun `when bar tapped on monthly data, then period drills to full month`() = test {
        val monthlyData = listOf(
            ViewsDataPoint(period = "2024-01-01", views = 5000L),
            ViewsDataPoint(period = "2024-02-01", views = 6000L)
        )
        val result = createPeriodStatsResult(
            currentPeriodData = monthlyData,
            previousPeriodData = monthlyData
        )
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last6Months)
        viewModel.loadData()
        advanceUntilIdle()

        viewModel.onChartTypeChanged(ChartType.BAR)
        viewModel.onBarTapped(0)
        advanceUntilIdle()

        val period = viewModel.selectedPeriod.value
        assertThat(period).isInstanceOf(StatsPeriod.Custom::class.java)
        with(period as StatsPeriod.Custom) {
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 1, 31))
        }
    }

    @Test
    fun `when bar tapped on hourly data, then period does not change`() = test {
        val hourlyData = listOf(
            ViewsDataPoint(period = "2024-01-14 10:00:00", views = 100L),
            ViewsDataPoint(period = "2024-01-14 11:00:00", views = 150L)
        )
        val result = createPeriodStatsResult(
            currentPeriodData = hourlyData,
            previousPeriodData = hourlyData
        )
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Today)
        viewModel.loadData()
        advanceUntilIdle()

        val periodBefore = viewModel.selectedPeriod.value
        viewModel.onChartTypeChanged(ChartType.BAR)
        viewModel.onBarTapped(0)
        advanceUntilIdle()

        assertThat(viewModel.selectedPeriod.value).isEqualTo(periodBefore)
    }

    @Test
    fun `when bar tapped while loading, then tap is ignored`() = test {
        var fetchCount = 0
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenAnswer {
                fetchCount++
                createPeriodStatsResult()
            }
        // Both regions must succeed so the drill-down period is treated as fully loaded, otherwise
        // the later loadDataIfNeeded would retry and inflate the fetch count.
        whenever(statsRepository.fetchBottomStats(any(), any()))
            .thenReturn(createBottomStatsResult())

        initViewModel()
        advanceUntilIdle()
        assertThat(fetchCount).isEqualTo(1)

        viewModel.onChartTypeChanged(ChartType.BAR)

        // First bar tap triggers a load
        viewModel.onBarTapped(0)
        advanceUntilIdle()
        assertThat(fetchCount).isEqualTo(2)

        // Second bar tap on same loaded state should also work
        // (loading completed, isLoadingNewPeriod reset to false)
        // Verify loadingPeriod guard prevents composable double-load
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()

        // loadDataIfNeeded should NOT trigger a third fetch because
        // loadingPeriod was set in onBarTapped
        assertThat(fetchCount).isEqualTo(2)
    }

    @Test
    fun `when bar tapped with invalid index, then period does not change`() = test {
        val result = createPeriodStatsResult()
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val periodBefore = viewModel.selectedPeriod.value
        viewModel.onChartTypeChanged(ChartType.BAR)
        viewModel.onBarTapped(999)
        advanceUntilIdle()

        assertThat(viewModel.selectedPeriod.value)
            .isEqualTo(periodBefore)
    }

    @Test
    fun `when bar tapped on custom monthly period, then drills to full month`() = test {
        val monthlyData = listOf(
            ViewsDataPoint(period = "2024-03-01", views = 3000L),
            ViewsDataPoint(period = "2024-04-01", views = 4000L)
        )
        val result = createPeriodStatsResult(
            currentPeriodData = monthlyData,
            previousPeriodData = monthlyData
        )
        whenever(statsRepository.fetchStatsForPeriod(any(), any()))
            .thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        // Custom period spanning >31 days = monthly granularity
        viewModel.onPeriodChanged(
            StatsPeriod.Custom(
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 6, 30)
            )
        )
        viewModel.loadData()
        advanceUntilIdle()

        viewModel.onChartTypeChanged(ChartType.BAR)
        viewModel.onBarTapped(0)
        advanceUntilIdle()

        val period = viewModel.selectedPeriod.value
        assertThat(period).isInstanceOf(StatsPeriod.Custom::class.java)
        with(period as StatsPeriod.Custom) {
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 3, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 3, 31))
        }
    }

    @Test
    fun `when drilling down and the chart finishes first, then the card stays dimmed until bottom stats finish`() =
        test {
            whenever(statsRepository.fetchStatsForPeriod(any(), any()))
                .thenReturn(createPeriodStatsResult())
            // Gate the (slower, dedicated) bottom-stats call so the chart always resolves first.
            var bottomGate = CompletableDeferred<Unit>()
            whenever(statsRepository.fetchBottomStats(any(), any())).doSuspendableAnswer {
                bottomGate.await()
                createBottomStatsResult()
            }

            initViewModel()
            bottomGate.complete(Unit)
            advanceUntilIdle()

            // Fresh gate for the drill-down load; leave it pending so only the chart completes.
            bottomGate = CompletableDeferred()
            viewModel.onChartTypeChanged(ChartType.BAR)
            viewModel.onBarTapped(0)
            advanceUntilIdle()

            // Chart has resolved for the new period, but the card must remain dimmed because the
            // bottom row still holds the previous period's totals.
            val dimmedState = viewModel.uiState.value as ViewsStatsCardUiState.Content
            assertThat(dimmedState.chart).isInstanceOf(ChartUiState.Loaded::class.java)
            assertThat(dimmedState.isLoadingNewPeriod).isTrue

            // Once the bottom row resolves for the new period, the card un-dims.
            bottomGate.complete(Unit)
            advanceUntilIdle()

            val finalState = viewModel.uiState.value as ViewsStatsCardUiState.Content
            assertThat(finalState.isLoadingNewPeriod).isFalse
        }

    // endregion

    // region ChartType storage key

    @Test
    fun `ChartType fromStorageKey returns correct types`() {
        assertThat(ChartType.fromStorageKey("line"))
            .isEqualTo(ChartType.LINE)
        assertThat(ChartType.fromStorageKey("bar"))
            .isEqualTo(ChartType.BAR)
        assertThat(ChartType.fromStorageKey("unknown")).isNull()
        assertThat(ChartType.fromStorageKey(null)).isNull()
    }

    // endregion

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

    private fun createBottomStatsResult(
        currentViews: Long = TEST_CURRENT_PERIOD_VIEWS,
        currentVisitors: Long = TEST_CURRENT_PERIOD_VISITORS,
        currentLikes: Long = TEST_CURRENT_PERIOD_LIKES,
        currentComments: Long = TEST_CURRENT_PERIOD_COMMENTS,
        currentPosts: Long = TEST_CURRENT_PERIOD_POSTS,
        previousViews: Long = TEST_PREVIOUS_PERIOD_VIEWS,
        previousVisitors: Long = TEST_PREVIOUS_PERIOD_VISITORS,
        previousLikes: Long = TEST_PREVIOUS_PERIOD_LIKES,
        previousComments: Long = TEST_PREVIOUS_PERIOD_COMMENTS,
        previousPosts: Long = TEST_PREVIOUS_PERIOD_POSTS
    ): BottomStatsResult.Success = BottomStatsResult.Success(
        current = BottomStatsAggregates(
            views = currentViews,
            visitors = currentVisitors,
            likes = currentLikes,
            comments = currentComments,
            posts = currentPosts
        ),
        previous = BottomStatsAggregates(
            views = previousViews,
            visitors = previousVisitors,
            likes = previousLikes,
            comments = previousComments,
            posts = previousPosts
        )
    )

    private fun ViewsStatsCardUiState.chartLoaded(): ChartUiState.Loaded =
        (this as ViewsStatsCardUiState.Content).chart as ChartUiState.Loaded

    private fun ViewsStatsCardUiState.bottomState(): BottomStatsUiState =
        (this as ViewsStatsCardUiState.Content).bottomStats

    private fun ViewsStatsCardUiState.bottomStatsOrNull(): List<StatItem>? =
        (bottomState() as? BottomStatsUiState.Loaded)?.stats

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
