package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.store.StatsStore.StatsType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.StatsType.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.StatsType.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.StatsType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.StatsType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.StatsType.PUBLICISE
import org.wordpress.android.fluxc.store.StatsStore.StatsType.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.StatsType.TODAY_STATS
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class StatsStore
@Inject constructor(private val coroutineContext: CoroutineContext) {
    suspend fun getStatsTypes(): List<StatsType> = withContext(coroutineContext){
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

    enum class StatsType {
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
}
