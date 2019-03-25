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
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DAYS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.MONTHS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.WEEKS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.YEARS
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.SelectedSectionManager
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.mergeNotNull
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

    val siteChanged = statsSiteProvider.siteChanged

    private val _toolbarHasShadow = MutableLiveData<Boolean>()
    val toolbarHasShadow: LiveData<Boolean> = _toolbarHasShadow

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
    }

    private fun CoroutineScope.loadData(executeLoading: suspend () -> Unit) = launch {
        _isRefreshing.value = true

        executeLoading()

        _isRefreshing.value = false
    }

    fun onPullToRefresh() {
        refreshData()
    }

    fun refreshData() {
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

    fun getSelectedSection() = statsSectionManager.getSelectedSection()

    fun onSectionSelected(statsSection: StatsSection) {
        statsSectionManager.setSelectedSection(statsSection)

        listUseCases[statsSection]?.onListSelected()

        _toolbarHasShadow.value = statsSection == INSIGHTS
        when (statsSection) {
            INSIGHTS -> analyticsTracker.track(STATS_INSIGHTS_ACCESSED)
            DAYS -> analyticsTracker.track(STATS_PERIOD_DAYS_ACCESSED)
            WEEKS -> analyticsTracker.track(STATS_PERIOD_WEEKS_ACCESSED)
            MONTHS -> analyticsTracker.track(STATS_PERIOD_MONTHS_ACCESSED)
            YEARS -> analyticsTracker.track(STATS_PERIOD_YEARS_ACCESSED)
            DETAIL -> {
            }
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
