package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.ERROR
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
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

    private var _pages: List<PageModel> = emptyList()
    val pages: List<PageModel>
        get() = _pages

    private lateinit var site: SiteModel
    private var searchJob: Job? = null

    fun start(site: SiteModel) {
        this.site = site

        clearSearch()
        reloadPagesAsync()
    }

    private fun reloadPagesAsync() = launch(CommonPool) {
        _pages = pageStore.loadPagesFromDb(site)
        refreshPages()
    }

    private suspend fun refreshPages() {
        var newState = FETCHING
        _listState.postValue(newState)

        val result = pageStore.requestPagesFromServer(site)
        if (result.isError) {
            newState = ERROR
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Pages")
        } else if (result.rowsAffected > 0) {
            _pages = pageStore.loadPagesFromDb(site)
            _refreshPages.asyncCall()
            newState = DONE
        }

        _listState.postValue(newState)
    }

    fun onSearch(searchQuery: String) {
        searchJob?.cancel()
        if (searchQuery.isNotEmpty()) {
            searchJob = launch {
                delay(200)
                searchJob = null
                if (isActive) {
                    val result = pageStore.groupedSearch(site, searchQuery)
                            .map { (status, results) ->
                                listOf(Divider(status.toResource())) +
                                        results.map { Page(it.pageId.toLong(), it.title, null) }
                            }
                            .fold(mutableListOf()) { acc: MutableList<PageItem>, list: List<PageItem> ->
                                acc.addAll(list)
                                acc
                            }
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

    private fun PageStatus.toResource(): Int {
        return when (this) {
            PageStatus.PUBLISHED -> string.pages_published
            PageStatus.DRAFT -> string.pages_drafts
            PageStatus.TRASHED -> string.pages_trashed
            PageStatus.SCHEDULED -> string.pages_scheduled
            PageStatus.UNKNOWN -> throw IllegalArgumentException("Unknown page type")
        }
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
