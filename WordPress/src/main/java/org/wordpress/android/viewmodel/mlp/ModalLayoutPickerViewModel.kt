package org.wordpress.android.viewmodel.mlp

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchBlockLayoutsPayload
import org.wordpress.android.fluxc.store.SiteStore.OnBlockLayoutsFetched
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.PreviewMode
import org.wordpress.android.ui.PreviewMode.MOBILE
import org.wordpress.android.ui.PreviewMode.TABLET
import org.wordpress.android.ui.PreviewModeHandler
import org.wordpress.android.ui.mlp.CategoryListItemUiState
import org.wordpress.android.ui.mlp.ButtonsUiState
import org.wordpress.android.ui.mlp.GutenbergPageLayouts
import org.wordpress.android.ui.mlp.LayoutListItemUiState
import org.wordpress.android.ui.mlp.LayoutCategoryUiState
import org.wordpress.android.ui.mlp.SupportedBlocksProvider
import org.wordpress.android.ui.mlp.ThumbDimensionProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ContentUiState
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ErrorUiState
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.LoadingUiState
import javax.inject.Inject
import javax.inject.Named

/**
 * Implements the Modal Layout Picker view model
 */
class ModalLayoutPickerViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val supportedBlocksProvider: SupportedBlocksProvider,
    private val thumbDimensionProvider: ThumbDimensionProvider,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val networkUtils: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher), PreviewModeHandler {
    private lateinit var layouts: GutenbergPageLayouts

    /**
     * Tracks the Modal Layout Picker visibility state
     */
    private val _isModalLayoutPickerShowing = MutableLiveData<Event<Boolean>>()
    val isModalLayoutPickerShowing: LiveData<Event<Boolean>> = _isModalLayoutPickerShowing

    private val _onCategorySelected = MutableLiveData<Event<Unit>>()
    val onCategorySelected: LiveData<Event<Unit>> = _onCategorySelected

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _previewMode = SingleLiveEvent<PreviewMode>()
    private val previewMode: LiveData<PreviewMode> = _previewMode

    /**
     * Create new page event
     */
    private val _onCreateNewPageRequested = SingleLiveEvent<PageRequest.Create>()
    val onCreateNewPageRequested: LiveData<PageRequest.Create> = _onCreateNewPageRequested

    /**
     * Preview page event
     */
    private val _onPreviewPageRequested = SingleLiveEvent<PageRequest.Preview>()
    val onPreviewPageRequested: LiveData<PageRequest.Preview> = _onPreviewPageRequested

    /**
     * Thumbnail mode button event
     */
    private val _onThumbnailModeButtonPressed = SingleLiveEvent<Unit>()
    val onThumbnailModeButtonPressed: LiveData<Unit> = _onThumbnailModeButtonPressed

    sealed class PageRequest(val template: String?, val content: String) {
        open class Create(template: String?, content: String, val title: String) : PageRequest(template, content)
        object Blank : Create(null, "", "")
        class Preview(template: String?, content: String, val site: SiteModel, val demoUrl: String?) : PageRequest(
                template,
                content
        )
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    private fun fetchLayouts() {
        updateUiState(LoadingUiState)
        launch(bgDispatcher) {
            val siteId = appPrefsWrapper.getSelectedSite()
            val site = siteStore.getSiteByLocalId(siteId)
            val payload = FetchBlockLayoutsPayload(
                    site,
                    supportedBlocksProvider.fromAssets().supported,
                    thumbDimensionProvider.previewWidth.toFloat(),
                    thumbDimensionProvider.scale.toFloat(),
                    BuildConfig.DEBUG
            )
            dispatcher.dispatch(SiteActionBuilder.newFetchBlockLayoutsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBlockLayoutsFetched(event: OnBlockLayoutsFetched) {
        if (event.isError) {
            setErrorState()
        } else {
            handleBlockLayoutsResponse(GutenbergPageLayouts(event.layouts, event.categories))
        }
    }

    fun fetchedLayouts(): GutenbergPageLayouts = if (::layouts.isInitialized) layouts else GutenbergPageLayouts()

    private fun handleBlockLayoutsResponse(response: GutenbergPageLayouts) {
        layouts = response
        loadCategories()
    }

    private fun setErrorState() {
        if (networkUtils.isNetworkAvailable()) {
            updateUiState(ErrorUiState(string.mlp_error_title, string.mlp_error_subtitle))
        } else {
            updateUiState(ErrorUiState(string.mlp_network_error_title, string.mlp_network_error_subtitle))
        }
    }

    private fun loadLayouts() {
        val state = uiState.value as? ContentUiState ?: ContentUiState()
        launch(bgDispatcher) {
            val listItems = ArrayList<LayoutCategoryUiState>()

            val selectedCategories = if (state.selectedCategoriesSlugs.isNotEmpty()) {
                layouts.categories.filter { state.selectedCategoriesSlugs.contains(it.slug) }
            } else {
                layouts.categories
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

    private fun loadCategories() {
        val state = uiState.value as? ContentUiState ?: ContentUiState()
        launch(bgDispatcher) {
            val listItems: List<CategoryListItemUiState> = layouts.categories.sortedBy { it.title }.map {
                CategoryListItemUiState(
                        it.slug,
                        it.title,
                        it.emoji,
                        state.selectedCategoriesSlugs.contains(it.slug)
                ) { onCategoryTapped(it.slug) }
            }
            withContext(mainDispatcher) {
                updateUiState(state.copy(categories = listItems))
            }
            loadLayouts()
        }
    }

    /**
     * Checks if the Modal Layout Picker can be shown
     * at this point the only requirement is to have the block editor enabled
     * @return true if the Modal Layout Picker can be shown
     */
    fun canShowModalLayoutPicker(): Boolean {
        val siteId = appPrefsWrapper.getSelectedSite()
        val site = siteStore.getSiteByLocalId(siteId)
        return SiteUtils.isBlockEditorDefaultForNewPost(site)
    }

    /**
     * Triggers the create page flow and shows the MLP
     */
    fun createPageFlowTriggered() {
        _isModalLayoutPickerShowing.value = Event(true)
        if (_previewMode.value == null) {
            _previewMode.value = if (displayUtilsWrapper.isTablet()) {
                TABLET
            } else {
                MOBILE
            }
        }
        fetchLayouts()
    }

    /**
     * Retries data fetching
     */
    fun onRetryClicked() {
        if (networkUtils.isNetworkAvailable()) {
            fetchLayouts()
        }
    }

    /**
     * Dismisses the MLP
     */
    fun dismiss() {
        _isModalLayoutPickerShowing.postValue(Event(false))
        updateUiState(ContentUiState())
    }

    /**
     * Sets the header and title visibility
     * @param headerShouldBeVisible if true the header is shown and the title hidden
     */
    private fun setHeaderTitleVisibility(headerShouldBeVisible: Boolean) {
        (uiState.value as? ContentUiState)?.let { state ->
            if (state.isHeaderVisible == headerShouldBeVisible) return // No change
            updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
        }
    }

    /**
     * Layout thumbnail is ready
     * @param layoutSlug the slug of the tapped layout
     */
    fun onThumbnailReady(layoutSlug: String) {
        (uiState.value as? ContentUiState)?.let { state ->
            updateUiState(state.copy(loadedThumbnailSlugs = state.loadedThumbnailSlugs.apply { add(layoutSlug) }))
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

    /**
     * Category tapped
     * @param categorySlug the slug of the tapped category
     */
    fun onCategoryTapped(categorySlug: String) {
        (uiState.value as? ContentUiState)?.let { state ->
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
        (uiState.value as? ContentUiState)?.let { state ->
            if (!state.loadedThumbnailSlugs.contains(layoutSlug)) return // No action
            if (layoutSlug == state.selectedLayoutSlug) { // deselect
                updateUiState(state.copy(selectedLayoutSlug = null))
            } else {
                updateUiState(state.copy(selectedLayoutSlug = layoutSlug))
            }
            updateButtonsUiState()
            loadLayouts()
        }
    }

    /**
     * Create page tapped
     */
    fun onCreatePageClicked() {
        createPage()
        dismiss()
    }

    /**
     * Preview page tapped
     */
    fun onPreviewPageClicked() {
        (uiState.value as? ContentUiState)?.let { state ->
            layouts.layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.let { layout ->
                val site = siteStore.getSiteByLocalId(appPrefsWrapper.getSelectedSite())
                _onPreviewPageRequested.value = PageRequest.Preview(layout.slug, layout.content, site, layout.demoUrl)
            }
        }
    }

    fun onThumbnailModePressed() {
        _onThumbnailModeButtonPressed.call()
    }

    /**
     * Updates the buttons UiState
     */
    private fun updateButtonsUiState() {
        (uiState.value as? ContentUiState)?.let { state ->
            val selection = state.selectedLayoutSlug != null
            updateUiState(state.copy(buttonsUiState = ButtonsUiState(!selection, selection, selection)))
        }
    }

    /**
     * Triggers the creation of a new page
     */
    private fun createPage() {
        (uiState.value as? ContentUiState)?.let { state ->
            layouts.layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.let { layout ->
                _onCreateNewPageRequested.value = PageRequest.Create(layout.slug, layout.content, layout.title)
                return
            }
        }
        _onCreateNewPageRequested.value = PageRequest.Blank
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    fun loadSavedState(layouts: GutenbergPageLayouts?, selectedLayout: String?, selectedCategories: List<String>?) {
        if (layouts == null || layouts.isEmpty) {
            setErrorState()
            return
        }
        val state = uiState.value as? ContentUiState ?: ContentUiState()
        val categories = ArrayList(selectedCategories ?: listOf())
        updateUiState(state.copy(selectedLayoutSlug = selectedLayout, selectedCategoriesSlugs = categories))
        updateButtonsUiState()
        handleBlockLayoutsResponse(layouts)
    }

    sealed class UiState(
        open val isHeaderVisible: Boolean = false,
        val isDescriptionVisible: Boolean = true,
        val loadingSkeletonVisible: Boolean = false,
        val errorViewVisible: Boolean = false,
        open val buttonsUiState: ButtonsUiState = ButtonsUiState()
    ) {
        object LoadingUiState : UiState(loadingSkeletonVisible = true)

        data class ContentUiState(
            override val isHeaderVisible: Boolean = false,
            val selectedCategoriesSlugs: ArrayList<String> = arrayListOf(),
            val selectedLayoutSlug: String? = null,
            val loadedThumbnailSlugs: ArrayList<String> = arrayListOf(),
            val categories: List<CategoryListItemUiState> = listOf(),
            val layoutCategories: List<LayoutCategoryUiState> = listOf(),
            override val buttonsUiState: ButtonsUiState = ButtonsUiState()
        ) : UiState()

        data class ErrorUiState(@StringRes val title: Int, @StringRes val subtitle: Int) : UiState(
                errorViewVisible = true,
                isHeaderVisible = true,
                isDescriptionVisible = false,
                buttonsUiState = ButtonsUiState(retryVisible = true)
        )
    }

    override fun selectedPreviewMode() = previewMode.value ?: MOBILE

    override fun onPreviewModeChanged(mode: PreviewMode) {
        if (_previewMode.value !== mode) {
            _previewMode.value = mode
            if (uiState.value is ContentUiState) {
                loadLayouts()
            }
        }
    }
}
