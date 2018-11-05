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
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ListState.DONE
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ListState.ERROR
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ListState.FETCHING
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ListState.PREINIT
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
    val fetchCategoriesJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + fetchCategoriesJob

    private var isStarted = false
    /* Should be updated only within updateUIState() */
    private var listState: ListState = PREINIT

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        if (isStarted) return
        isStarted = true
        AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_CATEGORY_VIEWED)
        fetchCategories()
    }

    private fun fetchCategories() {
        if (listState == FETCHING) return
        launch {
            withContext(MAIN) {
                updateUIState(FETCHING)
            }
            val event = fetchSegmentsUseCase.fetchCategories()
            withContext(MAIN) {
                onCategoriesFetched(event)
            }
        }
    }

    private fun onCategoriesFetched(event: OnSegmentsFetched) {
        if (event.isError) {
            // TODO handle error
            updateUIState(ERROR)
        } else {
            updateUIState(DONE, event.segmentList)
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

    private fun updateUIState(state: ListState, data: List<VerticalSegmentModel> = emptyList()) {
        listState = state
        _uiState.value = UiState(
                showProgress = state == FETCHING,
                showError = state == ERROR,
                showList = state == DONE,
                showHeader = state == FETCHING || state == DONE,
                data = data
        )
    }

    private enum class ListState {
        PREINIT,
        FETCHING,
        ERROR,
        DONE
    }

    data class UiState(
        val showProgress: Boolean,
        val showError: Boolean,
        val showList: Boolean,
        val showHeader: Boolean,
        val data: List<VerticalSegmentModel>
    )
}
