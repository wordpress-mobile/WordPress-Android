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

    private val _categories: MutableLiveData<List<VerticalSegmentModel>> = MutableLiveData()
    val categories: LiveData<List<VerticalSegmentModel>> = _categories

    private val _showProgress: MutableLiveData<Boolean> = MutableLiveData()
    val showProgress: LiveData<Boolean> = _showProgress

    private val _showError: MutableLiveData<Boolean> = MutableLiveData()
    val showError: LiveData<Boolean> = _showError

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
            _categories.value = event.segmentList
            updateUIState(DONE)
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

    private fun updateUIState(state: ListState) {
        listState = state
        updateIfChanged(_showProgress, state == FETCHING)
        updateIfChanged(_showError, state == ERROR)
    }

    private fun updateIfChanged(liveData: MutableLiveData<Boolean>, newValue: Boolean) {
        if (liveData.value != newValue) {
            liveData.value = newValue
        }
    }

    private enum class ListState {
        PREINIT,
        FETCHING,
        ERROR,
        DONE
    }
}
