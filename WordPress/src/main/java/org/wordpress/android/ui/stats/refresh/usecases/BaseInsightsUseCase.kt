package org.wordpress.android.ui.stats.refresh.usecases

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.ListInsightItem
import org.wordpress.android.ui.stats.refresh.NavigationTarget

abstract class BaseInsightsUseCase(
    val type: InsightsTypes,
    private val mainDispatcher: CoroutineDispatcher
) {
    private val mutableLiveData = MutableLiveData<InsightsItem>()
    val liveData: LiveData<InsightsItem> = mutableLiveData
    private val mutableNavigationTarget = MutableLiveData<NavigationTarget>()
    val navigationTarget: LiveData<NavigationTarget> = mutableNavigationTarget
    suspend fun fetch(site: SiteModel, refresh: Boolean, forced: Boolean) {
        if (liveData.value == null) {
            withContext(mainDispatcher) {
                mutableLiveData.value = loadCachedData(site) ?: ListInsightItem(
                        listOf(Empty)
                )
            }
        }
        if (refresh) {
            withContext(mainDispatcher) {
                mutableLiveData.value = fetchRemoteData(site, forced)
            }
        }
    }
    fun clear() {
        mutableLiveData.postValue(null)
    }

    fun navigateTo(target: NavigationTarget) {
        mutableNavigationTarget.value = target
    }

    protected abstract suspend fun loadCachedData(site: SiteModel): InsightsItem?
    protected abstract suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): InsightsItem?
}
