package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.networking.PageStore
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.CAN_LOAD_MORE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.ERROR
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.LOADING_MORE
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.coroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

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

    private var _pages: List<PageModel> = emptyList()
    val pages: List<PageModel>
        get() = _pages

    private lateinit var site: SiteModel
    private var searchContinuation: Continuation<String>? = null

    fun start(site: SiteModel) {
        this.site = site

        clearSearch()
        reloadPagesAsync()
        handleThrottledSearchAsync()
    }

    private fun handleThrottledSearchAsync() = launch(CommonPool) {
        var query: String
        while (true) {
            val searchQuery = suspendCoroutine<String> { continuation ->
                searchContinuation = continuation
            }
            if (searchQuery.isNotEmpty()) {
                launch(coroutineContext) {
                    query = searchQuery
                    delay(200)
                    if (query == searchQuery) {
                        val result = pageStore.search(site, searchQuery).map { Page(it.pageId.toLong(), it.title, null) }
                        if (result.isNotEmpty()) {
                            _searchResult.postValue(result)
                        } else {
                            _searchResult.postValue(listOf(Empty(string.pages_empty_search_result)))
                        }
                    }
                }
            } else {
                clearSearch()
            }
        }
    }

    private fun reloadPagesAsync() = launch(CommonPool) {
        _pages = pageStore.loadPagesFromDb(site)
        refreshPages()
    }

    private suspend fun refreshPages(isLoadingMore: Boolean = false) {
        var newState = if (isLoadingMore) LOADING_MORE else FETCHING
        _listState.postValue(newState)

        val result = pageStore.requestPagesFromServer(site, isLoadingMore)
        if (result.isError) {
            _listState.postValue(ERROR)
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Pages")
        } else if (result.rowsAffected > 0) {
            _pages = pageStore.loadPagesFromDb(site)
            _refreshPages.asyncCall()
        }

        newState = if (result.canLoadMore) CAN_LOAD_MORE else DONE
        _listState.postValue(newState)
    }

    fun onSearch(query: String) {
        searchContinuation?.resume(query)
    }

    fun onSearchExpanded(): Boolean {
        _isSearchExpanded.postValue(true)
        return true
    }

    fun onSearchCollapsed(): Boolean {
        _isSearchExpanded.postValue(false)
        return true
    }

    fun refresh() {
        launch(CommonPool) {
            refreshPages()
        }
    }

    fun onAction(action: Action, pageItem: PageItem): Boolean {
        TODO("not implemented")
    }

    private fun clearSearch() {
        _searchResult.postValue(listOf(Empty(string.empty_list_default)))
    }
}
