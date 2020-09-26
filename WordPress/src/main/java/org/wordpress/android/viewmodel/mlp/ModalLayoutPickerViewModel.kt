package org.wordpress.android.viewmodel.mlp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchBlockLayoutsPayload
import org.wordpress.android.fluxc.store.SiteStore.OnBlockLayoutsFetched
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mlp.CategoryListItemUiState
import org.wordpress.android.ui.mlp.ButtonsUiState
import org.wordpress.android.ui.mlp.GutenbergPageLayouts
import org.wordpress.android.ui.mlp.LayoutListItemUiState
import org.wordpress.android.ui.mlp.LayoutCategoryUiState
import org.wordpress.android.ui.mlp.SupportedBlocks
import org.wordpress.android.ui.prefs.AppPrefsWrapper
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
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var layouts: GutenbergPageLayouts
    private lateinit var supportedBlocks: SupportedBlocks

    /**
     * Tracks the Modal Layout Picker visibility state
     */
    private val _isModalLayoutPickerShowing = MutableLiveData<Event<Boolean>>()
    val isModalLayoutPickerShowing: LiveData<Event<Boolean>> = _isModalLayoutPickerShowing

    private val _onCategorySelected = MutableLiveData<Event<Unit>>()
    val onCategorySelected: LiveData<Event<Unit>> = _onCategorySelected

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    /**
     * Create new page event
     */
    private val _onCreateNewPageRequested = SingleLiveEvent<String>()
    val onCreateNewPageRequested: LiveData<String> = _onCreateNewPageRequested

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun init() {
        fetchLayouts()
    }

    private fun fetchLayouts() {
        updateUiState(LoadingUiState)
        launch(bgDispatcher) {
            val siteId = appPrefsWrapper.getSelectedSite()
            val site = siteStore.getSiteByLocalId(siteId)
            val payload = FetchBlockLayoutsPayload(site, supportedBlocks.supported)
            dispatcher.dispatch(SiteActionBuilder.newFetchBlockLayoutsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBlockLayoutsFetched(event: OnBlockLayoutsFetched) {
        if (event.isError) {
            updateUiState(ErrorUiState(event.error.message))
        } else {
            handleBlockLayoutsResponse(GutenbergPageLayouts(event.layouts, event.categories))
        }
    }

    private fun handleBlockLayoutsResponse(response: GutenbergPageLayouts) {
        layouts = response
        loadCategories()
    }

    private fun loadLayouts() {
        val state = uiState.value as? ContentUiState ?: ContentUiState()
        launch(bgDispatcher) {
            val listItems = ArrayList<LayoutCategoryUiState>()

            val selectedCategories = if (state.selectedCategoriesSlugs.isNotEmpty())
                layouts.categories.filter { state.selectedCategoriesSlugs.contains(it.slug) }
            else layouts.categories

            selectedCategories.sortedBy { it.title }.forEach { category ->

                val layouts = layouts.getFilteredLayouts(category.slug).map { layout ->
                    val selected = layout.slug == state.selectedLayoutSlug
                    LayoutListItemUiState(layout.slug, layout.title, layout.preview, selected,
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
     * Shows the MLP
     * @param supportedBlocks the supported blocks to filter fetched layouts (by default no filtering occurs)
     */
    fun show(supportedBlocks: SupportedBlocks = SupportedBlocks()) {
        this.supportedBlocks = supportedBlocks
        init()
        _isModalLayoutPickerShowing.value = Event(true)
    }

    /**
     * Dismisses the MLP
     */
    fun dismiss() {
        _isModalLayoutPickerShowing.postValue(Event(false))
        updateUiState(ContentUiState())
    }

    /**
     * Notifies the VM to start passing the orientation
     * @param landscapeMode app operates in landscape mode
     */
    fun start(landscapeMode: Boolean) {
        setHeaderTitleVisibility(landscapeMode)
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
     * Updates the buttons UiState depending on the [_selectedLayoutSlug] value
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
            val selection = state.selectedLayoutSlug != null
            _onCreateNewPageRequested.value = if (selection) {
                layouts.layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.content ?: ""
            } else ""
        }
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    sealed class UiState(
        open val isHeaderVisible: Boolean = false,
        val loadingSkeletonVisible: Boolean = false
    ) {
        object LoadingUiState : UiState(loadingSkeletonVisible = true)

        data class ContentUiState(
            override val isHeaderVisible: Boolean = false,
            val selectedCategoriesSlugs: ArrayList<String> = arrayListOf(),
            val selectedLayoutSlug: String? = null,
            val loadedThumbnailSlugs: ArrayList<String> = arrayListOf(),
            val categories: List<CategoryListItemUiState> = listOf(),
            val layoutCategories: List<LayoutCategoryUiState> = listOf(),
            val buttonsUiState: ButtonsUiState = ButtonsUiState(
                    createBlankPageVisible = true,
                    previewVisible = false,
                    createPageVisible = false
            )
        ) : UiState()

        data class ErrorUiState(val message: String) : UiState()
    }
}
