package org.wordpress.android.ui.stats.refresh.lists.sections

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.Loading
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.State.Data
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.State.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase.NotUsedUiState
import org.wordpress.android.util.merge

/**
 * Do not override this class directly. Use StatefulUseCase or StatelessUseCase instead.
 */
abstract class BaseStatsUseCase<DOMAIN_MODEL, UI_STATE>(
    val type: InsightsTypes,
    private val mainDispatcher: CoroutineDispatcher
) {
    private val domainModel = MutableLiveData<State<DOMAIN_MODEL>>()
    private val uiState = MutableLiveData<UI_STATE>()
    val liveData: LiveData<StatsBlock> = merge(domainModel, uiState) { data, uiState ->
        when (data) {
            is State.Loading -> Loading(type)
            is State.Error -> createFailedItem(data.error)
            is Data -> createDataItem(buildUiModel(data.model, uiState))
            is Empty, null -> null
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
            withContext(mainDispatcher) {
                this@BaseStatsUseCase.domainModel.value = State.Loading()
            }
            loadCachedData(site)
        }
        if (refresh) {
            fetchRemoteData(site, forced)
        }
    }

    /**
     * Trigger this method when there is a new (updated) model available.
     * @param domainModel new data
     */
    suspend fun onModel(domainModel: DOMAIN_MODEL) {
        withContext(mainDispatcher) {
            this@BaseStatsUseCase.domainModel.value = Data(model = domainModel)
        }
    }

    /**
     * Trigger this method when there is no response from the API (the block is missing).
     */
    suspend fun onEmpty() {
        withContext(mainDispatcher) {
            this@BaseStatsUseCase.domainModel.value = Empty()
        }
    }

    /**
     * Trigger this method when you want to display an error on the UI
     * @param message that should be displayed
     */
    suspend fun onError(message: String) {
        withContext(mainDispatcher) {
            this@BaseStatsUseCase.domainModel.value = State.Error(message)
        }
    }

    /**
     * Trigger this method when the UI state has changed.
     * @param newState
     */
    fun onUiState(newState: UI_STATE?) {
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
     */
    protected abstract suspend fun loadCachedData(site: SiteModel)

    /**
     * Fetches remote data from the endpoint.
     * @param site for which we fetch the data
     * @param forced is true when we want to get the fresh data
     */
    protected abstract suspend fun fetchRemoteData(site: SiteModel, forced: Boolean)

    /**
     * Transforms given domain model and ui state into the UI model
     * @param domainModel domain model coming from FluxC
     * @param nullableUiState contains UI specific data
     * @return a list of block list items
     */
    protected abstract fun buildUiModel(domainModel: DOMAIN_MODEL, nullableUiState: UI_STATE?): List<BlockListItem>

    private fun createFailedItem(message: String): Error {
        return Error(type, message)
    }

    private fun createDataItem(data: List<BlockListItem>): BlockList {
        return BlockList(type, data)
    }

    private sealed class State<DOMAIN_MODEL> {
        data class Error<DOMAIN_MODEL>(val error: String) : State<DOMAIN_MODEL>()
        data class Data<DOMAIN_MODEL>(val model: DOMAIN_MODEL) : State<DOMAIN_MODEL>()
        class Empty<DOMAIN_MODEL> : State<DOMAIN_MODEL>()
        class Loading<DOMAIN_MODEL> : State<DOMAIN_MODEL>()
    }

    /**
     * Stateful use case should be used when we have a block that has a UI state that needs to be preserved
     * over rotation pull to refresh. It is for example a block with Tabs or with expandable item.
     * @param defaultUiState default value the UI state should have when the screen first loads
     */
    abstract class StatefulUseCase<DOMAIN_MODEL, UI_STATE>(
        type: InsightsTypes,
        mainDispatcher: CoroutineDispatcher,
        private val defaultUiState: UI_STATE
    ) : BaseStatsUseCase<DOMAIN_MODEL, UI_STATE>(type, mainDispatcher) {
        final override fun buildUiModel(domainModel: DOMAIN_MODEL, nullableUiState: UI_STATE?): List<BlockListItem> {
            return buildStatefulUiModel(domainModel, nullableUiState ?: defaultUiState)
        }

        /**
         * Transforms given domain model and ui state into the UI model
         * @param domainModel domain model coming from FluxC
         * @param uiState contains UI specific data
         * @return a list of block list items
         */
        protected abstract fun buildStatefulUiModel(domainModel: DOMAIN_MODEL, uiState: UI_STATE): List<BlockListItem>
    }

    /**
     * Stateless use case should be used for the blocks that display just plain data.
     * These blocks don't have only one UI state and it doesn't change.
     */
    abstract class StatelessUseCase<DOMAIN_MODEL>(
        type: InsightsTypes,
        mainDispatcher: CoroutineDispatcher
    ) : BaseStatsUseCase<DOMAIN_MODEL, NotUsedUiState>(type, mainDispatcher) {
        /**
         * Transforms given domain model into the UI model
         * @param domainModel domain model coming from FluxC
         * @return a list of block list items
         */
        abstract fun buildUiModel(domainModel: DOMAIN_MODEL): List<BlockListItem>

        final override fun buildUiModel(
            domainModel: DOMAIN_MODEL,
            nullableUiState: NotUsedUiState?
        ): List<BlockListItem> {
            return buildUiModel(domainModel)
        }

        object NotUsedUiState
    }
}
