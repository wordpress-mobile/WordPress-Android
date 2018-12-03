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
import org.wordpress.android.ui.sitecreation.errors.SiteCreationErrorUiState
import org.wordpress.android.ui.sitecreation.errors.SiteCreationErrorUiState.ConnectionError
import org.wordpress.android.ui.sitecreation.errors.SiteCreationErrorUiState.GenericError
import org.wordpress.android.ui.sitecreation.segments.ItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.ItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.ItemUiState.SegmentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsContent.Visible
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsContentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsErrorUiState
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

    private fun updateUiStateToError(state: ListState<VerticalSegmentModel>, segmentError: SiteCreationErrorUiState) {
        updateUiState(state)
        _segmentsUiState.value = SegmentsErrorUiState(segmentError)
    }

    private fun updateUiStateToContent(state: ListState<VerticalSegmentModel>) {
        updateUiState(state)
        _segmentsUiState.value = SegmentsContentUiState(
                Visible(
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

sealed class SegmentsUiState {
    open val segmentsError: SiteCreationErrorUiState = SiteCreationErrorUiState.Hidden
    open val segmentsContent: SegmentsContent = SegmentsContent.Hidden

    data class SegmentsErrorUiState(override val segmentsError: SiteCreationErrorUiState) : SegmentsUiState()
    data class SegmentsContentUiState(override val segmentsContent: SegmentsContent) : SegmentsUiState()
}

sealed class SegmentsContent(val isVisible: Boolean) {
    open val items: List<ItemUiState> = emptyList()

    object Hidden : SegmentsContent(false)
    data class Visible(override val items: List<ItemUiState>) : SegmentsContent(true)
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
