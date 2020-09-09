package org.wordpress.android.viewmodel.mlp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mlp.CategoryListItemUiState
import org.wordpress.android.ui.mlp.ButtonsUiState
import org.wordpress.android.ui.mlp.GutenbergPageLayoutFactory
import org.wordpress.android.ui.mlp.GutenbergPageLayouts
import org.wordpress.android.ui.mlp.LayoutListItemUiState
import org.wordpress.android.ui.mlp.LayoutCategoryUiState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ContentUiState
import javax.inject.Inject
import javax.inject.Named

/**
 * Implements the Modal Layout Picker view model
 */
class ModalLayoutPickerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val layouts: GutenbergPageLayouts = GutenbergPageLayoutFactory.makeDefaultPageLayouts()

    /**
     * Tracks the Modal Layout Picker visibility state
     */
    private val _isModalLayoutPickerShowing = MutableLiveData<Event<Boolean>>()
    val isModalLayoutPickerShowing: LiveData<Event<Boolean>> = _isModalLayoutPickerShowing

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    /**
     * Create new page event
     */
    private val _onCreateNewPageRequested = SingleLiveEvent<Unit>()
    val onCreateNewPageRequested: LiveData<Unit> = _onCreateNewPageRequested

    fun init() {
        updateUiState(ContentUiState())
        loadLayouts()
        loadCategories()
        updateButtonsUiState()
    }

    private fun loadLayouts() {
        val state = uiState.value as ContentUiState
        val listItems = ArrayList<LayoutCategoryUiState>()

        val selectedCategories = if (state.selectedCategoriesSlugs.isNotEmpty())
            layouts.categories.filter { state.selectedCategoriesSlugs.contains(it.slug) }
        else layouts.categories

        selectedCategories.forEach { category ->
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
        val state = uiState.value as ContentUiState
        val listItems: List<CategoryListItemUiState> = layouts.categories.map {
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
     * Sets the header and title visibility
     * @param headerShouldBeVisible if true the header is shown and the title hidden
     */
    fun setHeaderTitleVisibility(headerShouldBeVisible: Boolean) {
        val state = uiState.value as ContentUiState
        if (state.isHeaderVisible == headerShouldBeVisible) return // No change
        updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
    }

    /**
     * Category tapped
     * @param categorySlug the slug of the tapped category
     */
    fun onCategoryTapped(categorySlug: String) {
        val state = uiState.value as ContentUiState
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
        val state = uiState.value as ContentUiState
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
        val state = uiState.value as ContentUiState
        val selection = state.selectedLayoutSlug != null
        updateUiState(state.copy(buttonsUiState = ButtonsUiState(!selection, selection, selection)))
    }

    /**
     * Triggers the creation of a new blank page
     */
    fun createPage() {
        _onCreateNewPageRequested.call()
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    sealed class UiState {
        object LoadingUiState : UiState()

        data class ContentUiState(
            val isHeaderVisible: Boolean = false,
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

        object ErrorUiState : UiState()
    }
}
