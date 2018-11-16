package org.wordpress.android.ui.sitecreation.segments

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ItemUiState.SegmentUiState
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

class NewSiteCreationSegmentsViewModel
@Inject constructor(
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

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

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
        launch {
            withContext(MAIN) {
                updateUIState(ListState.Loading(listState))
            }
            val event = fetchSegmentsUseCase.fetchCategories()
            withContext(MAIN) {
                onCategoriesFetched(event)
            }
        }
    }

    private fun onCategoriesFetched(event: OnSegmentsFetched) {
        if (event.isError) {
            updateUIState(ListState.Error(listState, event.error.message))
        } else {
            updateUIState(ListState.Success(event.segmentList))
        }
    }

    fun onRetryClicked() {
        fetchCategories()
    }

    fun onSegmentSelected(segmentId: Long) {
        // TODO send result to the SCMainVM
    }

    // TODO analytics

    private fun updateUIState(state: ListState<VerticalSegmentModel>) {
        listState = state
        _uiState.value = UiState(
                showError = state is Error,
                showContent = state !is Error,
                items = if (state is Error)
                    emptyList()
                else
                    createUiStatesForItems(showProgress = state is Loading, segments = state.data)
        )
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

    data class UiState(
        val showError: Boolean,
        val showContent: Boolean,
        val items: List<ItemUiState>
    )

    sealed class ItemUiState {
        object HeaderUiState : ItemUiState() {
            val titleResId: Int = R.string.site_creation_segments_title
            val subtitleResId: Int = R.string.site_creation_segments_subtitle
        }

        object ProgressUiState : ItemUiState()

        data class SegmentUiState(
            val segmentId: Long,
            val title: String,
            val subtitle: String,
            val iconUrl: String,
            val showDivider: Boolean
        ) : ItemUiState() {
            lateinit var onItemTapped: (Long) -> Unit
        }
    }
}
