package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LoadMode
import org.wordpress.android.fluxc.model.stats.LoadMode.INITIAL
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.PackageUtils
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

    suspend fun loadData(site: SiteModel, loadMode: LoadMode = INITIAL) {
        loadData(site, false, false, loadMode)
    }

    suspend fun refreshData(site: SiteModel, forced: Boolean = false, loadMode: LoadMode = INITIAL) {
        loadData(site, true, forced, loadMode)
    }

    private suspend fun loadData(site: SiteModel, refresh: Boolean, forced: Boolean, loadMode: LoadMode) {
        withContext(bgDispatcher) {
            if (PackageUtils.isDebugBuild() && useCases.distinctBy { it.type }.size < useCases.size) {
                throw RuntimeException("Duplicate stats type in a use case")
            }
            useCases.forEach { block -> launch { block.fetch(site, refresh, forced, loadMode) } }
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
