package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty

abstract class BaseInsightsUseCase(
    val type: InsightsTypes,
    private val mainDispatcher: CoroutineDispatcher
) {
    private val mutableLiveData = MutableLiveData<InsightsItem>()
    val liveData: LiveData<InsightsItem> = mutableLiveData
    suspend fun fetch(site: SiteModel, refresh: Boolean, forced: Boolean) {
        if (liveData.value == null) {
            withContext(mainDispatcher) {
                mutableLiveData.value = loadCachedData(site) ?: ListInsightItem(listOf(Empty))
            }
        }
        if (refresh) {
            withContext(mainDispatcher) {
                mutableLiveData.value = fetchRemoteData(site, refresh, forced)
            }
        }
    }

    protected abstract suspend fun loadCachedData(site: SiteModel): InsightsItem?
    protected abstract suspend fun fetchRemoteData(site: SiteModel, refresh: Boolean, forced: Boolean): InsightsItem
    fun clear() {
        mutableLiveData.postValue(null)
    }
}
