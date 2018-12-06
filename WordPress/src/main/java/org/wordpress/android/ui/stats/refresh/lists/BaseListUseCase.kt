package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.combineMap
import org.wordpress.android.util.mergeNotNull

class BaseListUseCase
constructor(
    private val bgDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val useCases: List<BaseStatsUseCase<*, *>>,
    private val getStatsTypes: suspend (() -> List<StatsTypes>)
) {
    private val blockListData = combineMap(
            useCases.associateBy { it.type }.mapValues { entry -> entry.value.liveData }
    )
    private val statsTypes = MutableLiveData<List<StatsTypes>>()
    val data: LiveData<List<StatsBlock>> = mergeNotNull(statsTypes, blockListData) { insights, map ->
        insights.mapNotNull {
            if (map.containsKey(it)) {
                map[it]
            } else {
                AppLog.e(T.STATS, "There is no use case consuming given Stats type: $it")
                null
            }
        }
    }

    val navigationTarget: LiveData<NavigationTarget> = mergeNotNull(useCases.map { it.navigationTarget })

    suspend fun loadInsightItems(site: SiteModel) {
        loadItems(site, false, false)
    }

    suspend fun refreshInsightItems(site: SiteModel, forced: Boolean = false) {
        loadItems(site, true, forced)
    }

    private suspend fun loadItems(site: SiteModel, refresh: Boolean, forced: Boolean) {
        withContext(bgDispatcher) {
            useCases.forEach { block -> launch { block.fetch(site, refresh, forced) } }
            val items = getStatsTypes()
            withContext(mainDispatcher) {
                statsTypes.value = items
            }
        }
    }

    fun onCleared() {
        useCases.forEach { it.clear() }
    }
}
