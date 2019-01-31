package org.wordpress.android.ui.stats.refresh

import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_DAYS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_MONTHS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_WEEKS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_YEARS_ACCESSED
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class StatsViewModelTest : BaseUnitTest() {
    @Mock lateinit var insightsUseCase: BaseListUseCase
    @Mock lateinit var dayStatsUseCase: BaseListUseCase
    @Mock lateinit var weekStatsUseCase: BaseListUseCase
    @Mock lateinit var monthStatsUseCase: BaseListUseCase
    @Mock lateinit var yearStatsUseCase: BaseListUseCase
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var statsSectionManager: SelectedSectionManager
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var viewModel: StatsViewModel
    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = StatsViewModel(
                insightsUseCase,
                dayStatsUseCase,
                weekStatsUseCase,
                monthStatsUseCase,
                yearStatsUseCase,
                Dispatchers.Unconfined,
                selectedDateProvider,
                statsDateFormatter,
                statsSectionManager,
                analyticsTracker
        )
    }

    @Test
    fun `stores and tracks tab insights selection`() {
        viewModel.onSectionSelected(INSIGHTS)

        verify(statsSectionManager).setSelectedSection(INSIGHTS)
        verify(analyticsTracker).track(STATS_INSIGHTS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab days selection`() {
        viewModel.onSectionSelected(DAYS)

        verify(statsSectionManager).setSelectedSection(DAYS)
        verify(analyticsTracker).track(STATS_PERIOD_DAYS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab weeks selection`() {
        viewModel.onSectionSelected(WEEKS)

        verify(statsSectionManager).setSelectedSection(WEEKS)
        verify(analyticsTracker).track(STATS_PERIOD_WEEKS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab months selection`() {
        viewModel.onSectionSelected(MONTHS)

        verify(statsSectionManager).setSelectedSection(MONTHS)
        verify(analyticsTracker).track(STATS_PERIOD_MONTHS_ACCESSED)
    }

    @Test
    fun `stores and tracks tab years selection`() {
        viewModel.onSectionSelected(YEARS)

        verify(statsSectionManager).setSelectedSection(YEARS)
        verify(analyticsTracker).track(STATS_PERIOD_YEARS_ACCESSED)
    }
}
