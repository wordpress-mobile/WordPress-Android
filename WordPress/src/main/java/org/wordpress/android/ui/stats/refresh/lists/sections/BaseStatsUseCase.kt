package org.wordpress.android.ui.stats.refresh.lists.sections

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Empty
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.Loading
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase.NotUsedUiState
import org.wordpress.android.util.merge

abstract class BaseStatsUseCase<Model, UiState>(
    val type: InsightsTypes,
    private val mainDispatcher: CoroutineDispatcher
) {
    private val domainModel = MutableLiveData<DomainModel<Model>>()
    private val uiState = MutableLiveData<UiState>()
    val liveData: LiveData<StatsBlock> = merge(domainModel, uiState) { domainModel, uiState ->
        when {
            domainModel == null -> Empty(false)
            domainModel.error != null -> createFailedItem(domainModel.error)
            domainModel.model != null -> createDataItem(buildModel(domainModel.model, uiState))
            else -> Loading(type)
        }
    }

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
            loadCachedData(site)
        }
        if (refresh) {
            fetchRemoteData(site, forced)
        }
    }

    suspend fun onModel(model: Model?) {
        withContext(mainDispatcher) {
            domainModel.value = DomainModel(model = model)
        }
    }

    suspend fun onError(message: String) {
        withContext(mainDispatcher) {
            domainModel.value = DomainModel(error = message)
        }
    }

    fun onUiState(newState: UiState?) {
        uiState.value = newState
    }

    /**
     * Clears the LiveData value when we switch the current Site so we don't show the old data for a new site
     */
    fun clear() {
        domainModel.postValue(null)
        uiState.postValue(null)
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
    protected abstract suspend fun loadCachedData(site: SiteModel)

    /**
     * Fetches remote data from the endpoint.
     * @param site for which we fetch the data
     * @param forced is true when we want to get the fresh data
     * @return the list item or null when we haven't received a correct response from the API
     */
    protected abstract suspend fun fetchRemoteData(site: SiteModel, forced: Boolean)

    protected abstract fun buildModel(model: Model, nullableUiState: UiState?): List<BlockListItem>

    private fun createFailedItem(message: String): Error {
        return Error(type, message)
    }

    private fun createDataItem(data: List<BlockListItem>): BlockList {
        return BlockList(type, data)
    }

    data class DomainModel<Model>(val model: Model? = null, val error: String? = null)

    abstract class StatefulUseCase<Model, UiState>(
        type: InsightsTypes,
        mainDispatcher: CoroutineDispatcher,
        private val defaultUiState: UiState
    ) : BaseStatsUseCase<Model, UiState>(type, mainDispatcher) {
        final override fun buildModel(model: Model, nullableUiState: UiState?): List<BlockListItem> {
            return buildStatefulModel(model, nullableUiState ?: defaultUiState)
        }

        protected abstract fun buildStatefulModel(model: Model, uiState: UiState): List<BlockListItem>
    }

    abstract class StatelessUseCase<Model>(
        type: InsightsTypes,
        mainDispatcher: CoroutineDispatcher
    ) : BaseStatsUseCase<Model, NotUsedUiState>(type, mainDispatcher) {
        abstract fun buildModel(model: Model): List<BlockListItem>

        final override fun buildModel(model: Model, nullableUiState: NotUsedUiState?): List<BlockListItem> {
            return buildModel(model)
        }

        object NotUsedUiState
    }
}
