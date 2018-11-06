package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
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
import org.wordpress.android.modules.DEFAULT_SCOPE
import javax.inject.Inject
import javax.inject.Named

class InsightsViewModel
@Inject constructor(
    private val statsStore: StatsStore,
    @Named(DEFAULT_SCOPE) private val scope: CoroutineScope,
    private val insightsAllTimeViewModel: InsightsAllTimeViewModel,
    private val latestPostSummaryViewModel: LatestPostSummaryViewModel
) {
    private val mediatorNavigationTarget: MediatorLiveData<NavigationTarget> = MediatorLiveData()
    val navigationTarget: LiveData<NavigationTarget> = mediatorNavigationTarget

    init {
        mediatorNavigationTarget.addSource(latestPostSummaryViewModel.navigationTarget) {
            mediatorNavigationTarget.value = it
        }
    }

    private suspend fun load(site: SiteModel, type: InsightsTypes, forced: Boolean): InsightsItem {
        return when (type) {
            ALL_TIME_STATS -> insightsAllTimeViewModel.loadAllTimeInsights(site, forced)
            LATEST_POST_SUMMARY -> latestPostSummaryViewModel.loadLatestPostSummary(site, forced)
            MOST_POPULAR_DAY_AND_HOUR,
            FOLLOWER_TOTALS,
            TAGS_AND_CATEGORIES,
            ANNUAL_SITE_STATS,
            COMMENTS,
            FOLLOWERS,
            TODAY_STATS,
            POSTING_ACTIVITY,
            PUBLICIZE -> NotImplemented(type.name)
        }
    }

    suspend fun loadInsightItems(site: SiteModel, forced: Boolean = false): List<InsightsItem> =
            withContext(scope.coroutineContext) {
                val items = statsStore.getInsights()
                        .map { async { load(site, it, forced) } }
                        .map { it.await() }

                if (items.isEmpty()) {
                    return@withContext listOf(Empty())
                } else {
                    return@withContext items
                }
            }
}
