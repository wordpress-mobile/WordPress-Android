package org.wordpress.android.ui.sitecreation.segments

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.models.networkresource.ListState.Success
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

class NewSiteCreationSegmentsViewModel
@Inject constructor(
    private val dispatcher: Dispatcher,
    private val fetchSegmentsUseCase: FetchSegmentsUseCase,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineDispatcher,
    @Named(IO_DISPATCHER) private val IO: CoroutineDispatcher
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

    private fun fetchCategories() {
        if (!listState.shouldFetch(loadMore = false)) throw IllegalStateException("Fetch already in progress.")
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

    fun onSegmentSelected(segmentId: Int) {
        // TODO send result to the SCMainVM
    }

    init {
        dispatcher.register(fetchSegmentsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        fetchCategoriesJob.cancel() // cancels all coroutines with the default coroutineContext
        dispatcher.unregister(fetchSegmentsUseCase)
    }

    // TODO analytics

    private fun updateUIState(state: ListState<VerticalSegmentModel>) {
        listState = state
        _uiState.value = UiState(
                showProgress = state is Loading,
                showError = state is Error,
                showList = state is Success,
                showHeader = state is Loading || state is Success,
                data = state.data
        )
    }

    data class UiState(
        val showProgress: Boolean = false,
        val showError: Boolean = false,
        val showList: Boolean = false,
        val showHeader: Boolean = false,
        val data: List<VerticalSegmentModel> = emptyList()
    )
}
