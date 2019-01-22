package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.stats.StatsStore.StatsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.Action
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.Action.MOVE_DOWN
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.Action.MOVE_UP
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.Action.REMOVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.PackageUtils
import org.wordpress.android.util.combineMap
import org.wordpress.android.util.merge
import org.wordpress.android.util.mergeNotNull

class BaseListUseCase
constructor(
    private val bgDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val useCases: List<BaseStatsUseCase<*, *>>,
    private val getStatsTypes: suspend ((site: SiteModel) -> List<StatsTypes>),
    private val moveTypeUp: suspend ((site: SiteModel, type: StatsTypes) -> Unit) = { _, _ -> },
    private val moveTypeDown: suspend ((site: SiteModel, type: StatsTypes) -> Unit) = { _, _ -> },
    private val removeType: suspend ((site: SiteModel, type: StatsTypes) -> Unit) = { _, _ -> }
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
    val menuClick: LiveData<MenuClick> = merge(mergeNotNull(useCases.map { it.menuClick }), data) { click, viewModel ->
        if (click != null && viewModel != null) {
            val indexOfBlock = viewModel.indexOfFirst { it.statsTypes == click.type }
            click.showUpAction = indexOfBlock > 0
            click.showDownAction = indexOfBlock < viewModel.size - 1
            click
        } else {
            null
        }
    }

    suspend fun loadData(site: SiteModel) {
        loadData(site, false, false)
    }

    suspend fun refreshData(site: SiteModel, forced: Boolean = false) {
        loadData(site, true, forced)
    }

    suspend fun refreshTypes(site: SiteModel) {
        val items = getStatsTypes(site)
        withContext(mainDispatcher) {
            statsTypes.value = items
        }
    }

    private suspend fun loadData(site: SiteModel, refresh: Boolean, forced: Boolean) {
        withContext(bgDispatcher) {
            if (PackageUtils.isDebugBuild() && useCases.distinctBy { it.type }.size < useCases.size) {
                throw RuntimeException("Duplicate stats type in a use case")
            }
            useCases.forEach { block -> launch { block.fetch(site, refresh, forced) } }
            refreshTypes(site)
        }
    }

    fun onCleared() {
        useCases.forEach { it.clear() }
    }

    suspend fun onAction(site: SiteModel, type: StatsTypes, action: Action) {
        when (action) {
            MOVE_UP -> moveTypeUp(site, type)
            MOVE_DOWN -> moveTypeDown(site, type)
            REMOVE -> removeType(site, type)
        }
    }
}
