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

    /**
     * Tracks the selected categories
     */
    val selectedCategoriesSlugs = arrayListOf<String>()

    /**
     * Tracks the selected layout slug
     */
    private val _selectedLayoutSlug = MutableLiveData<String?>()
    val selectedLayoutSlug: LiveData<String?> = _selectedLayoutSlug

    /**
     * Tracks the visibility of the action buttons
     */
    private val _buttonsUiState = MutableLiveData<ButtonsUiState>()
    val buttonsUiState: LiveData<ButtonsUiState> = _buttonsUiState

    /**
     * Tracks the layout categories
     */
    private val _layoutCategories = MutableLiveData<List<LayoutCategoryUiState>>()
    val layoutCategories: LiveData<List<LayoutCategoryUiState>> = _layoutCategories

    /**
     * Tracks the categories
     */
    private val _categories = MutableLiveData<List<CategoryListItemUiState>>()
    val categories: LiveData<List<CategoryListItemUiState>> = _categories

    /**
     * Create new page event
     */
    private val _onCreateNewPageRequested = SingleLiveEvent<Unit>()
    val onCreateNewPageRequested: LiveData<Unit> = _onCreateNewPageRequested

    fun init() {
        loadLayouts()
        loadCategories()
        updateButtonsUiState()
    }

    private fun loadLayouts() {
        val listItems = ArrayList<LayoutCategoryUiState>()

        val selectedCategories = if (selectedCategoriesSlugs.isNotEmpty())
            layouts.categories.filter { selectedCategoriesSlugs.contains(it.slug) }
        else layouts.categories

        selectedCategories.forEach { category ->
            val layouts = layouts.getFilteredLayouts(category.slug).map { layout ->
                val selected = layout.slug == _selectedLayoutSlug.value
                LayoutListItemUiState(layout.slug, layout.title, layout.preview, selected) {
                    layoutTapped(layoutSlug = layout.slug)
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

        _layoutCategories.postValue(listItems)
    }

    private fun loadCategories() {
        val listItems: List<CategoryListItemUiState> = layouts.categories.map {
            CategoryListItemUiState(
                    it.slug,
                    it.title,
                    it.emoji,
                    selectedCategoriesSlugs.contains(it.slug)
            ) { categoryTapped(it.slug) }
        }

        _categories.postValue(listItems)
    }

    /**
     * Shows the MLP
     */
    fun show() {
        _isModalLayoutPickerShowing.value = Event(true)
    }

    /**
     * Dismisses the MLP
     */
    fun dismiss() {
        _isModalLayoutPickerShowing.postValue(Event(false))
        _selectedLayoutSlug.value = null
        selectedCategoriesSlugs.clear()
    }

    /**
     * Category tapped
     * @param categorySlug the slug of the tapped category
     */
    fun categoryTapped(categorySlug: String) {
        if (selectedCategoriesSlugs.contains(categorySlug)) { // deselect
            selectedCategoriesSlugs.remove(categorySlug)
        } else {
            selectedCategoriesSlugs.add(categorySlug)
        }
        loadCategories()
        loadLayouts()
    }

    /**
     * Layout tapped
     * @param layoutSlug the slug of the tapped layout
     */
    fun layoutTapped(layoutSlug: String) {
        if (layoutSlug == _selectedLayoutSlug.value) { // deselect
            _selectedLayoutSlug.value = null
        } else {
            _selectedLayoutSlug.value = layoutSlug
        }
        updateButtonsUiState()
        loadLayouts()
    }

    /**
     * Updates the buttons UiState depending on the [_selectedLayoutSlug] value
     */
    private fun updateButtonsUiState() {
        val selection = _selectedLayoutSlug.value != null
        _buttonsUiState.value = ButtonsUiState(!selection, selection, selection)
    }

    /**
     * Triggers the creation of a new blank page
     */
    fun createPage() {
        _onCreateNewPageRequested.call()
    }
}
