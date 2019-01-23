package org.wordpress.android.ui.sitecreation.segments

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationErrorType
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.SegmentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsContentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsErrorUiState
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE = 1000L
private const val ERROR_CONTEXT = "segments"

class NewSiteCreationSegmentsViewModel
@Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchSegmentsUseCase: FetchSegmentsUseCase,
    private val tracker: NewSiteCreationTracker,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val fetchCategoriesJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + fetchCategoriesJob

    private var isStarted = false
    /* Should be updated only within updateUIState(). */
    private var listState: ListState<VerticalSegmentModel> = ListState.Ready(emptyList())

    private val _segmentsUiState: MutableLiveData<SegmentsUiState> = MutableLiveData()
    val segmentsUiState: LiveData<SegmentsUiState> = _segmentsUiState

    private val _segmentSelected = SingleLiveEvent<Long>()
    val segmentSelected: LiveData<Long> = _segmentSelected

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    fun start() {
        if (isStarted) return
        isStarted = true
        fetchCategories()
    }

    init {
        dispatcher.register(fetchSegmentsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        fetchCategoriesJob.cancel() // cancels all coroutines with the default coroutineContext
        dispatcher.unregister(fetchSegmentsUseCase)
    }

    private fun fetchCategories() {
        if (!listState.shouldFetch(loadMore = false)) {
            if (BuildConfig.DEBUG) {
                throw IllegalStateException("Fetch already in progress.")
            } else {
                return
            }
        }
        if (networkUtils.isNetworkAvailable()) {
            updateUiStateToContent(ListState.Loading(listState))
            launch {
                val event = fetchSegmentsUseCase.fetchCategories()
                withContext(mainDispatcher) {
                    onCategoriesFetched(event)
                }
            }
        } else {
            updateUiStateToContent(ListState.Loading(listState))
            launch {
                // We show the loading screen for a bit so the user has some feedback when they press the retry button
                delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
                tracker.trackErrorShown(ERROR_CONTEXT, NewSiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR)
                withContext(mainDispatcher) {
                    updateUiStateToError(
                            ListState.Error(listState, null),
                            SegmentsErrorUiState.SegmentsConnectionErrorUiState
                    )
                }
            }
        }
    }

    private fun onCategoriesFetched(event: OnSegmentsFetched) {
        if (event.isError) {
            tracker.trackErrorShown(
                    ERROR_CONTEXT,
                    event.error.type.toString(),
                    event.error.message
            )
            updateUiStateToError(
                    ListState.Error(listState, event.error.message),
                    SegmentsErrorUiState.SegmentsGenericErrorUiState
            )
        } else {
            tracker.trackSegmentsViewed()
            val segments = event.segmentList.filter { it.isMobileSegment }
            updateUiStateToContent(ListState.Success(segments))
        }
    }

    fun onRetryClicked() {
        fetchCategories()
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    private fun onSegmentSelected(segmentTitle: String, segmentId: Long) {
        tracker.trackSegmentSelected(segmentTitle, segmentId)
        _segmentSelected.value = segmentId
    }

    private fun updateUiStateToError(state: ListState<VerticalSegmentModel>, segmentError: SegmentsErrorUiState) {
        listState = state
        _segmentsUiState.value = segmentError
    }

    private fun updateUiStateToContent(state: ListState<VerticalSegmentModel>) {
        listState = state
        _segmentsUiState.value = SegmentsContentUiState(
                createUiStatesForItems(
                        showProgress = state is Loading,
                        segments = state.data
                )
        )
    }

    private fun createUiStatesForItems(
        showProgress: Boolean,
        segments: List<VerticalSegmentModel>
    ): List<SegmentsItemUiState> {
        val items: ArrayList<SegmentsItemUiState> = ArrayList()
        addHeader(items)
        if (showProgress) {
            addProgress(items)
        }
        addSegments(segments, items)
        return items
    }

    private fun addHeader(items: ArrayList<SegmentsItemUiState>) {
        items.add(HeaderUiState)
    }

    private fun addProgress(items: ArrayList<SegmentsItemUiState>) {
        items.add(ProgressUiState)
    }

    private fun addSegments(
        segments: List<VerticalSegmentModel>,
        items: ArrayList<SegmentsItemUiState>
    ) {
        val segmentsCount = segments.size
        segments.forEachIndexed { index, model ->
            val isLastItem = segmentsCount - 1 == index
            val segment = SegmentUiState(
                    model.segmentId,
                    model.title,
                    model.subtitle,
                    model.iconUrl,
                    model.iconColor,
                    showDivider = !isLastItem
            )
            segment.onItemTapped = { onSegmentSelected(model.title, model.segmentId) }
            items.add(segment)
        }
    }
}

sealed class SegmentsUiState {
    data class SegmentsContentUiState(val items: List<SegmentsItemUiState>) : SegmentsUiState()
    sealed class SegmentsErrorUiState constructor(
        val titleResId: Int,
        val subtitleResId: Int? = null
    ) : SegmentsUiState() {
        object SegmentsGenericErrorUiState : SegmentsErrorUiState(
                R.string.site_creation_error_generic_title,
                R.string.site_creation_error_generic_subtitle
        )

        object SegmentsConnectionErrorUiState : SegmentsErrorUiState(R.string.no_network_message)
    }
}

sealed class SegmentsItemUiState {
    object HeaderUiState : SegmentsItemUiState() {
        const val titleResId: Int = R.string.site_creation_segments_title
        const val subtitleResId: Int = R.string.site_creation_segments_subtitle
    }

    object ProgressUiState : SegmentsItemUiState()

    data class SegmentUiState(
        val segmentId: Long,
        val title: String,
        val subtitle: String,
        val iconUrl: String,
        val iconColor: String,
        val showDivider: Boolean
    ) : SegmentsItemUiState() {
        var onItemTapped: (() -> Unit)? = null
    }
}
