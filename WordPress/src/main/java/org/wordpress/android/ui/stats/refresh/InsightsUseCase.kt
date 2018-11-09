package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// TODO: This should be a "@SiteScope" of sorts
@Singleton
class InsightsUseCase
@Inject constructor(
    private val statsStore: StatsStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val insightsAllTimeUseCase: InsightsAllTimeUseCase,
    private val latestPostSummaryUseCase: LatestPostSummaryUseCase,
    private val todayStatsUseCase: TodayStatsUseCase
) {
    private val _data = MutableLiveData<List<InsightsItem>>()
    val data: LiveData<List<InsightsItem>> = _data

    private suspend fun load(site: SiteModel, type: InsightsTypes, forced: Boolean): InsightsItem {
        return when (type) {
            ALL_TIME_STATS -> insightsAllTimeUseCase.loadAllTimeInsights(site, forced)
            LATEST_POST_SUMMARY -> latestPostSummaryUseCase.loadLatestPostSummary(site, forced)
            TODAY_STATS -> todayStatsUseCase.loadTodayStats(site, forced)
            MOST_POPULAR_DAY_AND_HOUR,
            FOLLOWER_TOTALS,
            TAGS_AND_CATEGORIES,
            ANNUAL_SITE_STATS,
            COMMENTS,
            FOLLOWERS,
            POSTING_ACTIVITY,
            PUBLICIZE -> NotImplemented(type.name)
        }
    }

    // TODO: Remove once a separate instance is used every time a site is changed
    fun reset() {
        _data.value = listOf(Empty())
    }

    suspend fun loadInsightItems(site: SiteModel, forced: Boolean = false) {
        withContext(bgDispatcher) {
            val items = statsStore.getInsights()
                    .map { async { load(site, it, forced) } }
                    .map { it.await() }

            withContext(mainDispatcher) {
                _data.value = if (items.isEmpty()) {
                    listOf(Empty())
                } else {
                    items
                }
            }
        }
    }
}
