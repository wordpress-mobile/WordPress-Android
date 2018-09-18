package org.wordpress.android.ui.stats.refresh

import kotlinx.coroutines.experimental.async
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
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.PUBLICISE
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import javax.inject.Inject

class InsightsDomain
@Inject constructor(private val statsStore: StatsStore) {
    private fun load(type: InsightsTypes): InsightsItem {
        return when (type) {
            LATEST_POST_SUMMARY,
            MOST_POPULAR_DAY_AND_HOUR,
            ALL_TIME_STATS,
            FOLLOWER_TOTALS,
            TAGS_AND_CATEGORIES,
            ANNUAL_SITE_STATS,
            COMMENTS,
            FOLLOWERS,
            TODAY_STATS,
            POSTING_ACTIVITY,
            PUBLICISE -> NotImplemented(type.name)
        }
    }

    suspend fun loadInsightItems(): List<InsightsItem> {
        return statsStore.getInsights()
                .map { async { load(it) } }
                .map { it.await() }
    }
}
