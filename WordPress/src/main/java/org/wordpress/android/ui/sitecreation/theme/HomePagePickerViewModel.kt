package org.wordpress.android.ui.sitecreation.theme

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesign
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesignCategory
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.PreviewMode
import org.wordpress.android.ui.PreviewModeHandler
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.PreviewMode.MOBILE
import org.wordpress.android.ui.PreviewMode.TABLET
import org.wordpress.android.ui.PreviewMode.valueOf
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState
import org.wordpress.android.ui.layoutpicker.CategoryListItemUiState
import org.wordpress.android.ui.layoutpicker.LayoutCategoryUiState
import org.wordpress.android.ui.layoutpicker.LayoutListItemUiState
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

const val defaultTemplateSlug = "default"

private const val ERROR_CONTEXT = "design"
private const val FETCHED_LAYOUTS = "FETCHED_LAYOUTS"
private const val SELECTED_LAYOUT = "SELECTED_LAYOUT"
private const val PREVIEW_MODE = "PREVIEW_MODE"

class HomePagePickerViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    private val analyticsTracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher), PreviewModeHandler {
    lateinit var layouts: List<StarterDesign>
    lateinit var categories: List<StarterDesignCategory>

    private val _uiState: MutableLiveData<LayoutPickerUiState> = MutableLiveData()
    val uiState: LiveData<LayoutPickerUiState> = _uiState

    private val _previewState: MutableLiveData<PreviewUiState> = MutableLiveData()
    val previewState: LiveData<PreviewUiState> = _previewState

    private val _onDesignActionPressed = SingleLiveEvent<DesignSelectionAction>()
    val onDesignActionPressed: LiveData<DesignSelectionAction> = _onDesignActionPressed

    private val _onCategorySelected = MutableLiveData<Event<Unit>>()
    val onCategorySelected: LiveData<Event<Unit>> = _onCategorySelected

    private val _onPreviewActionPressed = SingleLiveEvent<DesignPreviewAction>()
    val onPreviewActionPressed: LiveData<DesignPreviewAction> = _onPreviewActionPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

    private val _previewMode = SingleLiveEvent<PreviewMode>()
    val previewMode: LiveData<PreviewMode> = _previewMode

    private val _onThumbnailModeButtonPressed = SingleLiveEvent<Unit>()
    val onThumbnailModeButtonPressed: LiveData<Unit> = _onThumbnailModeButtonPressed

    private val _onPreviewModeButtonPressed = SingleLiveEvent<Unit>()
    val onPreviewModeButtonPressed: LiveData<Unit> = _onPreviewModeButtonPressed

    sealed class DesignSelectionAction(val template: String) {
        object Skip : DesignSelectionAction(defaultTemplateSlug)
        class Choose(template: String) : DesignSelectionAction(template)
    }

    sealed class DesignPreviewAction {
        object Dismiss : DesignPreviewAction()
        class Show(val template: String, val demoUrl: String) : DesignPreviewAction()
    }

    init {
        dispatcher.register(fetchHomePageLayoutsUseCase)
    }

