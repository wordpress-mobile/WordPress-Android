package org.wordpress.android.viewmodel.mlp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnBlockLayoutsFetched
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mlp.CategoryListItemUiState
import org.wordpress.android.ui.mlp.ButtonsUiState
import org.wordpress.android.ui.mlp.GutenbergPageLayoutFactory
import org.wordpress.android.ui.mlp.GutenbergPageLayouts
import org.wordpress.android.ui.mlp.LayoutListItemUiState
import org.wordpress.android.ui.mlp.LayoutCategoryUiState
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
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var layouts: GutenbergPageLayouts = GutenbergPageLayouts()

    /**
     * Tracks the Modal Layout Picker visibility state
     */
    private val _isModalLayoutPickerShowing = MutableLiveData<Event<Boolean>>()
    val isModalLayoutPickerShowing: LiveData<Event<Boolean>> = _isModalLayoutPickerShowing

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val contentUiState: ContentUiState
        get() = uiState.value as? ContentUiState ?: ContentUiState()

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
        val siteId = appPrefsWrapper.getSelectedSite()
        val site = siteStore.getSiteByLocalId(siteId)
        if (site.isWPCom) {
            dispatcher.dispatch(SiteActionBuilder.newFetchBlockLayoutsAction(site))
        } else {
            layouts = GutenbergPageLayoutFactory.makeDefaultPageLayouts()
            loadLayouts()
            loadCategories()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBlockLayoutsFetched(event: OnBlockLayoutsFetched) {
        if (event.isError) {
            updateUiState(ErrorUiState(event.error.message))
        } else {
            layouts = GutenbergPageLayouts(event.layouts, event.categories)
            loadLayouts()
            loadCategories()
        }
    }

    private fun loadLayouts() {
        val state = contentUiState
        val listItems = ArrayList<LayoutCategoryUiState>()

        val selectedCategories = if (state.selectedCategoriesSlugs.isNotEmpty())
            layouts.categories.filter { state.selectedCategoriesSlugs.contains(it.slug) }
        else layouts.categories

        selectedCategories.sortedBy { it.title }.forEach { category ->

            val layouts = layouts.getFilteredLayouts(category.slug).map { layout ->
                val selected = layout.slug == state.selectedLayoutSlug
                LayoutListItemUiState(layout.slug, layout.title, layout.preview, selected) {
                    onLayoutTapped(layoutSlug = layout.slug)
                }
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
        updateUiState(state.copy(layoutCategories = listItems))
    }

    private fun loadCategories() {
        val state = contentUiState
        val listItems: List<CategoryListItemUiState> = layouts.categories.sortedBy { it.title }.map {
            CategoryListItemUiState(
                    it.slug,
                    it.title,
                    it.emoji,
                    state.selectedCategoriesSlugs.contains(it.slug)
            ) { onCategoryTapped(it.slug) }
        }
        updateUiState(state.copy(categories = listItems))
    }

    /**
     * Shows the MLP
     */
    fun show() {
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
        val state = contentUiState
        if (state.isHeaderVisible == headerShouldBeVisible) return // No change
        updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
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
        val state = contentUiState
        if (state.selectedCategoriesSlugs.contains(categorySlug)) { // deselect
            updateUiState(
                    state.copy(selectedCategoriesSlugs = state.selectedCategoriesSlugs.apply { remove(categorySlug) })
            )
        } else {
            updateUiState(
                    state.copy(selectedCategoriesSlugs = state.selectedCategoriesSlugs.apply { add(categorySlug) })
            )
        }
        loadCategories()
        loadLayouts()
    }

    /**
     * Layout tapped
     * @param layoutSlug the slug of the tapped layout
     */
    fun onLayoutTapped(layoutSlug: String) {
        val state = contentUiState
        if (layoutSlug == state.selectedLayoutSlug) { // deselect
            updateUiState(state.copy(selectedLayoutSlug = null))
        } else {
            updateUiState(state.copy(selectedLayoutSlug = layoutSlug))
        }
        updateButtonsUiState()
        loadLayouts()
    }

    /**
     * Updates the buttons UiState depending on the [_selectedLayoutSlug] value
     */
    private fun updateButtonsUiState() {
        val state = contentUiState
        val selection = state.selectedLayoutSlug != null
        updateUiState(state.copy(buttonsUiState = ButtonsUiState(!selection, selection, selection)))
    }

    /**
     * Triggers the creation of a new page
     */
    fun createPage() {
        val state = contentUiState
        val selection = state.selectedLayoutSlug != null
        _onCreateNewPageRequested.value = if (selection) {
            layouts.layouts.firstOrNull { it.slug == state.selectedLayoutSlug }?.content ?: ""
        } else ""
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
