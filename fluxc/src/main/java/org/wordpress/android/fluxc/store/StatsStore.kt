package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.PUBLICISE
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.AUTHORS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.CLICKS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.COUNTRIES
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.OVERVIEW
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.POSTS_AND_PAGES
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.REFERRERS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.SEARCH_TERMS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.VIDEOS
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class StatsStore
@Inject constructor(private val coroutineContext: CoroutineContext) {
    suspend fun getInsights(): List<InsightsTypes> = withContext(coroutineContext){
        return@withContext listOf(
                LATEST_POST_SUMMARY,
                TODAY_STATS,
                ALL_TIME_STATS,
                MOST_POPULAR_DAY_AND_HOUR,
                COMMENTS,
                TAGS_AND_CATEGORIES,
                FOLLOWERS,
                PUBLICISE)
    }

    suspend fun getTimeStatsTypes() :List<TimeStatsTypes> = withContext(coroutineContext) {
        return@withContext listOf(
                OVERVIEW,
                POSTS_AND_PAGES,
                REFERRERS,
                CLICKS,
                AUTHORS,
                COUNTRIES,
                SEARCH_TERMS,
                VIDEOS
        )
    }

    enum class InsightsTypes {
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
        PUBLICISE
    }

    enum class TimeStatsTypes {
        OVERVIEW,
        POSTS_AND_PAGES,
        REFERRERS,
        CLICKS,
        AUTHORS,
        COUNTRIES,
        SEARCH_TERMS,
        PUBLISHED,
        VIDEOS
    }
}
