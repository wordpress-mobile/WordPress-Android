package org.wordpress.android.ui.newstats.viewsstats

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardsConfiguration
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker.NavigationDirection
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.repository.PeriodAggregates
import org.wordpress.android.ui.newstats.repository.PeriodStatsResult
import org.wordpress.android.ui.newstats.repository.StatsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.ViewsDataPoint
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate

/**
 * What gets reported for the Views card, and - mostly - what doesn't. Every interaction here can
 * fire on a tap that changes nothing on screen (re-picking the metric already charted, tapping a
 * selected bar to deselect it, a navigation arrow at the edge of the available range), and each of
 * those would inflate its event.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class ViewsStatsViewModelTrackingTest : BaseUnitTest() {
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

    @Mock
    private lateinit var newStatsTracker: NewStatsTracker

    private lateinit var viewModel: ViewsStatsViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn("test_access_token")
        whenever(resourceProvider.getString(any())).thenReturn("")
    }

    // region Date range

    @Test
    fun `picking a preset reports it`() = test {
        initViewModel()

        viewModel.onPresetSelected(StatsPeriod.ThisMonth)

        verify(newStatsTracker).trackDateRangePresetSelected(StatsPeriod.ThisMonth)
    }

    @Test
    fun `picking a custom range reports its dates`() = test {
        initViewModel()
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 9)

        viewModel.onCustomRangeSelected(start, end)

        verify(newStatsTracker).trackCustomDateRangeSelected(start, end)
    }

    @Test
    fun `paging back reports the range being left, not the one arrived at`() = test {
        whenever(statsRepository.canNavigateBackward(any())).thenReturn(true)
        whenever(statsRepository.previousPeriod(any(), any())).thenReturn(StatsPeriod.Last30Days)
        initViewModel()

        viewModel.onNavigatePrevious()

        verify(newStatsTracker).trackDateNavigationButtonTapped(
            NavigationDirection.PREVIOUS,
            StatsPeriod.Last7Days
        )
    }

    @Test
    fun `an arrow tap at the edge of the range reports nothing`() = test {
        whenever(statsRepository.canNavigateForward(any())).thenReturn(false)
        initViewModel()

        viewModel.onNavigateNext()

        verify(newStatsTracker, never()).trackDateNavigationButtonTapped(any(), any())
    }

    // endregion

    // region Chart

    @Test
    fun `changing the chart type reports both types`() = test {
        initViewModel()

        viewModel.onChartTypeChanged(ChartType.LINE)
        viewModel.onChartTypeChanged(ChartType.BAR)

        verify(newStatsTracker).trackChartTypeChanged(from = ChartType.LINE, to = ChartType.BAR)
    }

    @Test
    fun `re-picking the chart type already on screen reports nothing`() = test {
        initViewModel()
        viewModel.onChartTypeChanged(ChartType.LINE)

        viewModel.onChartTypeChanged(ChartType.LINE)

        verify(newStatsTracker, times(1)).trackChartTypeChanged(any(), any())
    }

    @Test
    fun `selecting a metric reports it once`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any(), any()))
            .thenReturn(periodStatsResult())
        initViewModel()

        viewModel.onMetricSelected(StatsMetric.VISITORS)
        viewModel.onMetricSelected(StatsMetric.VISITORS)

        verify(newStatsTracker, times(1)).trackChartMetricSelected(StatsMetric.VISITORS)
    }

    @Test
    fun `selecting a bar reports its bucket width, metric and total`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any(), any()))
            .thenReturn(periodStatsResult())
        initViewModel()
        viewModel.onChartTypeChanged(ChartType.BAR)

        viewModel.onBarTapped(0)
        advanceUntilIdle()

        verify(newStatsTracker).trackChartBarSelected(
            metric = StatsMetric.VIEWS,
            unit = StatsUnit.DAY,
            value = FIRST_BAR_VIEWS
        )
    }

    /** The same tap undone: reporting it would double every bar interaction. */
    @Test
    fun `tapping the selected bar again, which deselects it, reports nothing further`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any(), any()))
            .thenReturn(periodStatsResult())
        initViewModel()
        viewModel.onChartTypeChanged(ChartType.BAR)

        viewModel.onBarTapped(0)
        advanceUntilIdle()
        viewModel.onBarTapped(0)
        advanceUntilIdle()

        verify(newStatsTracker, times(1)).trackChartBarSelected(any(), any(), any())
    }

    @Test
    fun `opening the card reports nothing by itself`() = test {
        whenever(statsRepository.fetchStatsForPeriod(any(), any(), any()))
            .thenReturn(periodStatsResult())

        initViewModel()

        verifyNoInteractions(newStatsTracker)
    }

    // endregion

    private suspend fun initViewModel() {
        whenever(cardsConfigurationRepository.getConfiguration(eq(TEST_SITE_ID)))
            .thenReturn(StatsCardsConfiguration())
        viewModel = ViewsStatsViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider,
            SavedStateHandle(),
            cardsConfigurationRepository,
            newStatsTracker
        )
        viewModel.loadDataIfNeeded()
        advanceUntilIdle()
    }

    private fun periodStatsResult() = PeriodStatsResult.Success(
        currentAggregates = PeriodAggregates(
            views = 7000L,
            visitors = 700L,
            likes = 50L,
            comments = 25L,
            posts = 5L,
            startDate = "2024-01-14",
            endDate = "2024-01-20"
        ),
        previousAggregates = PeriodAggregates(
            views = 8000L,
            visitors = 800L,
            likes = 60L,
            comments = 30L,
            posts = 4L,
            startDate = "2024-01-07",
            endDate = "2024-01-13"
        ),
        currentPeriodData = dataPoints(),
        previousPeriodData = dataPoints(),
        unit = StatsUnit.DAY
    )

    private fun dataPoints() = listOf(
        ViewsDataPoint(period = "2024-01-14", views = FIRST_BAR_VIEWS),
        ViewsDataPoint(period = "2024-01-15", views = 1500L)
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val FIRST_BAR_VIEWS = 1000L
    }
}
