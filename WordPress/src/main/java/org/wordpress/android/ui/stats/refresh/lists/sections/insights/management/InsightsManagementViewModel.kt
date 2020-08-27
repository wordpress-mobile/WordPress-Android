package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.INSIGHTS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.NavigationAction
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem.Type.INSIGHT
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.trackWithType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class InsightsManagementViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val defaultDispatcher: CoroutineDispatcher,
    @Named(INSIGHTS_USE_CASE) val insightsUseCase: BaseListUseCase,
    private val siteProvider: StatsSiteProvider,
    private val statsStore: StatsStore,
    private val insightsManagementMapper: InsightsManagementMapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _addedInsights = MutableLiveData<List<InsightListItem>>()
    val addedInsights: LiveData<List<InsightListItem>> = _addedInsights

    private val _closeInsightsManagement = SingleLiveEvent<Unit>()
    val closeInsightsManagement: LiveData<Unit> = _closeInsightsManagement

    private val _isMenuVisible = MutableLiveData<Boolean>()
    val isMenuVisible: LiveData<Boolean> = _isMenuVisible

    private val addedInsightTypes: MutableSet<InsightType> = mutableSetOf()
    private var isInitialized = false

    fun start(localSiteId: Int?) {
        if (!isInitialized) {
            isInitialized = true
            _isMenuVisible.value = false
            localSiteId?.let {
                siteProvider.start(localSiteId)
            }
            loadInsights()
        }
    }

    private fun loadInsights() {
        launch {
            addedInsightTypes.clear()
            addedInsightTypes.addAll(statsStore.getAddedInsights(siteProvider.siteModel))
            displayInsights()
        }
    }

    private fun displayInsights() {
        launch {
            _addedInsights.value = insightsManagementMapper.buildUIModel(
                    addedInsightTypes,
                    this@InsightsManagementViewModel::onItemButtonClicked
            )
        }
    }

    fun onSaveInsights() {
        analyticsTrackerWrapper.track(Stat.STATS_INSIGHTS_MANAGEMENT_SAVED)
        insightsUseCase.launch(defaultDispatcher) {
            statsStore.updateTypes(siteProvider.siteModel, addedInsightTypes.toList())

            insightsUseCase.loadData()
        }
        _closeInsightsManagement.call()
    }

    private fun onItemButtonClicked(insight: InsightType) {
        if (addedInsightTypes.contains(insight)) {
            analyticsTrackerWrapper.trackWithType(
                    Stat.STATS_INSIGHTS_MANAGEMENT_TYPE_REMOVED,
                    insight
            )
            addedInsightTypes.removeAll { it == insight }
        } else {
            analyticsTrackerWrapper.trackWithType(
                    Stat.STATS_INSIGHTS_MANAGEMENT_TYPE_ADDED,
                    insight
            )
            addedInsightTypes.add(insight)
        }
        displayInsights()
        _isMenuVisible.value = true
    }

    sealed class InsightListItem(val type: Type) {
        enum class Type {
            HEADER, INSIGHT
        }

        data class Header(@StringRes val text: Int) : InsightListItem(HEADER)

        data class InsightModel(
            val insightType: InsightType,
            val name: Int,
            val status: Status,
            val onClick: NavigationAction
        ) : InsightListItem(INSIGHT) {
            enum class Status {
                ADDED,
                REMOVED
            }
        }
    }
}
