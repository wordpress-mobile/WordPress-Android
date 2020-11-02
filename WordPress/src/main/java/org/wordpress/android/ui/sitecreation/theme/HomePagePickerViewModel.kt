package org.wordpress.android.ui.sitecreation.theme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.StarterDesignModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class HomePagePickerViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val fetchHomePageLayoutsJob = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + fetchHomePageLayoutsJob

    private lateinit var layouts: List<StarterDesignModel>

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    init {
        dispatcher.register(fetchHomePageLayoutsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        fetchHomePageLayoutsJob.cancel()
        dispatcher.unregister(fetchHomePageLayoutsUseCase)
    }

    fun start() {
        fetchLayouts()
    }

    private fun fetchLayouts() {
        updateUiState(UiState.Loading)
        launch {
            val event = fetchHomePageLayoutsUseCase.fetchStarterDesigns()
            withContext(mainDispatcher) {
                if (event.isError) {
                    updateUiState(UiState.Error)
                } else {
                    layouts = event.designs
                    loadLayouts()
                }
            }
        }
    }

    private fun loadLayouts() {
        val state = uiState.value as? UiState.Content ?: UiState.Content()
        launch(bgDispatcher) {
            val designs = layouts.filter { it.slug != null && it.screenshot != null }.map {
                LayoutGridItemUiState(
                        slug = it.slug!!,
                        title = it.title ?: "",
                        preview = it.screenshot!!,
                        selected = it.slug == state.selectedLayoutSlug,
                        onItemTapped = { onLayoutTapped(layoutSlug = it.slug!!) },
                        onThumbnailReady = { onThumbnailReady(layoutSlug = it.slug!!) }
                )
            }
            withContext(mainDispatcher) {
                updateUiState(state.copy(layouts = designs))
            }
        }
    }

    /**
     * Appbar scrolled event
     * @param verticalOffset the scroll state vertical offset
     * @param scrollThreshold the scroll threshold
     */
    fun onAppBarOffsetChanged(verticalOffset: Int, scrollThreshold: Int) {
        setHeaderTitleVisibility(verticalOffset < scrollThreshold)
    }

    fun onPreviewTapped() {
        // TODO
    }

    fun onChooseTapped() {
        // TODO
    }

    fun onSkippedTapped() {
        // TODO
    }

    fun onBackPressed() {
        // TODO
    }

    fun onRetryClicked() {
        if (networkUtils.isNetworkAvailable()) {
            fetchLayouts()
        }
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    /**
     * Layout tapped
     * @param layoutSlug the slug of the tapped layout
     */
    private fun onLayoutTapped(layoutSlug: String) {
        (uiState.value as? UiState.Content)?.let { state ->
            if (!state.loadedThumbnailSlugs.contains(layoutSlug)) return // No action
            if (layoutSlug == state.selectedLayoutSlug) { // deselect
                updateUiState(state.copy(selectedLayoutSlug = null, isToolbarVisible = false))
            } else {
                updateUiState(state.copy(selectedLayoutSlug = layoutSlug, isToolbarVisible = true))
            }
            loadLayouts()
        }
    }

    /**
     * Layout thumbnail is ready
     * @param layoutSlug the slug of the tapped layout
     */
    private fun onThumbnailReady(layoutSlug: String) {
        (uiState.value as? UiState.Content)?.let { state ->
            updateUiState(state.copy(loadedThumbnailSlugs = state.loadedThumbnailSlugs.apply { add(layoutSlug) }))
        }
    }

    private fun setHeaderTitleVisibility(headerShouldBeVisible: Boolean) {
        (uiState.value as? UiState.Content)?.let { state ->
            if (state.isHeaderVisible == headerShouldBeVisible) return // No change
            updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
        }
    }

    sealed class UiState(
        open val isHeaderVisible: Boolean = false,
        open val isToolbarVisible: Boolean = false,
        val isDescriptionVisible: Boolean = true,
        val loadingIndicatorVisible: Boolean = false,
        val errorViewVisible: Boolean = false
    ) {
        object Loading : UiState(loadingIndicatorVisible = true)

        data class Content(
            override val isHeaderVisible: Boolean = false,
            override val isToolbarVisible: Boolean = false,
            val selectedLayoutSlug: String? = null,
            val loadedThumbnailSlugs: ArrayList<String> = arrayListOf(),
            val layouts: List<LayoutGridItemUiState> = listOf()
        ) : UiState()

        object Error : UiState(errorViewVisible = true, isHeaderVisible = true, isDescriptionVisible = false)
    }
}
