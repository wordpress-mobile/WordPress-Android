package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.networking.PageStore
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.CAN_LOAD_MORE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.ERROR
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.LOADING_MORE
import javax.inject.Inject

class PagesViewModel
@Inject constructor(private val pageStore: PageStore) : ViewModel() {
    private val _isSearchExpanded = SingleLiveEvent<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private val _searchResult: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchResult: LiveData<List<PageItem>> = _searchResult

    private val _listState = MutableLiveData<PageListState>()
    val listState: LiveData<PageListState>
        get() = _listState

    private val _refreshPages = SingleLiveEvent<Unit>()
    val refreshPages: LiveData<Unit>
        get() = _refreshPages

    private val _createNewPage = SingleLiveEvent<Unit>()
    val createNewPage: LiveData<Unit>
        get() = _createNewPage

    private var _pages: List<PageModel> = emptyList()
    val pages: List<PageModel>
        get() = _pages

    private val _showSnackbarMessage = SingleLiveEvent<String>()
    val showSnackbarMessage: LiveData<String>
        get() = _showSnackbarMessage

    private lateinit var site: SiteModel

    fun start(site: SiteModel) {
        this.site = site

        clearSearch()
        reloadPagesAsync()
    }

    private fun reloadPagesAsync() = launch(CommonPool) {
        _pages = pageStore.getPages(site)
        refreshPagesAsync()
    }

    fun refreshPagesAsync(isLoadingMore: Boolean = false) = launch(CommonPool) {
        var newState = if (isLoadingMore) LOADING_MORE else FETCHING
        _listState.postValue(newState)

        val result = pageStore.requestPagesFromServer(site, isLoadingMore)
        if (result.isError) {
            _listState.postValue(ERROR)
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Pages")
        } else if (result.rowsAffected > 0) {
            _pages = pageStore.getPages(site)
            _refreshPages.asyncCall()
        }

        newState = if (result.canLoadMore) CAN_LOAD_MORE else DONE
        _listState.postValue(newState)
    }

    fun onPageEditFinished(pageId: Int) {
        launch {
            pageStore.getPageByLocalId(pageId, site)?.let {
                refreshPagesAsync()
            }
        }
    }

    fun onSearchTextSubmit(query: String?): Boolean {
        return onSearchTextChange(query)
    }

    fun onSearchTextChange(query: String?): Boolean {
        if (!query.isNullOrEmpty()) {
            val listOf = mockResult(query)
            _searchResult.postValue(listOf)
        } else {
            clearSearch()
        }
        return true
    }

    fun onSearchExpanded(): Boolean {
        _isSearchExpanded.postValue(true)
        return true
    }

    fun onSearchCollapsed(): Boolean {
        _isSearchExpanded.postValue(false)
        return true
    }

    fun onAction(action: Action, pageItem: PageItem): Boolean {
        TODO("not implemented")
    }

    private fun clearSearch() {
        _searchResult.postValue(listOf(Empty(string.empty_list_default)))
    }

    fun onNewPageButtonTapped() {
        _createNewPage.asyncCall()
    }
}

fun mockResult(query: String?): List<PageItem> {
    // TODO remove with real data
    return listOf(
//            Divider(1, "Data for $query"),
//            Page(1, "item 1", setOf(VIEW_PAGE), 0, null),
//            Page(2, "item 2", indent = 1, icon = null),
//            Page(3, "item 3", setOf(VIEW_PAGE, PUBLISH_NOW), 2, null),
//            Divider(2, "Divider 2"),
//            Page(4, "item 4", indent = 0, icon = null)
    )
}
