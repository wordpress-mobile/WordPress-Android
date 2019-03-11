package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_DAYS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_MONTHS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_WEEKS_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_PERIOD_YEARS_ACCESSED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toStatsSection
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.mapNullable
import org.wordpress.android.util.mergeNotNull
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsViewModel
@Inject constructor(
    @Named(LIST_STATS_USE_CASES) private val listUseCases: Map<StatsSection, BaseListUseCase>,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSectionManager: SelectedSectionManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val statsSiteProvider: StatsSiteProvider
) : ScopedViewModel(mainDispatcher) {
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private var isInitialized = false

    private val _showSnackbarMessage = mergeNotNull(
            listUseCases.values.map { it.snackbarMessage },
            distinct = true,
            singleEvent = true
    )
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    val selectedDateChanged = selectedDateProvider.selectedDateChanged

    val siteChanged = statsSiteProvider.siteChanged

    private val _toolbarHasShadow = MutableLiveData<Boolean>()
    val toolbarHasShadow: LiveData<Int> = _toolbarHasShadow.mapNullable {
        if (it == true) resourceProvider.getDimensionPixelSize(R.dimen.appbar_elevation) else 0
    }

    fun start(site: SiteModel, launchedFromWidget: Boolean, initialSection: StatsSection?) {
        // Check if VM is not already initialized
        if (!isInitialized) {
            statsSiteProvider.start(site)
            isInitialized = true

            initialSection?.let { statsSectionManager.setSelectedSection(it) }

            _toolbarHasShadow.value = statsSectionManager.getSelectedSection() == INSIGHTS

            analyticsTracker.track(AnalyticsTracker.Stat.STATS_ACCESSED, site)

            if (launchedFromWidget) {
                analyticsTracker.track(AnalyticsTracker.Stat.STATS_WIDGET_TAPPED, site)
            }
        }
        listUseCases.values.forEach { it.updateDateSelector(statsSectionManager.getSelectedStatsGranularity()) }
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
        _showSnackbarMessage.value = null
        statsSiteProvider.clear()
        if (networkUtilsWrapper.isNetworkAvailable()) {
            loadData {
                listUseCases[statsSectionManager.getSelectedSection()]?.refreshData(true)
            }
        } else {
            _isRefreshing.value = false
            _showSnackbarMessage.value = SnackbarMessageHolder(R.string.no_network_title)
        }
    }

    fun onSelectedDateChange(statsGranularity: StatsGranularity) {
        launch {
            listUseCases[statsGranularity.toStatsSection()]?.onDateChanged(statsGranularity)
        }
    }

    fun getSelectedSection() = statsSectionManager.getSelectedSection()

    fun onSectionSelected(statsSection: StatsSection) {
        statsSectionManager.setSelectedSection(statsSection)
        listUseCases[statsSection]?.updateDateSelector(statsSectionManager.getSelectedStatsGranularity())
        _toolbarHasShadow.value = statsSection == INSIGHTS
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
        _showSnackbarMessage.value = null
        selectedDateProvider.clear()
        statsSiteProvider.stop()
    }

    data class DateSelectorUiModel(
        val isVisible: Boolean = false,
        val date: String? = null,
        val enableSelectPrevious: Boolean = false,
        val enableSelectNext: Boolean = false
    )
}
