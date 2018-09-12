package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
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
import org.wordpress.android.modules.COMMON_POOL_CONTEXT
import org.wordpress.android.modules.UI_CONTEXT
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.AppLog
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
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class PagesViewModel
@Inject constructor(
    private val pageStore: PageStore,
    private val dispatcher: Dispatcher,
    private val actionPerfomer: ActionPerformer,
    @Named(UI_CONTEXT) private val uiContext: CoroutineDispatcher,
    @Named(COMMON_POOL_CONTEXT) private val commonPoolContext: CoroutineDispatcher
) : ViewModel() {
    private val _isSearchExpanded = MutableLiveData<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private val _listState = MutableLiveData<PageListState>()
    val listState: LiveData<PageListState> = _listState

    private val _displayDeleteDialog = SingleLiveEvent<Page>()
    val displayDeleteDialog: LiveData<Page> = _displayDeleteDialog

    private val _isNewPageButtonVisible = MutableLiveData<Boolean>()
    val isNewPageButtonVisible: LiveData<Boolean> = _isNewPageButtonVisible

    private val _pages = MutableLiveData<List<PageModel>>()
    val pages: LiveData<List<PageModel>> = _pages

    private val _searchPages: MutableLiveData<SortedMap<PageStatus, List<PageModel>>> = MutableLiveData()
    val searchPages: LiveData<SortedMap<PageStatus, List<PageModel>>> = _searchPages

    private val _createNewPage = SingleLiveEvent<Unit>()
    val createNewPage: LiveData<Unit> = _createNewPage

    private val _editPage = SingleLiveEvent<PageModel?>()
    val editPage: LiveData<PageModel?> = _editPage

    private val _previewPage = SingleLiveEvent<PageModel?>()
    val previewPage: LiveData<PageModel?> = _previewPage

    private val _setPageParent = SingleLiveEvent<PageModel?>()
    val setPageParent: LiveData<PageModel?> = _setPageParent

    private var pageMap: Map<Long, PageModel> = mapOf()
        set(value) {
            field = value
            _pages.postValue(field.values.toList())

            if (isSearchExpanded.value == true) {
                onSearch(lastSearchQuery)
            }
        }

    private val _showSnackbarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    private lateinit var _site: SiteModel
    val site: SiteModel
        get() = _site

    private var _arePageActionsEnabled = true
    val arePageActionsEnabled: Boolean
        get() = _arePageActionsEnabled

    private var _lastSearchQuery = ""
    val lastSearchQuery: String
        get() = _lastSearchQuery

    private var searchJob: Job? = null
    private val pageUpdateContinuation = mutableMapOf<Long, Continuation<Unit>>()
    private var currentPageType = PageStatus.PUBLISHED

    fun start(site: SiteModel) {
        _site = site

        reloadPagesAsync()
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)

        actionPerfomer.onCleanup()
    }

    private fun reloadPagesAsync() = launch(commonPoolContext) {
        refreshPages()

        val loadState = if (pageMap.isEmpty()) FETCHING else REFRESHING
        reloadPages(loadState)
    }

    private suspend fun reloadPages(state: PageListState = REFRESHING) {
        _listState.setOnUi(state)

        val result = pageStore.requestPagesFromServer(site)
        if (result.isError) {
            _listState.setOnUi(ERROR)
            _showSnackbarMessage.postValue(SnackbarMessageHolder(string.error_refresh_pages))
            AppLog.e(AppLog.T.PAGES, "An error occurred while fetching the Pages")
        } else {
            _listState.setOnUi(DONE)
            refreshPages()
        }
    }

    private suspend fun refreshPages() {
        pageMap = pageStore.getPagesFromDb(site).associateBy { it.remoteId }
    }

    fun onPageEditFinished(pageId: Long) {
        launch(uiContext) {
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
        launch(uiContext) {
            pageMap[pageId]?.let { page ->
                setParent(page, parentId)
            }
        }
    }

    fun onPageTypeChanged(type: PageStatus) {
        currentPageType = type
        checkIfNewPageButtonShouldBeVisible()
    }

    fun checkIfNewPageButtonShouldBeVisible() {
        val isNotEmpty = pageMap.values.any { it.status == currentPageType }
        _isNewPageButtonVisible.postOnUi(isNotEmpty && currentPageType != TRASHED && _isSearchExpanded.value != true)
    }

    fun  onSearch(searchQuery: String, delay: Long = 200) {
        searchJob?.cancel()
        if (searchQuery.isNotEmpty()) {
            searchJob = launch(uiContext) {
                delay(delay)
                searchJob = null
                if (isActive) {
                    _lastSearchQuery = searchQuery
                    val result = pageStore.groupedSearch(site, searchQuery)
                    _searchPages.value = result
                }
            }
        } else {
            clearSearch()
        }
    }

    fun onSearchExpanded(restorePreviousSearch: Boolean): Boolean {
        if (!restorePreviousSearch) {
            clearSearch()
        }

        _isSearchExpanded.value = true
        _isNewPageButtonVisible.value = false

        return true
    }

    fun onSearchCollapsed(): Boolean {
        _isSearchExpanded.value = false
        clearSearch()

        launch(uiContext) {
            delay(500)
            checkIfNewPageButtonShouldBeVisible()
        }
        return true
    }

    fun onMenuAction(action: Action, page: Page): Boolean {
        when (action) {
            VIEW_PAGE -> _previewPage.postValue(pageMap[page.id])
            SET_PARENT -> _setPageParent.postValue(pageMap[page.id])
            MOVE_TO_DRAFT -> changePageStatus(page.id, DRAFT)
            MOVE_TO_TRASH -> changePageStatus(page.id, TRASHED)
            PUBLISH_NOW -> publishPageNow(page.id)
            DELETE_PERMANENTLY -> _displayDeleteDialog.postValue(page)
        }
        return true
    }

    private fun publishPageNow(remoteId: Long) {
        pageMap[remoteId]?.date = Date()
        changePageStatus(remoteId, PUBLISHED)
    }

    fun onDeleteConfirmed(remoteId: Long) {
        launch(commonPoolContext) {
            pageMap[remoteId]?.let { deletePage(it) }
        }
    }

    fun onItemTapped(pageItem: Page) {
        _editPage.postValue(pageMap[pageItem.id])
    }

    fun onNewPageButtonTapped() {
        _createNewPage.asyncCall()
    }

    fun onPullToRefresh() {
        launch(uiContext) {
            reloadPages(FETCHING)
        }
    }

    private fun setParent(page: PageModel, parentId: Long) {
        val oldParent = page.parent?.remoteId ?: 0

        val action = PageAction(UPLOAD) {
            launch(commonPoolContext) {
                if (page.parent?.remoteId != parentId) {
                    page.parent = pageMap[parentId]

                    pageStore.uploadPageToServer(page)
                }
            }
        }
        action.undo = {
            launch(commonPoolContext) {
                pageMap[page.remoteId]?.let { changed ->
                    changed.parent = pageMap[oldParent]

                    pageStore.uploadPageToServer(changed)
                }
            }
        }
        action.onSuccess = {
            launch(commonPoolContext) {
                reloadPages()

                delay(100)
                _showSnackbarMessage.postValue(
                        SnackbarMessageHolder(string.page_parent_changed, string.undo, action.undo))
            }
        }
        action.onError = {
            launch(commonPoolContext) {
                refreshPages()

                _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_parent_change_error))
            }
        }

        launch(uiContext) {
            _arePageActionsEnabled = false
            actionPerfomer.performAction(action)
            _arePageActionsEnabled = true
        }
    }

    private fun deletePage(page: PageModel) {
        val action = PageAction(REMOVE) {
            launch(commonPoolContext) {
                pageMap = pageMap.filter { it.key != page.remoteId }

                checkIfNewPageButtonShouldBeVisible()

                pageStore.deletePageFromServer(page)
            }
        }
        action.onSuccess = {
            launch(commonPoolContext) {
                delay(100)
                reloadPages()

                _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_permanently_deleted))
            }
        }
        action.onError = {
            launch(commonPoolContext) {
                refreshPages()

                _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_delete_error))
            }
        }

        launch {
            actionPerfomer.performAction(action)
        }
    }

    private fun changePageStatus(remoteId: Long, status: PageStatus) {
        pageMap[remoteId]?.let { page ->
            val oldStatus = page.status
            val action = PageAction(UPLOAD) {
                page.status = status
                launch(commonPoolContext) {
                    pageStore.updatePageInDb(page)
                    refreshPages()

                    pageStore.uploadPageToServer(page)
                }
            }
            action.undo = {
                page.status = oldStatus
                launch(commonPoolContext) {
                    pageStore.updatePageInDb(page)
                    refreshPages()

                    pageStore.uploadPageToServer(page)
                }
            }
            action.onSuccess = {
                launch(commonPoolContext) {
                    delay(100)
                    reloadPages()

                    val message = prepareStatusChangeSnackbar(status, action.undo)
                    _showSnackbarMessage.postValue(message)
                }
            }
            action.onError = {
                launch(commonPoolContext) {
                    action.undo()

                    _showSnackbarMessage.postValue(SnackbarMessageHolder(string.page_status_change_error))
                }
            }

            launch(uiContext) {
                _arePageActionsEnabled = false
                actionPerfomer.performAction(action)
                _arePageActionsEnabled = true
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
        _lastSearchQuery = ""
        _searchPages.postValue(null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        pageUpdateContinuation[event.post.remotePostId]?.resume(Unit)
        pageUpdateContinuation[0]?.resume(Unit)
    }

    private suspend fun <T> MutableLiveData<T>.setOnUi(value: T) = withContext(uiContext) {
        this.value = value
    }

    private fun <T> MutableLiveData<T>.postOnUi(value: T) {
        val liveData = this
        launch(uiContext) {
            liveData.value = value
        }
    }
}

@StringRes fun PageStatus.getTitle(): Int {
    return when (this) {
        PUBLISHED -> string.pages_published
        DRAFT -> string.pages_drafts
        SCHEDULED -> string.pages_scheduled
        TRASHED -> string.pages_trashed
    }
}
