package org.wordpress.android.ui.sitecreation.theme

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.StarterDesignModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

const val defaultTemplateSlug = "default"

private const val ERROR_CONTEXT = "design"
private const val FETCHED_LAYOUTS = "FETCHED_LAYOUTS"
private const val SELECTED_LAYOUT = "SELECTED_LAYOUT"

class HomePagePickerViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    private val analyticsTracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val fetchHomePageLayoutsJob = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + fetchHomePageLayoutsJob

    lateinit var layouts: List<StarterDesignModel>

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _onDesignActionPressed = SingleLiveEvent<DesignSelectionAction>()
    val onDesignActionPressed: LiveData<DesignSelectionAction> = _onDesignActionPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

    sealed class DesignSelectionAction(val template: String, val segmentId: Long?) {
        object Skip : DesignSelectionAction(defaultTemplateSlug, null)
        class Choose(template: String, segmentId: Long?) : DesignSelectionAction(template, segmentId)
    }

    init {
        dispatcher.register(fetchHomePageLayoutsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        fetchHomePageLayoutsJob.cancel()
        dispatcher.unregister(fetchHomePageLayoutsUseCase)
    }

    fun start() {
        if (uiState.value !is UiState.Content) {
            analyticsTracker.trackSiteDesignViewed()
            fetchLayouts()
        }
    }

    private fun fetchLayouts() {
        updateUiState(UiState.Loading)
        launch {
            val event = fetchHomePageLayoutsUseCase.fetchStarterDesigns()
            withContext(mainDispatcher) {
                if (event.isError) {
                    analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error fetching designs")
                    updateUiState(UiState.Error())
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
        (uiState.value as? UiState.Content)?.let { state ->
            layouts.firstOrNull { it.slug != null && it.slug == state.selectedLayoutSlug }?.let { layout ->
                val template = layout.slug!!
                analyticsTracker.trackSiteDesignSelected(template)
                _onDesignActionPressed.value = DesignSelectionAction.Choose(template, layout.segmentId)
                return
            }
        }
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error choosing design")
        updateUiState(UiState.Error(toast = R.string.hpp_choose_error))
    }

    fun onSkippedTapped() {
        analyticsTracker.trackSiteDesignSkipped()
        _onDesignActionPressed.value = DesignSelectionAction.Skip
    }

    fun onBackPressed() {
        _onBackButtonPressed.call()
    }

    fun onRetryClicked() {
        if (networkUtils.isNetworkAvailable()) {
            fetchLayouts()
        } else {
            analyticsTracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR, "Retry error")
            updateUiState(UiState.Error(toast = R.string.hpp_retry_error))
        }
    }

    fun loadSavedState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        val layouts = savedInstanceState.getParcelableArrayList<StarterDesignModel>(FETCHED_LAYOUTS)
        val selected = savedInstanceState.getString(SELECTED_LAYOUT)
        if (layouts == null || layouts.isEmpty()) {
            fetchLayouts()
            return
        }
        val state = uiState.value as? UiState.Content ?: UiState.Content()
        updateUiState(state.copy(selectedLayoutSlug = selected))
        this.layouts = layouts
        loadLayouts()
    }

    fun writeToBundle(outState: Bundle) {
        (uiState.value as? UiState.Content)?.let {
            outState.putParcelableArrayList(FETCHED_LAYOUTS, ArrayList(layouts))
            outState.putString(SELECTED_LAYOUT, it.selectedLayoutSlug)
        }
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    /**
     * Layout tapped
     * @param layoutSlug the slug of the tapped layout
     */
    fun onLayoutTapped(layoutSlug: String) {
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
    fun onThumbnailReady(layoutSlug: String) {
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

        class Error(@StringRes val toast: Int? = null) :
                UiState(errorViewVisible = true, isHeaderVisible = true, isDescriptionVisible = false)
    }
}
