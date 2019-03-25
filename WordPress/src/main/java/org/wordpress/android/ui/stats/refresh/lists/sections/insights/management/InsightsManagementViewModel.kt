package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.*
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ScopedViewModel
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Named

class InsightsManagementViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val siteProvider: StatsSiteProvider,
    private val statsStore: StatsStore
) : ScopedViewModel(mainDispatcher) {
    private val _removedInsights = MutableLiveData<List<InsightModel>>()
    val removedInsights: LiveData<List<InsightModel>>
        get() = _removedInsights

    private val mutableSnackbarMessage = MutableLiveData<Int>()

    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = mutableSnackbarMessage.map {
        SnackbarMessageHolder(it)
    }

    fun start() {
        loadRemovedInsights()
    }

    fun loadRemovedInsights() {
        launch {
            val model = statsStore.getInsightsManagementModel(siteProvider.siteModel)
            _removedInsights.value = model.removedTypes.map { InsightModel(it) }
        }
    }

    override fun onCleared() {
        mutableSnackbarMessage.value = null
    }

    class InsightModel(insightsTypes: InsightsTypes) {
        @StringRes val name: Int = when (insightsTypes) {
            LATEST_POST_SUMMARY -> R.string.stats_insights_latest_post_summary
            MOST_POPULAR_DAY_AND_HOUR -> R.string.stats_insights_popular
            ALL_TIME_STATS -> R.string.stats_insights_all_time_stats
            TAGS_AND_CATEGORIES -> R.string.stats_insights_tags_and_categories
            COMMENTS -> R.string.stats_comments
            FOLLOWERS ->  R.string.stats_view_followers
            TODAY_STATS -> R.string.stats_insights_today
            POSTING_ACTIVITY -> R.string.stats_insights_posting_activity
            PUBLICIZE -> R.string.stats_view_publicize
            else -> R.string.unknown
        }
    }
}
