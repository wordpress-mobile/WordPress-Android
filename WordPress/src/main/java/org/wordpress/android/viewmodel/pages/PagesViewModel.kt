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
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.REMOVE
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPLOAD
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.ERROR
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.properties.Delegates

class PagesViewModel
@Inject constructor(
    private val pageStore: PageStore,
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    private val actionPerfomer: ActionPerformer
) : ViewModel() {
    private val _isSearchExpanded = SingleLiveEvent<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private val _searchResult: MutableLiveData<List<PageItem>> = MutableLiveData()
    val searchResult: LiveData<List<PageItem>> = _searchResult

    private val _listState = MutableLiveData<PageListState>()
    val listState: LiveData<PageListState> = _listState

    private val _displayDeleteDialog = SingleLiveEvent<Page>()
    val displayDeleteDialog: LiveData<Page> = _displayDeleteDialog

    private val _refreshPageLists = SingleLiveEvent<Unit>()
    val refreshPageLists: LiveData<Unit> = _refreshPageLists

    private val _isNewPageButtonVisible = SingleLiveEvent<Boolean>()
    val isNewPageButtonVisible: LiveData<Boolean> = _isNewPageButtonVisible

    private val _createNewPage = SingleLiveEvent<Unit>()
    val createNewPage: LiveData<Unit> = _createNewPage

    private val _editPage = SingleLiveEvent<PageModel?>()
    val editPage: LiveData<PageModel?> = _editPage

    private val _previewPage = SingleLiveEvent<PageModel?>()
    val previewPage: LiveData<PageModel?> = _previewPage

    private val _setPageParent = SingleLiveEvent<PageModel?>()
    val setPageParent: LiveData<PageModel?> = _setPageParent

    private var _pages: MutableMap<Long, PageModel>
        by Delegates.observable(mutableMapOf()) { _, _, _ ->
            checkIfNewPageButtonShouldBeVisible()
        }
    val pages: Map<Long, PageModel>
        get() = _pages

    private val _showSnackbarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    private lateinit var _site: SiteModel
    val site: SiteModel
        get() = _site

    private var searchJob: Job? = null
    private var lastSearchQuery = ""
    private val pageUpdateContinuation = mutableMapOf<Long, Continuation<Unit>>()
    private var currentPageType = PageStatus.PUBLISHED

    fun start(site: SiteModel) {
        _site = site

        clearSearch()
        reloadPagesAsync()
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)

        actionPerfomer.onCleanup()
    }

    private fun reloadPagesAsync() = launch(CommonPool) {
        _pages = pageStore.getPagesFromDb(site).associateBy { it.remoteId }.toMutableMap()

        val loadState = if (_pages.isEmpty()) FETCHING else REFRESHING
        reloadPages(loadState)
    }

    private suspend fun reloadPages(state: PageListState = REFRESHING) {
        var newState = state
        _listState.postValue(newState)

        val result = pageStore.requestPagesFromServer(site)
        if (result.isError) {
            newState = ERROR
            _showSnackbarMessage.postValue(SnackbarMessageHolder(string.error_refresh_pages))
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Pages")
        } else {
            refreshPages()
            newState = DONE
        }

        _listState.postValue(newState)
    }

    private suspend fun refreshPages() {
        _pages = pageStore.getPagesFromDb(site).associateBy { it.remoteId }.toMutableMap()
        _refreshPageLists.asyncCall()
    }

    fun onPageEditFinished(pageId: Long) {
        launch {
            waitForPageUpdate(pageId)
            reloadPages()
        }
    }

    private suspend fun waitForPageUpdate(pageId: Long) {
        suspendCoroutine<Unit> { cont ->
            pageUpdateContinuation[pageId] = cont
        }
        pageUpdateContinuation.remove(pageId)
    }

    fun onPageParentSet(pageId: Long, parentId: Long) {
        launch {
            pages[pageId]?.let { page ->
                setParent(page, parentId)
            }
        }
    }

    fun onPageTypeChanged(type: PageStatus) {
        currentPageType = type
        checkIfNewPageButtonShouldBeVisible()
    }

    private fun checkIfNewPageButtonShouldBeVisible() {
        val isNotEmpty = _pages.values.any { it.status == currentPageType }
        _isNewPageButtonVisible.postValue(isNotEmpty)
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
        clearSearch()
        _isSearchExpanded.postValue(true)
        return true
    }

    fun onSearchCollapsed(): Boolean {
        _isSearchExpanded.postValue(false)
        return true
    }

    fun onMenuAction(action: Action, page: Page): Boolean {
        when (action) {
            VIEW_PAGE -> _previewPage.postValue(pages[page.id])
            SET_PARENT -> _setPageParent.postValue(pages[page.id])
            MOVE_TO_DRAFT -> changePageStatus(page.id, DRAFT)
            MOVE_TO_TRASH -> changePageStatus(page.id, TRASHED)
            PUBLISH_NOW -> {
                pages[page.id]?.date = Date()
                changePageStatus(page.id, PUBLISHED)
            }
            DELETE_PERMANENTLY -> _displayDeleteDialog.postValue(page)
        }
        return true
    }

    fun onDeleteConfirmed(remoteId: Long) {
        launch {
            pages[remoteId]?.let { deletePage(it)}
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
            reloadPages(FETCHING)
        }
    }

    private fun setParent(page: PageModel, parentId: Long) {
        val oldParent = page.parent?.remoteId ?: 0

        val action = PageAction(UPLOAD) {
            launch(CommonPool) {
                if (page.parent?.remoteId != parentId) {
                    page.parent = pages[parentId]
                    _refreshPageLists.asyncCall()

                    pageStore.uploadPageToServer(page)
                }
            }
        }
        action.undo = {
            launch(CommonPool) {
                page.parent = pages[oldParent]
                _refreshPageLists.asyncCall()

                pageStore.uploadPageToServer(page)
            }
        }
        action.onSuccess = {
            launch(CommonPool) {
                reloadPages()
                onSearch(lastSearchQuery)

                delay(100)
                _showSnackbarMessage.postValue(
                        SnackbarMessageHolder(string.page_parent_changed, string.undo, action.undo))
            }
        }
        action.onError = {
            launch(CommonPool) {
                refreshPages()

                _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_parent_change_error))
            }
        }
        launch {
            actionPerfomer.performAction(action)
        }
    }

    private fun deletePage(page: PageModel) {
        val action = PageAction(REMOVE) {
            launch(CommonPool) {
                _pages.remove(page.remoteId)
                _refreshPageLists.asyncCall()

                pageStore.deletePageFromServer(page)
            }
        }
        action.onSuccess = {
            launch(CommonPool) {
                delay(100)
                reloadPages()
                onSearch(lastSearchQuery)

                _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_permanently_deleted))
            }
        }
        action.onError = {
            launch(CommonPool) {
                refreshPages()

                _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_delete_error))
            }
        }
        launch {
            actionPerfomer.performAction(action)
        }
    }

    private fun changePageStatus(remoteId: Long, status: PageStatus) {
        pages[remoteId]?.let { page ->
            val oldStatus = page.status
            val action = PageAction(UPLOAD) {
                page.status = status
                launch(CommonPool) {
                    pageStore.updatePageInDb(page)
                    refreshPages()

                    pageStore.uploadPageToServer(page)
                }
            }
            action.undo = {
                page.status = oldStatus
                launch(CommonPool) {
                    pageStore.updatePageInDb(page)
                    refreshPages()

                    pageStore.uploadPageToServer(page)
                }
            }
            action.onSuccess = {
                launch(CommonPool) {
                    delay(100)
                    reloadPages()
                    onSearch(lastSearchQuery)

                    val message = prepareStatusChangeSnackbar(status, action.undo)
                    _showSnackbarMessage.postValue(message)
                }
            }
            action.onError = {
                launch(CommonPool) {
                    action.undo()

                    _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_status_change_error))
                }
            }
        launch {
                actionPerfomer.performAction(action)
            }
        }
    }

    private fun prepareStatusChangeSnackbar(newStatus: PageStatus, undo: (() -> Unit)? = null): SnackbarMessageHolder {
        val message = when (newStatus) {
            DRAFT -> string.page_moved_to_draft
            PUBLISHED -> string.page_moved_to_published
            TRASHED -> string.page_moved_to_trash
            SCHEDULED -> string.page_moved_to_scheduled
        }

        return if (undo != null) {
            SnackbarMessageHolder(message, string.undo, undo)
        } else {
            SnackbarMessageHolder(message)
        }
    }

    private fun clearSearch() {
        _searchResult.postValue(listOf(Empty(string.pages_search_suggestion, true)))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        pageUpdateContinuation[event.post.remotePostId]?.resume(Unit)
        pageUpdateContinuation[0]?.resume(Unit)
    }
}
