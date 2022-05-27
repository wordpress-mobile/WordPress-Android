package org.wordpress.android.viewmodel.pages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PageParentSearchViewModel
@Inject constructor(
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    private val _searchResult: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchResult: LiveData<List<PageItem>> = _searchResult

    private var isStarted: Boolean = false
    private lateinit var pageParentViewModel: PageParentViewModel

    fun start(pageParentViewModel: PageParentViewModel) {
        this.pageParentViewModel = pageParentViewModel

        if (!isStarted) {
            isStarted = true

            pageParentViewModel.searchPages.observeForever(searchObserver)
        }
    }

    override fun onCleared() {
        pageParentViewModel.searchPages.removeObserver(searchObserver)
    }

    private val searchObserver = Observer<List<PageItem>?> { pageItems ->
        if (pageItems != null) {
            loadFoundPages(pageItems)
        } else {
            _searchResult.value = listOf(Empty(R.string.pages_search_suggestion, true))
        }
    }

    fun onParentSelected(page: ParentPage) {
        pageParentViewModel.onParentSelected(page)
    }

    private fun loadFoundPages(pageItems: List<PageItem>) = launch {
        if (pageItems.isNotEmpty()) {
            _searchResult.value = pageItems
        } else {
            _searchResult.value = listOf(Empty(R.string.pages_empty_search_result, true))
        }
    }
}
