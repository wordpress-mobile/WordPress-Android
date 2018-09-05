package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
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
import org.wordpress.android.modules.UI_CONTEXT
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
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.ERROR
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
import javax.inject.Inject
import javax.inject.Named

class PagesViewModel
@Inject constructor(
    private val pageStore: PageStore,
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    private val uploadUtil: PageUploadUtil,
    @Named(UI_CONTEXT) private val uiContext: CoroutineDispatcher
) : ViewModel() {
    private val _isSearchExpanded = MutableLiveData<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private val _searchResult: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchResult: LiveData<List<PageItem>> = _searchResult

    private val _listState = MutableLiveData<PageListState>()
    val listState: LiveData<PageListState> = _listState

    private val _displayDeleteDialog = SingleLiveEvent<Page>()
    val displayDeleteDialog: LiveData<Page> = _displayDeleteDialog

    private val _isNewPageButtonVisible = MutableLiveData<Boolean>()
    val isNewPageButtonVisible: LiveData<Boolean> = _isNewPageButtonVisible

    private val _refreshPages = SingleLiveEvent<Unit>()
    val refreshPages: LiveData<Unit> = _refreshPages

    private val _createNewPage = SingleLiveEvent<Unit>()
    val createNewPage: LiveData<Unit> = _createNewPage

    private val _editPage = SingleLiveEvent<PageModel?>()
    val editPage: LiveData<PageModel?> = _editPage

    private val _previewPage = SingleLiveEvent<PageModel?>()
    val previewPage: LiveData<PageModel?> = _previewPage

    private val _setPageParent = SingleLiveEvent<PageModel?>()
    val setPageParent: LiveData<PageModel?> = _setPageParent

    private var _pages: Map<Long, PageModel> = emptyMap()
    val pages: Map<Long, PageModel>
        get() {
            checkIfNewPageButtonShouldBeVisible()
            return _pages
        }

    private val _showSnackbarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    private lateinit var _site: SiteModel
    val site: SiteModel
        get() = _site

    private var _lastSearchQuery = ""
    val lastSearchQuery: String
        get() = _lastSearchQuery

    private var searchJob: Job? = null
    private var statusPageSnackbarMessage: SnackbarMessageHolder? = null
    private var currentPageType = PageStatus.PUBLISHED

    init {
        dispatcher.register(this)
    }

    fun start(site: SiteModel) {
        _site = site

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

    private suspend fun refreshPages(state: PageListState = REFRESHING) {
        updateListState(state)

        val result = pageStore.requestPagesFromServer(site)
        if (result.isError) {
            updateListState(ERROR)
            _showSnackbarMessage.postValue(
                    SnackbarMessageHolder(resourceProvider.getString(string.error_refresh_pages)))
            AppLog.e(AppLog.T.PAGES, "An error occurred while fetching the Pages")
        } else {
            _pages = pageStore.getPagesFromDb(site).associateBy { it.remoteId }

            updateListState(DONE)
            _refreshPages.asyncCall()
        }
    }

    private suspend fun updateListState(newState: PageListState) = withContext(uiContext) {
        _listState.value = newState
    }

    fun onPageEditFinished(pageId: Long) {
        launch {
            if (!uploadUtil.isPageUploading(pageId, site)) {
                refreshPages()
            }
        }
    }

    fun onPageParentSet(pageId: Long, parentId: Long) {
        launch {
            pages[pageId]?.let { page ->
                if (page.parent?.remoteId != parentId) {
                    page.parent = pages[parentId]

                    statusPageSnackbarMessage = SnackbarMessageHolder(
                            resourceProvider.getString(string.page_parent_changed))

                    uploadUtil.uploadPage(page)
                    refreshPages()
                }
            }
        }
    }

    fun onPageTypeChanged(type: PageStatus) {
        currentPageType = type
        checkIfNewPageButtonShouldBeVisible()
    }

    private fun checkIfNewPageButtonShouldBeVisible() {
        val isNotEmpty = _pages.values.any { it.status == currentPageType }
        _isNewPageButtonVisible.postValue(isNotEmpty && currentPageType != TRASHED)
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
                        _searchResult.postValue(listOf(Empty(string.pages_empty_search_result, true)))
                    }
                }
            }
        } else {
            clearSearch()
        }
    }

    suspend fun search(searchQuery: String): MutableList<PageItem> {
        _lastSearchQuery = searchQuery
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
        _isNewPageButtonVisible.postValue(false)
        return true
    }

    fun onSearchCollapsed(): Boolean {
        _isSearchExpanded.postValue(false)
        clearSearch()

        launch {
            delay(500)
            checkIfNewPageButtonShouldBeVisible()
        }
        return true
    }

    fun onMenuAction(action: Action, page: Page): Boolean {
        when (action) {
            VIEW_PAGE -> _previewPage.postValue(pages[page.id])
            SET_PARENT -> _setPageParent.postValue(pages[page.id])
            MOVE_TO_DRAFT -> changePageStatus(page.id, DRAFT)
            MOVE_TO_TRASH -> changePageStatus(page.id, TRASHED)
            PUBLISH_NOW -> changePageStatus(page.id, PUBLISHED)
            DELETE_PERMANENTLY -> _displayDeleteDialog.postValue(page)
        }
        return true
    }

    fun onDeleteConfirmed(remoteId: Long) {
        launch {
            val page = pages[remoteId]
            if (page != null) {
                pageStore.deletePageFromServer(page)
                refreshPages()

                onSearch(lastSearchQuery)

                _showSnackbarMessage.postValue(
                        SnackbarMessageHolder(resourceProvider.getString(string.page_permanently_deleted)))
            } else {
                _showSnackbarMessage.postValue(
                        SnackbarMessageHolder(resourceProvider.getString(string.page_delete_error)))
            }
        }
    }

    fun onItemTapped(pageItem: Page) {
        _editPage.postValue(pages[pageItem.id])
    }

    fun onNewPageButtonTapped() {
        _createNewPage.asyncCall()
    }

    fun onPullToRefresh() {
        launch {
            refreshPages(FETCHING)
        }
    }

    private fun changePageStatus(remoteId: Long, status: PageStatus) {
        pages[remoteId]
                ?.let { page ->
                    launch {
                        statusPageSnackbarMessage = prepareStatusChangeSnackbar(status, page)
                        page.status = status
                        uploadUtil.uploadPage(page)
                    }
                }
    }

    private fun prepareStatusChangeSnackbar(newStatus: PageStatus, page: PageModel? = null): SnackbarMessageHolder {
        val message = when (newStatus) {
            DRAFT -> resourceProvider.getString(string.page_moved_to_draft)
            PUBLISHED -> resourceProvider.getString(string.page_moved_to_published)
            TRASHED -> resourceProvider.getString(string.page_moved_to_trash)
            SCHEDULED -> resourceProvider.getString(string.page_moved_to_scheduled)
        }

        return if (page != null) {
            val previousStatus = page.status
            SnackbarMessageHolder(message, resourceProvider.getString(string.undo)) {
                changePageStatus(page.remoteId, previousStatus)
            }
        } else {
            SnackbarMessageHolder(message)
        }
    }

    private fun clearSearch() {
        _lastSearchQuery = ""
        _searchResult.postValue(listOf(Empty(string.pages_search_suggestion, true)))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post.isPage) {
            if (event.isError) {
                _showSnackbarMessage.postValue(
                        SnackbarMessageHolder(resourceProvider.getString(string.page_upload_error)))
            } else {
                launch {
                    refreshPages()
                    onSearch(lastSearchQuery)

                    if (statusPageSnackbarMessage != null) {
                        _showSnackbarMessage.postValue(statusPageSnackbarMessage!!)
                        statusPageSnackbarMessage = null
                    } else {
                        _showSnackbarMessage.postValue(prepareStatusChangeSnackbar(PageModel(event.post, site).status))
                    }
                }
            }
        }
    }
}
