package org.wordpress.android.viewmodel.mlp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mlp.GutenbergPageLayoutFactory
import org.wordpress.android.ui.mlp.LayoutListItem
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem
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
    /**
     * Tracks the Modal Layout Picker visibility state
     */
    private val _isModalLayoutPickerShowing = MutableLiveData<Event<Boolean>>()
    val isModalLayoutPickerShowing: LiveData<Event<Boolean>> = _isModalLayoutPickerShowing

    /**
     * Tracks the header visibility
     */
    private val _isHeaderVisible = MutableLiveData<Event<Boolean>>()
    val isHeaderVisible: LiveData<Event<Boolean>> = _isHeaderVisible

    /**
     * Tracks the selected layout slug
     */
    private val _selectedLayoutSlug = MutableLiveData<String?>()
    val selectedLayoutSlug: LiveData<String?> = _selectedLayoutSlug

    /**
     * Tracks the Modal Layout Picker list items
     */
    private val _listItems = MutableLiveData<List<ModalLayoutPickerListItem>>()
    val listItems: LiveData<List<ModalLayoutPickerListItem>> = _listItems

    /**
     * Create new page event
     */
    private val _onCreateNewPageRequested = SingleLiveEvent<Unit>()
    val onCreateNewPageRequested: LiveData<Unit> = _onCreateNewPageRequested

    private var landscapeMode: Boolean = false

    fun init(landscape: Boolean) {
        landscapeMode = landscape
        loadListItems()
    }

    private fun loadListItems() {
        val listItems = ArrayList<ModalLayoutPickerListItem>()

        if (!landscapeMode) {
            val titleVisibility = _isHeaderVisible.value?.peekContent() ?: true
            listItems.add(ModalLayoutPickerListItem.Title(R.string.mlp_choose_layout_title, titleVisibility))
            listItems.add(ModalLayoutPickerListItem.Subtitle(R.string.mlp_choose_layout_subtitle))
        }

        listItems.add(ModalLayoutPickerListItem.Categories())

        loadLayouts(listItems)

        _listItems.postValue(listItems)
    }

    /**
     * Loads DEMO layout data
     */
    private fun loadLayouts(listItems: ArrayList<ModalLayoutPickerListItem>) {
        val demoLayouts = GutenbergPageLayoutFactory.makeDefaultPageLayouts()

        demoLayouts.categories.forEach { category ->
            val layouts = demoLayouts.layouts(category.slug).map { layout ->
                val selected = layout.slug == _selectedLayoutSlug.value
                LayoutListItem(layout.slug, layout.title, layout.preview, selected)
            }
            listItems.add(ModalLayoutPickerListItem.LayoutCategory(category.title, category.description, layouts))
        }
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
        _isHeaderVisible.postValue(Event(true))
        _selectedLayoutSlug.value = null
    }

    /**
     * Sets the header and title visibility
     * @param headerShouldBeVisible if true the header is shown and the title row hidden
     */
    fun setHeaderTitleVisibility(headerShouldBeVisible: Boolean) {
        if (_isHeaderVisible.value?.peekContent() == headerShouldBeVisible) return
        _isHeaderVisible.postValue(Event(headerShouldBeVisible))
        loadListItems()
    }

    /**
     * Layout tapped
     */
    fun layoutTapped(layout: LayoutListItem) {
        if (layout.slug == _selectedLayoutSlug.value) { // deselect
            _selectedLayoutSlug.value = null
        } else {
            _selectedLayoutSlug. value = layout.slug
        }
    }

    /**
     * Triggers the creation of a new blank page
     */
    fun createPage() {
        _isModalLayoutPickerShowing.postValue(Event(false))
        _onCreateNewPageRequested.call()
    }
}
