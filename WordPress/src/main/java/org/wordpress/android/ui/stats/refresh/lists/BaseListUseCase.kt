package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.util.combineMap
import org.wordpress.android.util.mergeNotNull

class BaseListUseCase
constructor(
    private val bgDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val useCases: List<BaseStatsUseCase<*, *>>,
    private val getStatsTypes: suspend (() -> List<StatsTypes>)
) {
    private val liveData = combineMap(
            useCases.associateBy { it.type }.mapValues { entry -> entry.value.liveData }
    )
    private val insights = MutableLiveData<List<StatsTypes>>()
    val data: LiveData<List<StatsBlock>> = mergeNotNull(insights, liveData) { insights, map ->
        insights.mapNotNull { map[it] }
    }

    val navigationTarget: LiveData<NavigationTarget> = mergeNotNull(useCases.map { it.navigationTarget })

    suspend fun loadInsightItems(site: SiteModel) {
        loadItems(site, false)
    }

    suspend fun refreshInsightItems(site: SiteModel, forced: Boolean = false) {
        loadItems(site, true, forced)
    }

    private suspend fun loadItems(site: SiteModel, refresh: Boolean, forced: Boolean = false) {
        withContext(bgDispatcher) {
            useCases.forEach { block -> launch { block.fetch(site, refresh, forced) } }
            val items = getStatsTypes()
            withContext(mainDispatcher) {
                insights.value = items
            }
        }
    }

    fun onCleared() {
        useCases.forEach { it.clear() }
    }
}
