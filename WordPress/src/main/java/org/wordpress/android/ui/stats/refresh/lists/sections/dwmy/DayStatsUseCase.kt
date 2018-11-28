package org.wordpress.android.ui.stats.refresh.lists.sections.dwmy

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.DAY_STATS_USE_CASES
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.util.combineMap
import org.wordpress.android.util.merge
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DayStatsUseCase
@Inject constructor(
    private val statsStore: StatsStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(DAY_STATS_USE_CASES) private val useCases: List<@JvmSuppressWildcards BaseStatsUseCase>
) {
    private val liveData = combineMap(
            useCases.associateBy { it.type }.mapValues { entry -> entry.value.liveData }
    )
    private val insights = MutableLiveData<List<TimeStatsTypes>>()
    val data: LiveData<List<StatsBlock>> = merge(insights, liveData) { insights, map ->
        insights.mapNotNull { map[it] }
    }

    val navigationTarget: LiveData<NavigationTarget> = merge(useCases.map { it.navigationTarget })

    suspend fun loadInsightItems(site: SiteModel) {
        loadItems(site, false)
    }

    suspend fun refreshInsightItems(site: SiteModel, forced: Boolean = false) {
        loadItems(site, true, forced)
    }

    private suspend fun loadItems(site: SiteModel, refresh: Boolean, forced: Boolean = false) {
        withContext(bgDispatcher) {
            useCases.forEach { block -> launch { block.fetch(site, refresh, forced) } }
            val items = statsStore.getTimeStatsTypes()
            withContext(mainDispatcher) {
                insights.value = items
            }
        }
    }

    fun onCleared() {
        useCases.forEach { it.clear() }
    }
}
