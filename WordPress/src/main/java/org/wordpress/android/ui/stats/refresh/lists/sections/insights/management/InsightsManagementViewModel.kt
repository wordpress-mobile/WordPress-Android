package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightTypesModel
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWER_TOTALS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.PUBLICIZE
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.INSIGHTS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel.Type.ADDED
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel.Type.REMOVED
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
            val model = statsStore.getInsightsManagementModel(siteProvider.siteModel)
            insights = model.addedTypes
                        .filter { it != FOLLOWER_TOTALS && it != ANNUAL_SITE_STATS }
                        .map { InsightModel(it, ADDED) } +
                    model.removedTypes
                        .filter { it != FOLLOWER_TOTALS && it != ANNUAL_SITE_STATS }
                        .map { InsightModel(it, REMOVED) }
            displayInsights()
        }
    }

    private fun displayInsights() {
        _addedInsights.value = insights.filter { it.type == ADDED }
        _removedInsights.value = insights.filter { it.type == REMOVED }
    }

    fun onSaveInsights() {
        launch {
            val addedTypes = insights.filter { it.type == ADDED }.map { it.insightsTypes }
            val removedTypes = insights.filter { it.type == REMOVED }.map { it.insightsTypes }
            val model = InsightTypesModel(addedTypes, removedTypes + FOLLOWER_TOTALS + ANNUAL_SITE_STATS)
            statsStore.updateTypes(siteProvider.siteModel, model)

            insightsUseCase.refreshData(true)
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

    data class InsightModel(val insightsTypes: InsightsTypes, var type: Type) {
        @StringRes val name: Int = when (insightsTypes) {
            LATEST_POST_SUMMARY -> R.string.stats_insights_latest_post_summary
            MOST_POPULAR_DAY_AND_HOUR -> R.string.stats_insights_popular
            ALL_TIME_STATS -> R.string.stats_insights_all_time_stats
            TAGS_AND_CATEGORIES -> R.string.stats_insights_tags_and_categories
            COMMENTS -> R.string.stats_comments
            FOLLOWERS -> R.string.stats_view_followers
            TODAY_STATS -> R.string.stats_insights_today
            POSTING_ACTIVITY -> R.string.stats_insights_posting_activity
            PUBLICIZE -> R.string.stats_view_publicize
            else -> R.string.unknown
        }

        enum class Type {
            ADDED,
            REMOVED
        }
    }
}
