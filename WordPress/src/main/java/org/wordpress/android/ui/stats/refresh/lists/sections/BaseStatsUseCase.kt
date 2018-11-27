package org.wordpress.android.ui.stats.refresh.lists.sections

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.BlockList.ListUiState
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.Loading
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock

abstract class BaseStatsUseCase(
    val type: InsightsTypes,
    private val mainDispatcher: CoroutineDispatcher
) {
    private val mutableLiveData = MutableLiveData<StatsBlock>()
    val liveData: LiveData<StatsBlock> = mutableLiveData
    private val mutableNavigationTarget = MutableLiveData<NavigationTarget>()
    val navigationTarget: LiveData<NavigationTarget> = mutableNavigationTarget

    /**
     * Fetches data either from a local cache or from remote API
     * @param site for which we're fetching the data
     * @param refresh is true when we want to get the remote data
     * @param forced is true when we want to get fresh data and skip the cache
     */
    suspend fun fetch(site: SiteModel, refresh: Boolean, forced: Boolean) {
        if (liveData.value == null) {
            withContext(mainDispatcher) {
                mutableLiveData.value = loadCachedData(site) ?: Loading(
                        type
                )
            }
        }
        if (refresh) {
            withContext(mainDispatcher) {
                mutableLiveData.value = fetchRemoteData(site, forced)
            }
        }
    }

    /**
     * Clears the LiveData value when we switch the current Site so we don't show the old data for a new site
     */
    fun clear() {
        mutableLiveData.postValue(null)
    }

    /**
     * Passes a navigation target to the View layer which uses the context to open the correct activity.
     */
    fun navigateTo(target: NavigationTarget) {
        mutableNavigationTarget.value = target
    }

    /**
     * Loads data from a local cache. Returns a null value when the cache is empty.
     * @param site for which we load the data
     * @return the list item or null when the local value is empty
     */
    protected abstract suspend fun loadCachedData(site: SiteModel): StatsBlock?

    /**
     * Fetches remote data from the endpoint.
     * @param site for which we fetch the data
     * @param forced is true when we want to get the fresh data
     * @return the list item or null when we haven't received a correct response from the API
     */
    protected abstract suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): StatsBlock?

    protected fun onDataChanged(data: StatsBlock) {
        mutableLiveData.value = data
    }

    protected fun createFailedItem(@StringRes failingType: Int, message: String): Error {
        return Error(type, failingType, message)
    }

    protected fun createDataItem(data: List<BlockListItem>, uiState: ListUiState? = null): BlockList {
        return BlockList(type, data, uiState ?: this.uiState)
    }

    protected val uiState: ListUiState?
        get() {
            return (liveData.value as? BlockList)?.uiState
        }
}
