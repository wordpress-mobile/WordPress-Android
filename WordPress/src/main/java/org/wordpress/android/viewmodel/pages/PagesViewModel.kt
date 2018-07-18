package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.networking.PageStore
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Singleton

class PagesViewModel
@Inject constructor(private val pageStore: PageStore) : ViewModel() {
    private val mutableSearchExpanded: MutableLiveData<Boolean> = MutableLiveData()
    val searchExpanded: LiveData<Boolean> = mutableSearchExpanded

    private val mutableSearchResult: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchResult: LiveData<List<PageItem>> = mutableSearchResult

    private val _refreshPages = SingleLiveEvent<Unit>()
    val refreshPages: LiveData<Unit>
        get() = _refreshPages

    private var _pages: List<PageModel> = emptyList()
    var pages: List<PageModel> = _pages
        get() = _pages

    fun start(site: SiteModel) {
        clear()
        loadPagesAsync(site)
    }

    private fun loadPagesAsync(site: SiteModel) {
        launch(UI) {
            val result = pageStore.fetchPagesAsync(site, false)
            if (!result.isError) {
                _pages = pageStore.loadPostsAsync(site)
                _refreshPages.asyncCall()
            }
        }
    }

    fun onSearchTextSubmit(query: String?): Boolean {
        return onSearchTextChange(query)
    }

    fun onSearchTextChange(query: String?): Boolean {
        if (!query.isNullOrEmpty()) {
            val listOf = mockResult(query)
            mutableSearchResult.postValue(listOf)
        } else {
            clear()
        }
        return true
    }

    fun onSearchExpanded(): Boolean {
        mutableSearchExpanded.postValue(true)
        return true
    }

    fun onSearchCollapsed(): Boolean {
        mutableSearchExpanded.postValue(false)
        return true
    }

    fun refresh() {
    }

    fun onAction(action: Action, pageItem: PageItem): Boolean {
        TODO("not implemented")
    }

    private fun clear() {
        mutableSearchResult.postValue(listOf(Empty(string.empty_list_default)))
    }
}

fun mockResult(query: String?): List<PageItem> {
    // TODO remove with real data
    return listOf(
            Divider(1, "Data for $query"),
            Page(1, "item 1", null, 0, setOf(VIEW_PAGE)),
            Page(2, "item 2", null, 1),
            Page(3, "item 3", null, 2, setOf(VIEW_PAGE, PUBLISH_NOW)),
            Divider(2, "Divider 2"),
            Page(4, "item 4", null, 0)
    )
}
