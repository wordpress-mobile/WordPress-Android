package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TOTALS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.PUBLICIZE
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TODAY_STATS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.INSIGHTS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel.Status.ADDED
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel.Status.REMOVED
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class InsightsManagementViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(INSIGHTS_USE_CASE) val insightsUseCase: BaseListUseCase,
    private val siteProvider: StatsSiteProvider,
    private val statsStore: StatsStore
) : ScopedViewModel(mainDispatcher) {
    private val _removedInsights = MutableLiveData<List<InsightModel>>()
    val removedInsights: LiveData<List<InsightModel>> = _removedInsights

    private val _addedInsights = MutableLiveData<List<InsightModel>>()
    val addedInsights: LiveData<List<InsightModel>> = _addedInsights

    private val _closeInsightsManagement = SingleLiveEvent<Unit>()
    val closeInsightsManagement: LiveData<Unit> = _closeInsightsManagement

    private val _isMenuVisible = MutableLiveData<Boolean>()
    val isMenuVisible: LiveData<Boolean> = _isMenuVisible

    private lateinit var insights: List<InsightModel>
    private var isInitialized = false

    fun start() {
        if (!isInitialized) {
            isInitialized = true
            _isMenuVisible.value = false
            loadInsights()
        }
    }

    private fun loadInsights() {
        launch {
            val addedInsights = statsStore.getAddedInsights(siteProvider.siteModel)
            insights = addedInsights.map { InsightModel(it, ADDED) } +
                    statsStore.getRemovedInsights(addedInsights).map { InsightModel(it, REMOVED) }
            displayInsights()
        }
    }

    private fun displayInsights() {
        _addedInsights.value = insights.filter { it.type == ADDED }
        _removedInsights.value = insights.filter { it.type == REMOVED }
    }

    fun onSaveInsights() {
        // This has to be GlobalScope because otherwise the coroutine gets killed with the ViewModel
        GlobalScope.launch {
            val addedTypes = insights.filter { it.type == ADDED }.map { it.insightType }
            statsStore.updateTypes(siteProvider.siteModel, addedTypes)

            insightsUseCase.loadData()
        }
        _closeInsightsManagement.call()
    }

    fun onAddedInsightsReordered(addedInsights: List<InsightModel>) {
        insights = addedInsights + insights.filter { it.type == REMOVED }
        _isMenuVisible.value = true
    }

    fun onItemButtonClicked(insight: InsightModel) {
        if (insight.type == ADDED) {
            insight.type = REMOVED
        } else {
            insight.type = ADDED
        }
        displayInsights()
        _isMenuVisible.value = true
    }

    data class InsightModel(val insightType: InsightType, var type: Status) {
        @StringRes val name: Int = when (insightType) {
            LATEST_POST_SUMMARY -> R.string.stats_insights_latest_post_summary
            MOST_POPULAR_DAY_AND_HOUR -> R.string.stats_insights_popular
            ALL_TIME_STATS -> R.string.stats_insights_all_time_stats
            TAGS_AND_CATEGORIES -> R.string.stats_insights_tags_and_categories
            COMMENTS -> R.string.stats_comments
            FOLLOWERS -> R.string.stats_view_followers
            TODAY_STATS -> R.string.stats_insights_today
            POSTING_ACTIVITY -> R.string.stats_insights_posting_activity
            PUBLICIZE -> R.string.stats_view_publicize
            ANNUAL_SITE_STATS -> R.string.stats_insights_this_year_site_stats
            FOLLOWER_TOTALS -> R.string.stats_view_follower_totals
        }

        enum class Status {
            ADDED,
            REMOVED
        }
    }
}
