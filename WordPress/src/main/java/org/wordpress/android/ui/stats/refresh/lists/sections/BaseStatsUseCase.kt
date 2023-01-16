package org.wordpress.android.ui.stats.refresh.lists.sections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.State.Data
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.State.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.State.Error
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.State.Loading
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase.NotUsedUiState
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.Event
import kotlin.coroutines.CoroutineContext

/**
 * Do not override this class directly. Use StatefulUseCase or StatelessUseCase instead.
 */
abstract class BaseStatsUseCase<DOMAIN_MODEL, UI_STATE>(
    val type: StatsType,
    private val mainDispatcher: CoroutineDispatcher,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val defaultUiState: UI_STATE,
    private val fetchParams: List<UseCaseParam> = listOf(),
    private val uiUpdateParams: List<UseCaseParam> = listOf()
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = backgroundDispatcher

    private var domainState: UseCaseState = LOADING
    private var domainModel: DOMAIN_MODEL? = null
    private var uiState: UI_STATE = defaultUiState
    private var updateJob: Job? = null

    private val _liveData = MutableLiveData<UseCaseModel>()
    val liveData: LiveData<UseCaseModel> = _liveData

    private val mutableNavigationTarget = MutableLiveData<Event<NavigationTarget>>()
    val navigationTarget: LiveData<Event<NavigationTarget>> = mutableNavigationTarget

    /**
     * Fetches data either from a local cache or from remote API
     * @param refresh is true when we want to get the remote data
     * @param forced is true when we want to get fresh data and skip the cache
     */
    suspend fun fetch(refresh: Boolean, forced: Boolean) {
        val firstLoad = domainModel == null
        var emptyDb = false
        if (firstLoad) {
            updateUseCaseState(LOADING)
        }
        if (domainState == LOADING) {
            val cachedData = loadCachedData()
            if (cachedData != null) {
                domainModel = cachedData
                updateState()
            } else {
                emptyDb = true
            }
        }
        if (refresh || domainState != SUCCESS || emptyDb) {
            updateUseCaseState(LOADING)
            val state = fetchRemoteData(forced)
            evaluateState(state)
        }
    }

    suspend fun onParamsChange(param: UseCaseParam) {
        if (uiUpdateParams.any { it == param }) {
            onUiState()
        }
        if (fetchParams.any { it == param }) {
            fetch(refresh = true, forced = false)
        }
    }

    protected suspend fun evaluateState(state: State<DOMAIN_MODEL>) {
        val useCaseState = when (state) {
            is Error -> ERROR
            is Data -> {
                if (!state.cached) {
                    val updatedCachedData = loadCachedData()
                    if (domainModel != updatedCachedData) {
                        domainModel = updatedCachedData
                        updateState()
                    }
                }
                SUCCESS
            }
            is Empty -> EMPTY
            is Loading -> LOADING
        }
        updateUseCaseState(useCaseState)
    }

    /**
     * Trigger this method when the UI state has changed.
     * @param newState
     */
    fun onUiState(newState: UI_STATE? = null) {
        uiState = newState ?: uiState
        updateState()
    }

    /**
     * Trigger this method when updating only a part of UI state.
     * @param update function
     */
    fun updateUiState(update: (UI_STATE) -> UI_STATE) {
        val previousState = uiState ?: defaultUiState
        val updatedState = update(previousState)
        if (previousState != updatedState) {
            onUiState(updatedState)
        }
    }

    private fun updateUseCaseState(newState: UseCaseState) {
        if (domainState != newState) {
            domainState = newState
            updateState()
        }
    }

    /**
     * Clears the LiveData value when we switch the current Site so we don't show the old data for a new site
     */
    fun clear() {
        domainModel = null
        domainState = LOADING
        uiState = defaultUiState
        updateState()
    }

    /**
     * Passes a navigation target to the View layer which uses the context to open the correct activity.
     */
    fun navigateTo(target: NavigationTarget) {
        mutableNavigationTarget.value = Event(target)
    }

    /**
     * Loads data from a local cache. Returns a null value when the cache is empty.
     */
    protected abstract suspend fun loadCachedData(): DOMAIN_MODEL?

    /**
     * Fetches remote data from the endpoint.
     * @param forced is true when we want to get the fresh data
     */
    protected abstract suspend fun fetchRemoteData(forced: Boolean): State<DOMAIN_MODEL>

    /**
     * Transforms given domain model and ui state into the UI model
     * @param domainModel domain model coming from FluxC
     * @param uiState contains UI specific data
     * @return a list of block list data
     */
    protected abstract fun buildUiModel(domainModel: DOMAIN_MODEL, uiState: UI_STATE): List<BlockListItem>

    protected abstract fun buildLoadingItem(): List<BlockListItem>

    protected open fun buildErrorItem(): List<BlockListItem> {
        return buildLoadingItem() + listOf(
            BlockListItem.Text(
                textResource = R.string.stats_loading_block_error,
                isLast = true
            )
        )
    }

    protected open fun buildEmptyItem(): List<BlockListItem> {
        return buildLoadingItem() + listOf(BlockListItem.Empty(textResource = R.string.stats_no_data_yet))
    }

    private fun updateState() {
        updateJob?.let { job ->
            if (job.isActive) {
                job.cancel()
            }
        }

        updateJob = launch {
            delay(50)
            val currentData = domainModel?.let { buildUiModel(it, uiState) }

            val useCaseModel = try {
                when (domainState) {
                    LOADING -> {
                        UseCaseModel(
                            type,
                            data = currentData,
                            stateData = buildLoadingItem(),
                            state = LOADING
                        )
                    }
                    ERROR -> {
                        UseCaseModel(type, data = currentData, stateData = buildErrorItem(), state = ERROR)
                    }
                    SUCCESS -> {
                        UseCaseModel(type, data = currentData)
                    }
                    EMPTY -> UseCaseModel(type, state = EMPTY, stateData = buildEmptyItem())
                }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.STATS, e)
                UseCaseModel(type, state = ERROR, stateData = buildErrorItem())
            }
            withContext(mainDispatcher) {
                _liveData.value = useCaseModel
            }
        }
    }

    sealed class State<DOMAIN_MODEL> {
        data class Error<DOMAIN_MODEL>(val error: String) : State<DOMAIN_MODEL>()
        data class Data<DOMAIN_MODEL>(val model: DOMAIN_MODEL, val cached: Boolean = false) : State<DOMAIN_MODEL>()
        class Empty<DOMAIN_MODEL> : State<DOMAIN_MODEL>()
        class Loading<DOMAIN_MODEL> : State<DOMAIN_MODEL>()
    }

    data class UseCaseModel(
        val type: StatsType,
        val data: List<BlockListItem>? = null,
        val stateData: List<BlockListItem>? = null,
        val state: UseCaseState = SUCCESS
    ) {
        enum class UseCaseState {
            SUCCESS, ERROR, LOADING, EMPTY
        }
    }

    /**
     * Stateless use case should be used for the blocks that display just plain data.
     * These blocks don't have only one UI state and it doesn't change.
     */
    abstract class StatelessUseCase<DOMAIN_MODEL>(
        type: StatsType,
        mainDispatcher: CoroutineDispatcher,
        backgroundDispatcher: CoroutineDispatcher,
        inputParams: List<UseCaseParam> = listOf()
    ) : BaseStatsUseCase<DOMAIN_MODEL, NotUsedUiState>(
        type,
        mainDispatcher,
        backgroundDispatcher,
        NotUsedUiState,
        inputParams
    ) {
        /**
         * Transforms given domain model into the UI model
         * @param domainModel domain model coming from FluxC
         * @return a list of block list data
         */
        abstract fun buildUiModel(domainModel: DOMAIN_MODEL): List<BlockListItem>

        final override fun buildUiModel(
            domainModel: DOMAIN_MODEL,
            uiState: NotUsedUiState
        ): List<BlockListItem> {
            return buildUiModel(domainModel)
        }

        object NotUsedUiState
    }

    enum class UseCaseMode {
        BLOCK,
        BLOCK_DETAIL,
        VIEW_ALL
    }

    sealed class UseCaseParam {
        data class SelectedDateParam(val statsSection: StatsSection) : UseCaseParam()
    }
}
