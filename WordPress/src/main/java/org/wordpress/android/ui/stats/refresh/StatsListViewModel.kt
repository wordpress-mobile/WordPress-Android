package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
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
import org.wordpress.android.modules.UI_CONTEXT
import org.wordpress.android.ui.stats.refresh.StatsType.NOT_IMPLEMENTED
import java.util.Random
import javax.inject.Inject
import javax.inject.Named

class StatsListViewModel
@Inject constructor(
    private val statsStore: StatsStore,
    @Named(UI_CONTEXT) private val uiContext: CoroutineDispatcher
) : ViewModel() {
    private val mutableData: MutableLiveData<List<StatsItem>> = MutableLiveData()
    val data: LiveData<List<StatsItem>> = mutableData
    private val random = Random()
    private val loaders: Map<InsightsTypes, (InsightsTypes) -> StatsItem> = mapOf(ALL_TIME_STATS to {})
    fun start() {
        launch(uiContext) {
            val deferred = statsStore.getInsights().map {
                when (it) {
                    ALL_TIME_STATS,
                    LATEST_POST_SUMMARY,
                    MOST_POPULAR_DAY_AND_HOUR,
                    FOLLOWER_TOTALS,
                    TAGS_AND_CATEGORIES,
                    ANNUAL_SITE_STATS,
                    COMMENTS,
                    FOLLOWERS,
                    TODAY_STATS,
                    POSTING_ACTIVITY,
                    PUBLICISE -> async { loadItem(it) }
                }
            }
            mutableData.value = deferred.map { it.await() }
        }
    }

    private suspend fun loadItem(type: InsightsTypes): StatsItem {
        val delay = (random.nextInt() % 5000) + 5000
        Log.d("stats_loading", "Starting loading $type with delay $delay")
        delay(delay)
        Log.d("stats_loading", "Finished loading $type")
        return NotImplemented(type.name)
    }

    interface StatsItemLoader<T: InsightsTypes>{
        fun loadItem(type: T): StatsItem
    }
}

enum class StatsType {
    // TODO Remove once all the Types are implemented
    NOT_IMPLEMENTED
}

open class StatsItem(val type: StatsType) {
    val uuid = type.ordinal
}
data class NotImplemented(val text: String) : StatsItem(NOT_IMPLEMENTED)

