package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.apache.commons.lang3.StringUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.usecases.DummyOnVerticalsHeaderInfoFetched
import org.wordpress.android.ui.sitecreation.usecases.DummyVerticalsHeaderInfoModel
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsHeaderInfoUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

private const val throttleDelay: Int = 500

class NewSiteCreationVerticalsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val fetchVerticalsHeaderInfoUseCase: FetchVerticalsHeaderInfoUseCase,
    private val fetchVerticalsUseCase: FetchVerticalsUseCase,
    private val resultObservable: NewSiteCreationVerticalsResultObservable,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext
) : ViewModel(), CoroutineScope {
    private val job = Job()
    private var fetchVerticalsJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() = IO + job
    private var isStarted = false

    private val _uiState: MutableLiveData<VerticalsUiState> = MutableLiveData()
    val uiState: LiveData<VerticalsUiState> = _uiState

    private var listState: ListState<VerticalModel> = ListState.Init()
    private lateinit var headerInfo: DummyVerticalsHeaderInfoModel

    private val _clearBtnClicked = SingleLiveEvent<Void>()
    val clearBtnClicked = _clearBtnClicked

    init {
        dispatcher.register(fetchVerticalsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchVerticalsUseCase)
    }

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        fetchHeaderInfo()
    }

    private fun fetchHeaderInfo() {
        launch {
            withContext(MAIN) {
                updateUiStateToFullScreenProgress()
            }
            val headerInfoEvent = fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()
            withContext(MAIN) {
                onHeaderInfoFetched(headerInfoEvent)
            }
        }
    }

    private fun onHeaderInfoFetched(event: DummyOnVerticalsHeaderInfoFetched) {
        if (event.isError) {
            updateUiStateToFullScreenError()
        } else {
            headerInfo = event.headerInfo!!
            updateUiStateToContent("", ListState.Ready(emptyList()))
        }
    }

    fun onFetchHeaderInfoRetry() {
        fetchHeaderInfo()
    }

    fun onClearTextBtnClicked() {
        _clearBtnClicked.call()
    }

    fun updateQuery(query: String, delay: Int = throttleDelay) {
        fetchVerticalsJob?.cancel() // cancel any previous requests
        if (query.isNotEmpty()) {
            fetchVerticals(query, delay)
        } else {
            updateUiStateToContent(query, ListState.Ready(emptyList()))
        }
    }

    private fun fetchVerticals(query: String, throttleDelay: Int) {
        fetchVerticalsJob = launch {
            withContext(MAIN) {
                updateUiStateToContent(query, ListState.Loading(ListState.Ready(emptyList()), false))
            }
            delay(throttleDelay)
            val fetchedVerticals = fetchVerticalsUseCase.fetchVerticals(query)
            withContext(MAIN) {
                onVerticalsFetched(query, fetchedVerticals)
            }
        }
    }

    private fun onVerticalsFetched(query: String, event: OnVerticalsFetched) {
        if (event.isError) {
            updateUiStateToContent(query, ListState.Error(listState, event.error.message))
        } else {
            updateUiStateToContent(query, ListState.Success(event.verticalList))
        }
    }

    private fun updateUiStateToFullScreenProgress() {
        _uiState.value = VerticalsUiState(
                showFullscreenError = false,
                showFullscreenProgress = true,
                showContent = false,
                showSkipButton = false,
                items = emptyList()
        )
    }

    private fun updateUiStateToFullScreenError() {
        _uiState.value = VerticalsUiState(
                showFullscreenError = true,
                showFullscreenProgress = false,
                showContent = false,
                showSkipButton = false,
                items = emptyList()
        )
    }

    private fun updateUiStateToContent(query: String, state: ListState<VerticalModel>) {
        listState = state
        _uiState.value = VerticalsUiState(
                showFullscreenError = false,
                showFullscreenProgress = false,
                showContent = true,
                showSkipButton = StringUtils.isEmpty(query),
                headerUiState = createHeaderUiState(shouldShowHeader(query), headerInfo),
                searchInputState = createSearchInputUiState(
                        query,
                        showProgress = state is Loading,
                        hint = headerInfo.inputHint
                ),
                items = createSuggestionsUiStates(
                        onRetry = { updateQuery(query) },
                        data = state.data,
                        errorFetchingSuggestions = state is Error
                )
        )
    }

    private fun createSuggestionsUiStates(
        onRetry: () -> Unit,
        data: List<VerticalModel>,
        errorFetchingSuggestions: Boolean
    ): List<VerticalsListItemUiState> {
        val items: ArrayList<VerticalsListItemUiState> = ArrayList()
        if (errorFetchingSuggestions) {
            val errorUiState = VerticalsFetchSuggestionsErrorUiState(
                    messageResId = R.string.site_creation_fetch_suggestions_failed,
                    retryButonResId = R.string.button_retry
            )
            errorUiState.onItemTapped = onRetry
            items.add(errorUiState)
        } else {
            val lastItemIndex = data.size - 1
            data.forEachIndexed { index, model ->
                val itemUiState = VerticalsModelUiState(
                        model.verticalId,
                        model.name,
                        showDivider = index != lastItemIndex
                )
                itemUiState.onItemTapped = { resultObservable.selectedVertical.value = itemUiState.id }
                items.add(itemUiState)
            }
        }
        return items
    }

    private fun shouldShowHeader(query: String): Boolean {
        return StringUtils.isEmpty(query)
    }

    private fun createHeaderUiState(
        isVisible: Boolean,
        headerInfo: DummyVerticalsHeaderInfoModel
    ): VerticalsHeaderUiState {
        return VerticalsHeaderUiState(isVisible, headerInfo.title, headerInfo.subtitle)
    }

    private fun createSearchInputUiState(
        query: String,
        showProgress: Boolean,
        hint: String
    ): VerticalsSearchInputUiState {
        return VerticalsSearchInputUiState(
                hint,
                showProgress,
                showClearButton = !StringUtils.isEmpty(query)
        )
    }

    data class VerticalsUiState(
        val showFullscreenError: Boolean,
        val showFullscreenProgress: Boolean,
        val showSkipButton: Boolean,
        val showContent: Boolean,
        val headerUiState: VerticalsHeaderUiState? = null,
        val searchInputState: VerticalsSearchInputUiState? = null,
        val items: List<VerticalsListItemUiState>
    )

    data class VerticalsSearchInputUiState(
        val hint: String,
        val showProgress: Boolean,
        val showClearButton: Boolean
    )

    data class VerticalsHeaderUiState(
        val isVisible: Boolean,
        val title: String,
        val subtitle: String
    )

    sealed class VerticalsListItemUiState {
        lateinit var onItemTapped: () -> Unit

        data class VerticalsModelUiState(val id: String, val title: String, val showDivider: Boolean) :
                VerticalsListItemUiState()

        data class VerticalsFetchSuggestionsErrorUiState(
            @StringRes val messageResId: Int,
            @StringRes val retryButonResId: Int
        ) : VerticalsListItemUiState() {
        }
    }
}
