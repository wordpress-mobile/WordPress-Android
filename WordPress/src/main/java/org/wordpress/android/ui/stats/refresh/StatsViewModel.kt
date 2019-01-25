package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_DAYS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_MONTHS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_WEEKS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_YEARS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.track
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class StatsViewModel
@Inject constructor(
    @Named(INSIGHTS_USE_CASE) private val insightsUseCase: BaseListUseCase,
    @Named(DAY_STATS_USE_CASE) private val dayStatsUseCase: BaseListUseCase,
    @Named(WEEK_STATS_USE_CASE) private val weekStatsUseCase: BaseListUseCase,
    @Named(MONTH_STATS_USE_CASE) private val monthStatsUseCase: BaseListUseCase,
    @Named(YEAR_STATS_USE_CASE) private val yearStatsUseCase: BaseListUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSectionManager: SelectedSectionManager,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private var isInitialized = false

    private val _showSnackbarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    val selectedDateChanged = selectedDateProvider.selectedDateChanged

    fun start(site: SiteModel) {
        // Check if VM is not already initialized
        if (!isInitialized) {
            isInitialized = true

            this.site = site

            loadStats()
        }
    }

    private fun loadStats() {
        loadData {
            insightsUseCase.loadData(site)
            dayStatsUseCase.loadData(site)
            weekStatsUseCase.loadData(site)
            monthStatsUseCase.loadData(site)
            yearStatsUseCase.loadData(site)
        }
    }

    private fun CoroutineScope.loadData(executeLoading: suspend () -> Unit) = launch {
        _isRefreshing.value = true

        executeLoading()

        _isRefreshing.value = false
    }

    // TODO: To be implemented in the future
    fun onMenuAction(action: Action, page: Page): Boolean {
        return when (action) {
            else -> true
        }
    }

    // TODO: To be implemented in the future
    fun onItemTapped(pageItem: Page) {
    }

    fun onPullToRefresh() {
        loadData {
            insightsUseCase.refreshData(site, true)
            dayStatsUseCase.refreshData(site, true)
            weekStatsUseCase.refreshData(site, true)
            monthStatsUseCase.refreshData(site, true)
            yearStatsUseCase.refreshData(site, true)
        }
    }

    fun onSelectedDateChange(statsGranularity: StatsGranularity) {
        loadData {
            when (statsGranularity) {
                DAYS -> dayStatsUseCase.refreshData(site)
                WEEKS -> weekStatsUseCase.refreshData(site)
                MONTHS -> monthStatsUseCase.refreshData(site)
                YEARS -> yearStatsUseCase.refreshData(site)
            }
        }
    }

    fun getSelectedSection() = statsSectionManager.getSelectedSection()

    fun onSectionSelected(statsSection: StatsSection) {
        statsSectionManager.setSelectedSection(statsSection)
        when (statsSection) {
            StatsSection.INSIGHTS -> analyticsTracker.track(STATS_INSIGHTS_ACCESSED)
            StatsSection.DAYS -> analyticsTracker.track(STATS_PERIOD_DAYS_ACCESSED)
            StatsSection.WEEKS -> analyticsTracker.track(STATS_PERIOD_WEEKS_ACCESSED)
            StatsSection.MONTHS -> analyticsTracker.track(STATS_PERIOD_MONTHS_ACCESSED)
            StatsSection.YEARS -> analyticsTracker.track(STATS_PERIOD_YEARS_ACCESSED)
        }
    }

    override fun onCleared() {
        super.onCleared()
        selectedDateProvider.clear()
    }
}
