package org.wordpress.android.ui.sitecreation.verticals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType
import org.wordpress.android.ui.sitecreation.misc.SiteCreationHeaderUiState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSearchInputUiState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentPromptUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenProgressUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val THROTTLE_DELAY = 500L
private const val CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE = 1000L
private const val ERROR_CONTEXT_LIST_ITEM = "verticals_list_item"
private const val ERROR_CONTEXT_FULLSCREEN = "verticals_fullscreen"

class SiteCreationVerticalsViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchSegmentPromptUseCase: FetchSegmentPromptUseCase,
    private val fetchVerticalsUseCase: FetchVerticalsUseCase,
    private val tracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    private var fetchVerticalsJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false

    private val _uiState: MutableLiveData<VerticalsUiState> = MutableLiveData()
    val uiState: LiveData<VerticalsUiState> = _uiState

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
        job.cancel()
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
                withContext(mainDispatcher) {
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
            tracker.trackErrorShown(ERROR_CONTEXT_FULLSCREEN, SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR)
            withContext(mainDispatcher) {
                updateUiState(VerticalsFullscreenErrorUiState.VerticalsConnectionErrorUiState)
            }
        }
    }

    private fun onSegmentsPromptFetched(event: OnSegmentPromptFetched) {
        if (event.isError) {
            tracker.trackErrorShown(ERROR_CONTEXT_FULLSCREEN, event.error.type.toString(), event.error.message)
            updateUiState(VerticalsFullscreenErrorUiState.VerticalsGenericErrorUiState)
        } else {
            segmentPrompt = event.prompt!!
            updateUiStateToContent("")
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
            updateUiStateToContent(query)
        }
    }

    private fun fetchVerticals(query: String) {
        if (networkUtils.isNetworkAvailable()) {
            updateUiStateToContent(query)
            fetchVerticalsJob = launch {
                delay(THROTTLE_DELAY)
                val fetchedVerticals = fetchVerticalsUseCase.fetchVerticals(query)
                withContext(mainDispatcher) {
                    onVerticalsFetched(query, fetchedVerticals)
                }
            }
        } else {
            showConnectionErrorWithDelay(query)
        }
    }

    private fun showConnectionErrorWithDelay(query: String) {
        updateUiStateToContent(query)
        launch {
            // We show the loading indicator for a bit so the user has some feedback when they press retry
            delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
            tracker.trackErrorShown(ERROR_CONTEXT_LIST_ITEM, SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR)
            withContext(mainDispatcher) {
                updateUiStateToContent(
                        query
                )
            }
        }
    }

    private fun onVerticalsFetched(query: String, event: OnVerticalsFetched) {
        if (event.isError) {
            tracker.trackErrorShown(ERROR_CONTEXT_LIST_ITEM, event.error.type.toString(), event.error.message)
            updateUiStateToContent(
                    query
            )
        } else {
            updateUiStateToContent(query)
        }
    }

    private fun updateUiStateToContent(query: String) {
        updateUiState(
                VerticalsContentUiState(
                        showSkipButton = StringUtils.isEmpty(query),
                        headerUiState = createHeaderUiState(
                                shouldShowHeader(query),
                                segmentPrompt
                        ),
                        searchInputUiState = createSearchInputUiState(
                                query,
                                showProgress = false,
                                showDivider = false,
                                hint = segmentPrompt.hint
                        )
                )
        )
    }

    private fun updateUiState(uiState: VerticalsUiState) {
        _uiState.value = uiState
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
        showDivider: Boolean,
        hint: String
    ): SiteCreationSearchInputUiState {
        return SiteCreationSearchInputUiState(
                UiStringText(hint),
                showProgress,
                showClearButton = !StringUtils.isEmpty(query),
                showDivider = showDivider
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
            val showSkipButton: Boolean
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
}
