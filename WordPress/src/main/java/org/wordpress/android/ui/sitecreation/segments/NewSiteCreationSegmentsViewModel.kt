package org.wordpress.android.ui.sitecreation.segments

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.segments.ItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.ItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.ItemUiState.SegmentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsErrorUiState.ConnectionError
import org.wordpress.android.ui.sitecreation.segments.SegmentsErrorUiState.GenericError
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

private const val FAKE_DELAY = 1000

class NewSiteCreationSegmentsViewModel
@Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchSegmentsUseCase: FetchSegmentsUseCase,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext
) : ViewModel(), CoroutineScope {
    private val fetchCategoriesJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + fetchCategoriesJob

    private var isStarted = false
    /* Should be updated only within updateUIState(). */
    private var listState: ListState<VerticalSegmentModel> = ListState.Ready(emptyList())

    private val _segmentsUiState: MutableLiveData<SegmentsUiState> = MutableLiveData()
    val segmentsUiState: LiveData<SegmentsUiState> = _segmentsUiState

    private val _segmentSelected = SingleLiveEvent<Long>()
    val segmentSelected: LiveData<Long> = _segmentSelected

    fun start() {
        if (isStarted) return
        isStarted = true
        AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_CATEGORY_VIEWED)
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
            launch {
                withContext(MAIN) {
                    updateUiStateToContent(ListState.Loading(listState))
                }
                val event = fetchSegmentsUseCase.fetchCategories()
                withContext(MAIN) {
                    onCategoriesFetched(event)
                }
            }
        } else {
            launch {
                withContext(MAIN) {
                    updateUiStateToContent(ListState.Loading(listState))
                }
                // We show the loading screen for a bit so the user has some feedback when they press the retry button
                delay(FAKE_DELAY)
                withContext(MAIN) {
                    updateUiStateToError(ListState.Error(listState, null), ConnectionError)
                }
            }
        }
    }

    private fun onCategoriesFetched(event: OnSegmentsFetched) {
        if (event.isError) {
            updateUiStateToError(ListState.Error(listState, event.error.message), GenericError)
        } else {
            updateUiStateToContent(ListState.Success(event.segmentList))
        }
    }

    fun onRetryClicked() {
        fetchCategories()
    }

    private fun onSegmentSelected(segmentId: Long) {
        _segmentSelected.value = segmentId
    }

    // TODO analytics

    private fun updateUiStateToError(state: ListState<VerticalSegmentModel>, segmentError: SegmentsErrorUiState) {
        updateUiState(state)
        _segmentsUiState.value = SegmentsUiState(segmentError)
    }

    private fun updateUiStateToContent(state: ListState<VerticalSegmentModel>) {
        updateUiState(state)
        _segmentsUiState.value = SegmentsUiState(
                SegmentsContentUiState.Visible(
                        createUiStatesForItems(
                                showProgress = state is Loading,
                                segments = state.data
                        )
                )
        )
    }

    private fun updateUiState(state: ListState<VerticalSegmentModel>) {
        listState = state
    }

    private fun createUiStatesForItems(
        showProgress: Boolean,
        segments: List<VerticalSegmentModel>
    ): List<ItemUiState> {
        val items: ArrayList<ItemUiState> = ArrayList()
        addHeader(items)
        if (showProgress) {
            addProgress(items)
        }
        addSegments(segments, items)
        return items
    }

    private fun addHeader(items: ArrayList<ItemUiState>) {
        items.add(HeaderUiState)
    }

    private fun addProgress(items: ArrayList<ItemUiState>) {
        items.add(ProgressUiState)
    }

    private fun addSegments(
        segments: List<VerticalSegmentModel>,
        items: ArrayList<ItemUiState>
    ) {
        val segmentsCount = segments.size
        segments.forEachIndexed { index, model ->
            val isLastItem = segmentsCount - 1 == index
            val segment = SegmentUiState(
                    model.segmentId,
                    model.title,
                    model.subtitle,
                    model.iconUrl,
                    showDivider = !isLastItem
            )
            segment.onItemTapped = { onSegmentSelected(model.segmentId) }
            items.add(segment)
        }
    }
}

class SegmentsUiState {
    val contentUiStateState: SegmentsContentUiState
    val errorUiStateState: SegmentsErrorUiState

    constructor(errorUiStateState: SegmentsErrorUiState) {
        this.contentUiStateState = SegmentsContentUiState.Hidden
        this.errorUiStateState = errorUiStateState
    }

    constructor(contentUiStateState: SegmentsContentUiState) {
        this.contentUiStateState = contentUiStateState
        this.errorUiStateState = SegmentsErrorUiState.Hidden
    }
}

sealed class SegmentsContentUiState(val visible: Boolean) {
    open val items: List<ItemUiState> = emptyList()

    internal object Hidden : SegmentsContentUiState(false)
    internal data class Visible(override val items: List<ItemUiState>) : SegmentsContentUiState(true)
}

sealed class SegmentsErrorUiState(val visible: Boolean) {
    open val titleResId: Int = R.string.empty
    open val subtitleVisible = false
    open val subtitleResId: Int = R.string.empty

    internal object Hidden : SegmentsErrorUiState(false)
    internal object GenericError : SegmentsErrorUiState(true) {
        override val titleResId: Int = R.string.site_creation_error_generic_title
        override val subtitleResId: Int = R.string.site_creation_error_generic_subtitle
        override val subtitleVisible: Boolean = true
    }

    internal object ConnectionError : SegmentsErrorUiState(true) {
        override val titleResId: Int = R.string.site_creation_error_connection_title
    }
}

sealed class ItemUiState {
    object HeaderUiState : ItemUiState() {
        const val titleResId: Int = R.string.site_creation_segments_title
        const val subtitleResId: Int = R.string.site_creation_segments_subtitle
    }

    object ProgressUiState : ItemUiState()

    data class SegmentUiState(
        val segmentId: Long,
        val title: String,
        val subtitle: String,
        val iconUrl: String,
        val showDivider: Boolean
    ) : ItemUiState() {
        var onItemTapped: (() -> Unit)? = null
    }
}