    override fun selectedPreviewMode() = previewMode.value ?: MOBILE

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchHomePageLayoutsUseCase)
    }

    fun start(isTablet: Boolean = false) {
        if (_previewMode.value == null) {
            _previewMode.value = if (isTablet) TABLET else MOBILE
        }
        if (uiState.value !is LayoutPickerUiState.Content) {
            analyticsTracker.trackSiteDesignViewed(selectedPreviewMode().key)
            fetchLayouts()
        }
    }

    private fun fetchLayouts() {
        if (_uiState.value === LayoutPickerUiState.Loading) return
        updateUiState(LayoutPickerUiState.Loading)
        launch {
            val event = fetchHomePageLayoutsUseCase.fetchStarterDesigns()
            withContext(mainDispatcher) {
                if (event.isError) {
                    analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error fetching designs")
                    updateUiState(LayoutPickerUiState.Error())
                } else {
                    categories = event.categories
                    layouts = event.designs
                    loadCategories()
                }
            }
        }
    }

    private fun loadCategories() {
        val state = uiState.value as? LayoutPickerUiState.Content ?: LayoutPickerUiState.Content()
        launch(bgDispatcher) {
            val listItems: List<CategoryListItemUiState> = categories.sortedBy { it.title }.map {
                CategoryListItemUiState(
                        it.slug,
                        it.title,
                        it.emoji ?: "",
                        state.selectedCategoriesSlugs.contains(it.slug)
                ) { onCategoryTapped(it.slug) }
            }
            withContext(mainDispatcher) {
                updateUiState(state.copy(categories = listItems))
            }
            loadLayouts()
        }
    }

    fun List<StarterDesign>.getFilteredLayouts(categorySlug: String) =
            layouts.filter { l -> l.categories.any { c -> c.slug == categorySlug } }

    private fun loadLayouts() {
        val state = uiState.value as? LayoutPickerUiState.Content ?: LayoutPickerUiState.Content()
        launch(bgDispatcher) {
            val listItems = ArrayList<LayoutCategoryUiState>()

            val selectedCategories = if (state.selectedCategoriesSlugs.isNotEmpty()) {
                categories.filter { state.selectedCategoriesSlugs.contains(it.slug) }
            } else {
                categories
            }

            selectedCategories.sortedBy { it.title }.forEach { category ->

                val layouts = layouts.getFilteredLayouts(category.slug).map { layout ->
                    LayoutListItemUiState(
                            slug = layout.slug,
                            title = layout.title,
                            preview = when (_previewMode.value) {
                                MOBILE -> layout.previewMobile
                                TABLET -> layout.previewTablet
                                else -> layout.preview
                            },
                            selected = layout.slug == state.selectedLayoutSlug,
                            onItemTapped = { onLayoutTapped(layoutSlug = layout.slug) },
                            onThumbnailReady = { onThumbnailReady(layoutSlug = layout.slug) }
                    )
                }
                listItems.add(
                        LayoutCategoryUiState(
                                category.slug,
                                category.title,
                                category.description,
                                layouts
                        )
                )
            }
            withContext(mainDispatcher) {
                updateUiState(state.copy(layoutCategories = listItems))
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
        (uiState.value as? LayoutPickerUiState.Content)?.let { state ->
            layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.let { layout ->
                val template = layout.slug
                analyticsTracker.trackSiteDesignPreviewViewed(template, selectedPreviewMode().key)
                _onPreviewActionPressed.value = DesignPreviewAction.Show(template, layout.demoUrl)
                return
            }
        }
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error previewing design")
        updateUiState(LayoutPickerUiState.Error(toast = R.string.hpp_choose_error))
    }

    fun onDismissPreview() {
        _onPreviewActionPressed.value = DesignPreviewAction.Dismiss
    }

    fun onPreviewChooseTapped() {
        onDismissPreview()
        onChooseTapped()
    }

    fun onPreviewLoading(template: String) {
        if (networkUtils.isNetworkAvailable()) {
            _previewState.value = PreviewUiState.Loading
            analyticsTracker.trackSiteDesignPreviewLoading(template, selectedPreviewMode().key)
        } else {
            _previewState.value = PreviewUiState.Error(toast = R.string.hpp_retry_error)
            analyticsTracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR, "Preview error")
        }
    }

    fun onPreviewLoaded(template: String) {
        _previewState.value = PreviewUiState.Loaded
        analyticsTracker.trackSiteDesignPreviewLoaded(template, selectedPreviewMode().key)
    }

    fun onPreviewError() {
        _previewState.value = PreviewUiState.Error()
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Preview error")
    }

    fun onChooseTapped() {
        (uiState.value as? LayoutPickerUiState.Content)?.let { state ->
            layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.let { layout ->
                val template = layout.slug
                analyticsTracker.trackSiteDesignSelected(template)
                _onDesignActionPressed.value = DesignSelectionAction.Choose(template)
                return
            }
        }
        analyticsTracker.trackErrorShown(ERROR_CONTEXT, UNKNOWN, "Error choosing design")
        updateUiState(LayoutPickerUiState.Error(toast = R.string.hpp_choose_error))
    }

    fun onSkippedTapped() {
        analyticsTracker.trackSiteDesignSkipped()
        _onDesignActionPressed.value = DesignSelectionAction.Skip
    }

    fun onBackPressed() {
        _onBackButtonPressed.call()
    }

    fun onThumbnailModePressed() {
        analyticsTracker.trackSiteDesignThumbnailModeTapped(selectedPreviewMode().key)
        _onThumbnailModeButtonPressed.call()
    }

    fun onPreviewModePressed() {
        analyticsTracker.trackSiteDesignPreviewModeTapped(selectedPreviewMode().key)
        _onPreviewModeButtonPressed.call()
    }

    fun onRetryClicked() {
        if (networkUtils.isNetworkAvailable()) {
            fetchLayouts()
        } else {
            analyticsTracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR, "Retry error")
            updateUiState(LayoutPickerUiState.Error(toast = R.string.hpp_retry_error))
        }
    }

    fun loadSavedState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        val layouts = savedInstanceState.getParcelableArrayList<StarterDesign>(FETCHED_LAYOUTS)
        val selected = savedInstanceState.getString(SELECTED_LAYOUT)
        val previewMode = savedInstanceState.getString(PREVIEW_MODE, MOBILE.name)
        if (layouts == null || layouts.isEmpty()) {
            fetchLayouts()
            return
        }
        val state = uiState.value as? LayoutPickerUiState.Content ?: LayoutPickerUiState.Content()
        updateUiState(state.copy(selectedLayoutSlug = selected))
        this.layouts = layouts
        _previewMode.value = valueOf(previewMode)
        loadLayouts()
    }

    fun writeToBundle(outState: Bundle) {
        (uiState.value as? LayoutPickerUiState.Content)?.let {
            outState.putParcelableArrayList(FETCHED_LAYOUTS, ArrayList(layouts))
            outState.putString(SELECTED_LAYOUT, it.selectedLayoutSlug)
            outState.putString(PREVIEW_MODE, _previewMode.value?.name ?: MOBILE.name)
        }
    }

    private fun updateUiState(uiState: LayoutPickerUiState) {
        _uiState.value = uiState
    }

    /**
     * Category tapped
     * @param categorySlug the slug of the tapped category
     */
    fun onCategoryTapped(categorySlug: String) {
        (uiState.value as? LayoutPickerUiState.Content)?.let { state ->
            if (state.selectedCategoriesSlugs.contains(categorySlug)) { // deselect
                updateUiState(
                        state.copy(selectedCategoriesSlugs = state.selectedCategoriesSlugs.apply {
                            remove(categorySlug)
                        })
                )
            } else {
                updateUiState(
                        state.copy(selectedCategoriesSlugs = state.selectedCategoriesSlugs.apply { add(categorySlug) })
                )
            }
            loadCategories()
            _onCategorySelected.postValue(Event(Unit))
        }
    }

    /**
     * Layout tapped
     * @param layoutSlug the slug of the tapped layout
     */
    fun onLayoutTapped(layoutSlug: String) {
        (uiState.value as? LayoutPickerUiState.Content)?.let { state ->
            if (!state.loadedThumbnailSlugs.contains(layoutSlug)) return // No action
            if (layoutSlug == state.selectedLayoutSlug) { // deselect
                updateUiState(state.copy(selectedLayoutSlug = null, isToolbarVisible = false))
            } else {
                updateUiState(state.copy(selectedLayoutSlug = layoutSlug, isToolbarVisible = true))
            }
            loadLayouts()
        }
    }

    override fun onPreviewModeChanged(mode: PreviewMode) {
        if (_previewMode.value !== mode) {
            analyticsTracker.trackSiteDesignPreviewModeChanged(mode.key)
            _previewMode.value = mode
            if (uiState.value is LayoutPickerUiState.Content) {
                loadLayouts()
            }
        }
    }

    /**
     * Layout thumbnail is ready
     * @param layoutSlug the slug of the tapped layout
     */
    fun onThumbnailReady(layoutSlug: String) {
        (uiState.value as? LayoutPickerUiState.Content)?.let { state ->
            updateUiState(state.copy(loadedThumbnailSlugs = state.loadedThumbnailSlugs.apply { add(layoutSlug) }))
        }
    }

    private fun setHeaderTitleVisibility(headerShouldBeVisible: Boolean) {
        (uiState.value as? LayoutPickerUiState.Content)?.let { state ->
            if (state.isHeaderVisible == headerShouldBeVisible) return // No change
            updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
        }
    }

    sealed class PreviewUiState {
        object Loading : PreviewUiState()
        object Loaded : PreviewUiState()
        class Error(@StringRes val toast: Int? = null) : PreviewUiState()
    }
}
