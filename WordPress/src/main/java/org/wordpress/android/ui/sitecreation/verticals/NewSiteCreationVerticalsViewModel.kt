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
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.models.networkresource.ListState.Ready
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.SiteCreationHeaderUiState
import org.wordpress.android.ui.sitecreation.SiteCreationSearchInputUiState
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentPromptUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsCustomModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenProgressUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

private const val THROTTLE_DELAY: Int = 500
private const val CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE = 1000

class NewSiteCreationVerticalsViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchSegmentPromptUseCase: FetchSegmentPromptUseCase,
    private val fetchVerticalsUseCase: FetchVerticalsUseCase,
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
    private lateinit var segmentPrompt: SegmentPromptModel

    private var segmentId: Long? = null

    private val _clearBtnClicked = SingleLiveEvent<Unit>()
    val clearBtnClicked = _clearBtnClicked

    private val _verticalSelected = SingleLiveEvent<String>()
    val verticalSelected: LiveData<String> = _verticalSelected

    private val _skipBtnClicked = SingleLiveEvent<Unit>()
    val skipBtnClicked: LiveData<Unit> = _skipBtnClicked

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    init {
        dispatcher.register(fetchVerticalsUseCase)
        dispatcher.register(fetchSegmentPromptUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchVerticalsUseCase)
        dispatcher.unregister(fetchSegmentPromptUseCase)
    }

    fun start(segmentId: Long) {
        if (isStarted) {
            return
        }
        this.segmentId = segmentId
        isStarted = true
        fetchSegmentsPrompt()
    }

    private fun fetchSegmentsPrompt() {
        if (networkUtils.isNetworkAvailable()) {
            updateUiState(VerticalsFullscreenProgressUiState)
            launch {
                val onSegmentsPromptFetchedEvent = fetchSegmentPromptUseCase.fetchSegmentsPrompt(segmentId!!)
                withContext(MAIN) {
                    onSegmentsPromptFetched(onSegmentsPromptFetchedEvent)
                }
            }
        } else {
            showFullscreenErrorWithDelay()
        }
    }

    private fun showFullscreenErrorWithDelay() {
        updateUiState(VerticalsFullscreenProgressUiState)
        launch {
            // We show the loading indicator for a bit so the user has some feedback when they press retry
            delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
            withContext(MAIN) {
                updateUiState(VerticalsFullscreenErrorUiState.VerticalsConnectionErrorUiState)
            }
        }
    }

    private fun onSegmentsPromptFetched(event: OnSegmentPromptFetched) {
        if (event.isError) {
            updateUiState(VerticalsFullscreenErrorUiState.VerticalsGenericErrorUiState)
        } else {
            segmentPrompt = event.prompt!!
            updateUiStateToContent("", ListState.Ready(emptyList()))
        }
    }

    fun onFetchSegmentsPromptRetry() {
        fetchSegmentsPrompt()
    }

    fun onClearTextBtnClicked() {
        _clearBtnClicked.call()
    }

    fun onSkipStepBtnClicked() {
        _skipBtnClicked.call()
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    fun updateQuery(query: String) {
        fetchVerticalsJob?.cancel() // cancel any previous requests
        if (query.isNotEmpty()) {
            fetchVerticals(query)
        } else {
            updateUiStateToContent(query, ListState.Ready(emptyList()))
        }
    }

    private fun fetchVerticals(query: String) {
        if (networkUtils.isNetworkAvailable()) {
            updateUiStateToContent(query, Loading(Ready(emptyList()), false))
            fetchVerticalsJob = launch {
                delay(THROTTLE_DELAY)
                val fetchedVerticals = fetchVerticalsUseCase.fetchVerticals(query)
                withContext(MAIN) {
                    onVerticalsFetched(query, fetchedVerticals)
                }
            }
        } else {
            showFullscreenConnectionErrorWithDelay(query)
        }
    }

    private fun showFullscreenConnectionErrorWithDelay(query: String) {
        updateUiStateToContent(query, Loading(Ready(emptyList()), false))
        launch {
            // We show the loading indicator for a bit so the user has some feedback when they press retry
            delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
            withContext(MAIN) {
                updateUiStateToContent(
                        query,
                        Error(
                                listState,
                                errorMessageResId = R.string.site_creation_fetch_suggestions_error_no_connection
                        )
                )
            }
        }
    }

    private fun onVerticalsFetched(query: String, event: OnVerticalsFetched) {
        if (event.isError) {
            updateUiStateToContent(
                    query,
                    ListState.Error(
                            listState,
                            errorMessageResId = R.string.site_creation_fetch_suggestions_error_unknown
                    )
            )
        } else {
            updateUiStateToContent(query, ListState.Success(event.verticalList))
        }
    }

    private fun updateUiStateToContent(query: String, state: ListState<VerticalModel>) {
        listState = state
        updateUiState(
                VerticalsContentUiState(
                        showSkipButton = StringUtils.isEmpty(query),
                        headerUiState = createHeaderUiState(
                                shouldShowHeader(query),
                                segmentPrompt
                        ),
                        searchInputUiState = createSearchInputUiState(
                                query,
                                showProgress = state is Loading,
                                hint = segmentPrompt.hint
                        ),
                        items = createSuggestionsUiStates(
                                onRetry = { updateQuery(query) },
                                data = state.data,
                                errorFetchingSuggestions = state is Error,
                                errorResId = if (state is Error) state.errorMessageResId else null
                        )
                )
        )
    }

    private fun updateUiState(uiState: VerticalsUiState) {
        _uiState.value = uiState
    }

    private fun createSuggestionsUiStates(
        onRetry: () -> Unit,
        data: List<VerticalModel>,
        errorFetchingSuggestions: Boolean,
        @StringRes errorResId: Int?
    ): List<VerticalsListItemUiState> {
        val items: ArrayList<VerticalsListItemUiState> = ArrayList()
        if (errorFetchingSuggestions) {
            val errorUiState = VerticalsFetchSuggestionsErrorUiState(
                    messageResId = errorResId ?: R.string.site_creation_fetch_suggestions_error_unknown,
                    retryButtonResId = R.string.button_retry
            )
            errorUiState.onItemTapped = onRetry
            items.add(errorUiState)
        } else {
            val lastItemIndex = data.size - 1
            data.forEachIndexed { index, model ->
                if (model.isNewUserVertical) {
                    val itemUiState = VerticalsCustomModelUiState(
                            model.verticalId,
                            model.name,
                            R.string.new_site_creation_verticals_custom_subtitle,
                            showDivider = index != lastItemIndex
                    )
                    itemUiState.onItemTapped = { _verticalSelected.value = itemUiState.id }
                    items.add(itemUiState)
                } else {
                    val itemUiState = VerticalsModelUiState(
                            model.verticalId,
                            model.name,
                            showDivider = index != lastItemIndex
                    )
                    itemUiState.onItemTapped = { _verticalSelected.value = itemUiState.id }
                    items.add(itemUiState)
                }
            }
        }
        return items
    }

    private fun shouldShowHeader(query: String): Boolean {
        return StringUtils.isEmpty(query)
    }

    private fun createHeaderUiState(
        isVisible: Boolean,
        segmentsPrompt: SegmentPromptModel
    ): SiteCreationHeaderUiState? {
        return if (isVisible) SiteCreationHeaderUiState(
                UiStringText(segmentsPrompt.title),
                UiStringText(segmentsPrompt.subtitle)
        ) else null
    }

    private fun createSearchInputUiState(
        query: String,
        showProgress: Boolean,
        hint: String
    ): SiteCreationSearchInputUiState {
        return SiteCreationSearchInputUiState(
                UiStringText(hint),
                showProgress,
                showClearButton = !StringUtils.isEmpty(query)
        )
    }

    sealed class VerticalsUiState(
        val fullscreenProgressLayoutVisibility: Boolean,
        val contentLayoutVisibility: Boolean,
        val fullscreenErrorLayoutVisibility: Boolean
    ) {
        data class VerticalsContentUiState(
            val searchInputUiState: SiteCreationSearchInputUiState,
            val headerUiState: SiteCreationHeaderUiState?,
            val showSkipButton: Boolean,
            val items: List<VerticalsListItemUiState>
        ) : VerticalsUiState(
                fullscreenProgressLayoutVisibility = false,
                contentLayoutVisibility = true,
                fullscreenErrorLayoutVisibility = false
        )

        object VerticalsFullscreenProgressUiState : VerticalsUiState(
                fullscreenProgressLayoutVisibility = true,
                contentLayoutVisibility = false,
                fullscreenErrorLayoutVisibility = false
        )

        sealed class VerticalsFullscreenErrorUiState constructor(
            val titleResId: Int,
            val subtitleResId: Int? = null
        ) : VerticalsUiState(
                fullscreenProgressLayoutVisibility = false,
                contentLayoutVisibility = false,
                fullscreenErrorLayoutVisibility = true
        ) {
            object VerticalsGenericErrorUiState : VerticalsFullscreenErrorUiState(
                    R.string.site_creation_error_generic_title,
                    R.string.site_creation_error_generic_subtitle
            )

            object VerticalsConnectionErrorUiState : VerticalsFullscreenErrorUiState(
                    R.string.no_network_message
            )
        }
    }

    sealed class VerticalsListItemUiState {
        var onItemTapped: (() -> Unit)? = null

        data class VerticalsModelUiState(val id: String, val title: String, val showDivider: Boolean) :
                VerticalsListItemUiState()

        data class VerticalsCustomModelUiState(
            val id: String,
            val title: String,
            @StringRes val subTitleResId: Int,
            val showDivider: Boolean
        ) : VerticalsListItemUiState()

        data class VerticalsFetchSuggestionsErrorUiState(
            @StringRes val messageResId: Int,
            @StringRes val retryButtonResId: Int
        ) : VerticalsListItemUiState()
    }
}
