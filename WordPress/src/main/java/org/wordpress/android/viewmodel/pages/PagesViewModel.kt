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
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.model.page.PageStatus.SCHEDULED
import org.wordpress.android.fluxc.model.page.PageStatus.TRASHED
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.networking.PageUploadUtil
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.ui.pages.PageItem.ScheduledPage
import org.wordpress.android.ui.pages.PageItem.TrashedPage
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
    private val resourceProvider: ResourceProvider,
    private val uploadUtil: PageUploadUtil
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

    private val _editPage = SingleLiveEvent<PageModel?>()
    val editPage: LiveData<PageModel?>
        get() = _editPage

    private val _previewPage = SingleLiveEvent<PageModel?>()
    val previewPage: LiveData<PageModel?>
        get() = _previewPage

    private val _setPageParent = SingleLiveEvent<PageModel?>()
    val setPageParent: LiveData<PageModel?>
        get() = _setPageParent

    private var _pages: Map<Long, PageModel> = emptyMap()
    val pages: Map<Long, PageModel>
        get() = _pages

    private val _showSnackbarMessage = SingleLiveEvent<String>()
    val showSnackbarMessage: LiveData<String>
        get() = _showSnackbarMessage

    private lateinit var site: SiteModel
    private var searchJob: Job? = null
    private var lastSearchQuery = ""

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
        _pages = pageStore.getPagesFromDb(site).associateBy { it.remoteId }

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
            _pages = pageStore.getPagesFromDb(site).associateBy { it.remoteId }
            _refreshPages.asyncCall()
            newState = DONE
        }

        _listState.postValue(newState)
    }

    fun onPageEditFinished(pageId: Long) {
        launch {
            if (!uploadUtil.isPageUploading(pageId, site)) {
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
                    val result = search(searchQuery)
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

    suspend fun search(searchQuery: String): MutableList<PageItem> {
        lastSearchQuery = searchQuery
        return pageStore.groupedSearch(site, searchQuery)
                .map { (status, results) ->
                    listOf(Divider(resourceProvider.getString(status.toResource()))) + results.map { it.toPageItem() }
                }
                .fold(mutableListOf()) { acc: MutableList<PageItem>, list: List<PageItem> ->
                    acc.addAll(list)
                    return@fold acc
                }
    }

    private fun PageModel.toPageItem(): PageItem {
        return when (status) {
            PUBLISHED -> PublishedPage(remoteId, title)
            DRAFT -> DraftPage(remoteId, title)
            TRASHED -> TrashedPage(remoteId, title)
            SCHEDULED -> ScheduledPage(remoteId, title)
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

    fun onMenuAction(action: Action, pageItem: Page): Boolean {
        when (action) {
            VIEW_PAGE -> _previewPage.postValue(pages[pageItem.id])
            SET_PARENT -> _setPageParent.postValue(pages[pageItem.id])
            MOVE_TO_DRAFT -> changePageStatus(pageItem, DRAFT)
            MOVE_TO_TRASH -> changePageStatus(pageItem, TRASHED)
            PUBLISH_NOW -> changePageStatus(pageItem, PUBLISHED)
        }
        return true
    }

    private fun changePageStatus(pageItem: Page, status: PageStatus) {
        pages[pageItem.id]
                ?.let { page ->
                    launch {
                        page.status = status
                        uploadUtil.uploadPage(page)
                    }
                }
    }

    fun onItemTapped(pageItem: Page) {
        _editPage.postValue(pages[pageItem.id])
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
                onSearch(lastSearchQuery)
            }
        }
    }
}
