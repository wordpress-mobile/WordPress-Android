package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.models.pages.PageStatus
import org.wordpress.android.networking.PageStore
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.ERROR
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
import javax.inject.Inject

class PagesViewModel
@Inject constructor(
    private val pageStore: PageStore,
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
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
    private var searchJob: Job? = null

    init {
        dispatcher.register(this)
    }

    fun start(site: SiteModel) {
        this.site = site

        clearSearch()
        reloadPagesAsync()
    }

    override fun onCleared() {
        dispatcher.unregister(this)
    }

    private fun reloadPagesAsync() = launch(CommonPool) {
        _pages = pageStore.getPages(site)

        val loadState = if (_pages.isEmpty()) FETCHING else REFRESHING
        refreshPages(loadState)
    }

    suspend fun refreshPages(state: PageListState = REFRESHING) {
        var newState = state
        _listState.postValue(newState)

        val result = pageStore.requestPagesFromServer(site)
        if (result.isError) {
            newState = ERROR
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Pages")
        } else if (result.rowsAffected > 0) {
            _pages = pageStore.getPages(site)
            _refreshPages.asyncCall()
            newState = DONE
        }

        _listState.postValue(newState)
    }

    fun onPageEditFinished(pageId: Long) {
        launch {
            if (!pageStore.isPageUploading(pageId, site)) {
                refreshPages()
            }
        }
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
                                listOf(Divider(resourceProvider.getString(status.toResource()))) +
                                        results.map { PublishedPage(it.pageId.toLong(), it.title) }
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

    fun onMenuAction(action: Action, pageItem: PageItem): Boolean {
        TODO("not implemented")
    }

    fun onItemTapped(pageItem: Page) {

    }

    private fun clearSearch() {
        _searchResult.postValue(listOf(Empty(string.empty_list_default)))
    }

    fun onNewPageButtonTapped() {
        _createNewPage.asyncCall()
    }

    fun onPullToRefresh() {
        launch {
            refreshPages(FETCHING)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post.isPage) {
            launch {
                refreshPages()
            }
        }
    }
}
