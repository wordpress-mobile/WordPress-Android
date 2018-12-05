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
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.SegmentUiState
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
                    updateUiStateToError(
                            ListState.Error(listState, null),
                            SegmentsErrorUiState.createConnectionErrorUiState()
                    )
                }
            }
        }
    }

    private fun onCategoriesFetched(event: OnSegmentsFetched) {
        if (event.isError) {
            updateUiStateToError(
                    ListState.Error(listState, event.error.message),
                    SegmentsErrorUiState.createGenericErrorUiState()
            )
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
            segment.onItemTapped = { onSegmentSelected(model.segmentId) }
            items.add(segment)
        }
    }
}

interface SegmentsUiState
data class SegmentsContentUiState(val items: List<SegmentsItemUiState>) : SegmentsUiState
data class SegmentsErrorUiState constructor(val titleResId: Int, val subtitleResId: Int? = null) : SegmentsUiState {
    companion object {
        fun createGenericErrorUiState() = SegmentsErrorUiState(
                R.string.site_creation_error_generic_title,
                R.string.site_creation_error_generic_subtitle
        )

        fun createConnectionErrorUiState() = SegmentsErrorUiState(R.string.site_creation_error_connection_title)
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
