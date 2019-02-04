package org.wordpress.android.viewmodel.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_OPTIONS_PRESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_SEARCH_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_TAB_PRESSED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.TRASHED
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
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
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.coroutines.suspendCoroutineWithTimeout
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.DELETE
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPDATE
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPLOAD
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.DONE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.ERROR
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.REFRESHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import java.util.Date
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val ACTION_DELAY = 100L
private const val SEARCH_DELAY = 200L
private const val SNACKBAR_DELAY = 500L
private const val SEARCH_COLLAPSE_DELAY = 500L
private const val PAGE_UPLOAD_TIMEOUT = 5000L

class PagesViewModel
@Inject constructor(
    private val pageStore: PageStore,
    private val dispatcher: Dispatcher,
    private val actionPerfomer: ActionPerformer,
    private val networkUtils: NetworkUtilsWrapper,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val defaultDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
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

    private val _searchPages: MutableLiveData<SortedMap<PageListType, List<PageModel>>> = MutableLiveData()
    val searchPages: LiveData<SortedMap<PageListType, List<PageModel>>> = _searchPages

    private val _createNewPage = SingleLiveEvent<Unit>()
    val createNewPage: LiveData<Unit> = _createNewPage

    private val _editPage = SingleLiveEvent<PageModel?>()
    val editPage: LiveData<PageModel?> = _editPage

    private val _previewPage = SingleLiveEvent<PageModel?>()
    val previewPage: LiveData<PageModel?> = _previewPage

    private val _setPageParent = SingleLiveEvent<PageModel?>()
    val setPageParent: LiveData<PageModel?> = _setPageParent

    private val _scrollToPage = SingleLiveEvent<PageModel>()
    val scrollToPage: LiveData<PageModel?> = _scrollToPage

    private var isInitialized = false
    private var scrollToPageId: Long? = null

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

    private var _site: SiteModel? = null
    val site: SiteModel
        get() = checkNotNull(_site) { "Trying to access unitialized site" }

    private var _arePageActionsEnabled = true
    val arePageActionsEnabled: Boolean
        get() = _arePageActionsEnabled

    private var _lastSearchQuery = ""
    val lastSearchQuery: String
        get() = _lastSearchQuery

    private var searchJob: Job? = null
    private var pageUpdateContinuations: MutableMap<Long, Continuation<Unit>> = mutableMapOf()
    private var currentPageType = PageListType.PUBLISHED

    fun start(site: SiteModel) {
        // Check if VM is not already initialized
        if (_site == null) {
            _site = site

            loadPagesAsync()
        }
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)

        actionPerfomer.onCleanup()
    }

    private fun loadPagesAsync() = launch(defaultDispatcher) {
        refreshPages()

        val loadState = if (pageMap.isEmpty()) FETCHING else REFRESHING
        reloadPages(loadState)

        isInitialized = true

        scrollToPageId?.let {
            onSpecificPageRequested(it)
        }
    }

    private suspend fun reloadPages(state: PageListState = REFRESHING) {
        if (performIfNetworkAvailableAsync {
            _listState.setOnUi(state)

            val result = pageStore.requestPagesFromServer(site)
            if (result.isError) {
                _listState.setOnUi(ERROR)
                showSnackbar(SnackbarMessageHolder(string.error_refresh_pages))
                AppLog.e(AppLog.T.PAGES, "An error occurred while fetching the Pages")
            } else {
                _listState.setOnUi(DONE)
            }
            refreshPages()
        }) else {
            _listState.setOnUi(DONE)
        }
    }

    private suspend fun refreshPages() {
        pageMap = pageStore.getPagesFromDb(site).associateBy { it.remoteId }
    }

    fun onPageEditFinished(remotePageId: Long, wasPageUpdated: Boolean) {
        launch {
            refreshPages() // show local changes immediately

            if (wasPageUpdated) {
                performIfNetworkAvailableAsync {
                    waitForPageUpdate(remotePageId)
                    reloadPages()
                }
            }
        }
    }

    private suspend fun waitForPageUpdate(remotePageId: Long) {
        _arePageActionsEnabled = false
        suspendCoroutineWithTimeout<Unit>(PAGE_UPLOAD_TIMEOUT) { cont ->
            pageUpdateContinuations[remotePageId] = cont
        }
        _arePageActionsEnabled = true
    }

    fun onPageParentSet(pageId: Long, parentId: Long) {
        launch {
            pageMap[pageId]?.let { page ->
                setParent(page, parentId)
            }
        }
    }

    fun onPageTypeChanged(type: PageListType) {
        trackTabChangeEvent(type)

        currentPageType = type
        checkIfNewPageButtonShouldBeVisible()
    }

    private fun trackTabChangeEvent(type: PageListType) {
        val tab = when (type) {
            PUBLISHED -> "published"
            DRAFTS -> "drafts"
            SCHEDULED -> "scheduled"
            PageListType.TRASHED -> "binned"
        }
        val properties = mutableMapOf("tab_name" to tab as Any)
        AnalyticsUtils.trackWithSiteDetails(PAGES_TAB_PRESSED, site, properties)
    }

    fun checkIfNewPageButtonShouldBeVisible() {
        val isNotEmpty = pageMap.values.any { currentPageType.pageStatuses.contains(it.status) }
        val hasNoExceptions = !currentPageType.pageStatuses.contains(PageStatus.TRASHED) &&
                _isSearchExpanded.value != true
        _isNewPageButtonVisible.postOnUi(isNotEmpty && hasNoExceptions)
    }

    fun onSearch(searchQuery: String, delay: Long = SEARCH_DELAY) {
        searchJob?.cancel()
        if (searchQuery.isNotEmpty()) {
            searchJob = launch {
                delay(delay)
                searchJob = null
                if (isActive) {
                    _lastSearchQuery = searchQuery
                    val result = groupedSearch(site, searchQuery)
                    _searchPages.postValue(result)
                }
            }
        } else {
            clearSearch()
        }
    }

    fun onSpecificPageRequested(remotePageId: Long) {
        if (isInitialized) {
            val page = pageMap[remotePageId]
            if (page != null) {
                _scrollToPage.postValue(page)
            } else {
                _showSnackbarMessage.postValue(SnackbarMessageHolder(string.pages_open_page_error))
            }
        } else {
            scrollToPageId = remotePageId
        }
    }

    private suspend fun groupedSearch(
        site: SiteModel,
        searchQuery: String
    ): SortedMap<PageListType, List<PageModel>> = withContext(defaultDispatcher) {
        val list = pageStore.search(site, searchQuery).groupBy { PageListType.fromPageStatus(it.status) }
        return@withContext list.toSortedMap(
                Comparator { previous, next ->
                    when {
                        previous == next -> 0
                        previous == PageListType.PUBLISHED -> -1
                        next == PageListType.PUBLISHED -> 1
                        previous == PageListType.DRAFTS -> -1
                        next == PageListType.DRAFTS -> 1
                        previous == PageListType.SCHEDULED -> -1
                        next == PageListType.SCHEDULED -> 1
                        else -> throw IllegalArgumentException("Unexpected page type")
                    }
                })
    }

    fun onSearchExpanded(restorePreviousSearch: Boolean) {
        if (isSearchExpanded.value != true) {
            AnalyticsUtils.trackWithSiteDetails(PAGES_SEARCH_ACCESSED, site)

            if (!restorePreviousSearch) {
                clearSearch()
            }

            _isSearchExpanded.value = true
            _isNewPageButtonVisible.value = false
        }
    }

    fun onSearchCollapsed() {
        _isSearchExpanded.value = false
        clearSearch()

        launch {
            delay(SEARCH_COLLAPSE_DELAY)
            checkIfNewPageButtonShouldBeVisible()
        }
    }

    fun onMenuAction(action: Action, page: Page): Boolean {
        when (action) {
            VIEW_PAGE -> previewPage(page)
            SET_PARENT -> setParent(page)
            MOVE_TO_DRAFT -> changePageStatus(page.id, PageStatus.DRAFT)
            MOVE_TO_TRASH -> changePageStatus(page.id, PageStatus.TRASHED)
            PUBLISH_NOW -> publishPageNow(page.id)
            DELETE_PERMANENTLY -> deletePage(page)
        }
        return true
    }

    private fun deletePage(page: Page) {
        performIfNetworkAvailable {
            _displayDeleteDialog.postValue(page)
        }
    }

    private fun setParent(page: Page) {
        performIfNetworkAvailable {
            trackMenuSelectionEvent(SET_PARENT)

            _setPageParent.postValue(pageMap[page.id])
        }
    }

    private fun previewPage(page: Page) {
        performIfNetworkAvailable {
            trackMenuSelectionEvent(VIEW_PAGE)

            _previewPage.postValue(pageMap[page.id])
        }
    }

    private fun performIfNetworkAvailable(performAction: () -> Unit): Boolean {
        return if (networkUtils.isNetworkAvailable()) {
            performAction()
            true
        } else {
            _showSnackbarMessage.postValue(SnackbarMessageHolder(R.string.no_network_message))
            false
        }
    }

    private suspend fun performIfNetworkAvailableAsync(performAction: suspend () -> Unit): Boolean {
        return if (networkUtils.isNetworkAvailable()) {
            performAction()
            true
        } else {
            _showSnackbarMessage.postValue(SnackbarMessageHolder(R.string.no_network_message))
            false
        }
    }

    private fun trackMenuSelectionEvent(action: Action) {
        val menu = when (action) {
            VIEW_PAGE -> "view"
            SET_PARENT -> "set_parent"
            MOVE_TO_DRAFT -> "move_to_draft"
            MOVE_TO_TRASH -> "move_to_bin"
            else -> return
        }
        val properties = mutableMapOf("option_name" to menu as Any)
        AnalyticsUtils.trackWithSiteDetails(PAGES_OPTIONS_PRESSED, site, properties)
    }

    private fun publishPageNow(remoteId: Long) {
        performIfNetworkAvailable {
            pageMap[remoteId]?.date = Date()
            changePageStatus(remoteId, PageStatus.PUBLISHED)
        }
    }

    fun onImagesChanged() {
        launch {
            refreshPages()
        }
    }

    fun onDeleteConfirmed(remoteId: Long) {
        launch(defaultDispatcher) {
            pageMap[remoteId]?.let { deletePage(it) }
        }
    }

    fun onItemTapped(pageItem: Page) {
        _editPage.postValue(pageMap[pageItem.id])
    }

    fun onNewPageButtonTapped() {
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.PAGES_ADD_PAGE, site)

        _createNewPage.asyncCall()
    }

    fun onPullToRefresh() {
        launch {
            reloadPages(FETCHING)
        }
    }

    private fun setParent(page: PageModel, parentId: Long) {
        val oldParent = page.parent?.remoteId ?: 0

        val action = PageAction(page.remoteId, UPLOAD) {
            if (page.parent?.remoteId != parentId) {
                val updatedPage = updateParent(page, parentId)

                pageStore.uploadPageToServer(updatedPage)
            }
        }

        action.undo = {
            launch(defaultDispatcher) {
                pageMap[action.remoteId]?.let { changed ->
                    val updatedPage = updateParent(changed, oldParent)

                    pageStore.uploadPageToServer(updatedPage)
                }
            }
        }

        action.onSuccess = {
            launch(defaultDispatcher) {
                reloadPages()

                showSnackbar(
                        if (action.undo != null) {
                            SnackbarMessageHolder(string.page_parent_changed, string.undo, action.undo!!)
                        } else {
                            SnackbarMessageHolder(string.page_parent_changed)
                        }
                )
            }
        }
        action.onError = {
            launch(defaultDispatcher) {
                refreshPages()

                showSnackbar(SnackbarMessageHolder(string.page_parent_change_error))
            }
        }

        launch {
            _arePageActionsEnabled = false
            actionPerfomer.performAction(action)
            _arePageActionsEnabled = true
        }
    }

    private suspend fun showSnackbar(message: SnackbarMessageHolder) {
        delay(SNACKBAR_DELAY)
        _showSnackbarMessage.postValue(message)
    }

    private fun updateParent(page: PageModel, parentId: Long): PageModel {
        val updatedPage = page.copy(parent = pageMap[parentId])
        val updatedMap = pageMap.toMutableMap()
        updatedMap[page.remoteId] = updatedPage
        pageMap = updatedMap
        return updatedPage
    }

    private fun deletePage(page: PageModel) {
        val action = PageAction(page.remoteId, DELETE) {
            pageMap = pageMap.filter { it.key != page.remoteId }

            checkIfNewPageButtonShouldBeVisible()

            if (page.remoteId < 0) {
                pageStore.deletePageFromDb(page)
            } else {
                pageStore.deletePageFromServer(page)
            }
        }
        action.onSuccess = {
            launch(defaultDispatcher) {
                delay(ACTION_DELAY)
                reloadPages()

                showSnackbar(SnackbarMessageHolder(string.page_permanently_deleted))
            }
        }
        action.onError = {
            launch(defaultDispatcher) {
                refreshPages()

                showSnackbar(SnackbarMessageHolder(string.page_delete_error))
            }
        }

        launch {
            actionPerfomer.performAction(action)
        }
    }

    private fun changePageStatus(remoteId: Long, status: PageStatus) {
        performIfNetworkAvailable {
            if (status == DRAFT) {
                trackMenuSelectionEvent(MOVE_TO_DRAFT)
            } else if (status == TRASHED) {
                trackMenuSelectionEvent(MOVE_TO_TRASH)
            }

            pageMap[remoteId]?.let { page ->
                val oldStatus = page.status

                val action = if (status != TRASHED || remoteId > 0) {
                    PageAction(remoteId, UPLOAD) {
                        val updatedPage = updatePageStatus(page, status)
                        pageStore.updatePageInDb(updatedPage)

                        refreshPages()
                        pageStore.uploadPageToServer(updatedPage)
                    }
                } else {
                    // Local pages are trashed locally
                    PageAction(remoteId, UPDATE) {
                        val updatedPage = updatePageStatus(page, status)
                        pageStore.updatePageInDb(updatedPage)

                        refreshPages()
                    }
                }

                action.undo = {
                    val updatedPage = updatePageStatus(page.copy(remoteId = action.remoteId), oldStatus)
                    launch(defaultDispatcher) {
                        pageStore.updatePageInDb(updatedPage)
                        refreshPages()

                        pageStore.uploadPageToServer(updatedPage)
                    }
                }

                action.onSuccess = {
                    launch(defaultDispatcher) {
                        delay(ACTION_DELAY)
                        reloadPages()

                        val message = prepareStatusChangeSnackbar(status, action.undo)
                        showSnackbar(message)
                    }
                }
                action.onError = {
                    launch(defaultDispatcher) {
                        action.undo?.let { it() }

                        showSnackbar(SnackbarMessageHolder(string.page_status_change_error))
                    }
                }

            launch {
                    _arePageActionsEnabled = false
                    actionPerfomer.performAction(action)
                    _arePageActionsEnabled = true
                }
            }
        }
    }

    private fun updatePageStatus(page: PageModel, oldStatus: PageStatus): PageModel {
        val updatedPage = page.copy(status = oldStatus)
        val updatedMap = pageMap.toMutableMap()
        updatedMap[page.remoteId] = updatedPage
        pageMap = updatedMap
        return updatedPage
    }

    private fun prepareStatusChangeSnackbar(newStatus: PageStatus, undo: (() -> Unit)? = null): SnackbarMessageHolder {
        val message = when (newStatus) {
            PageStatus.DRAFT -> string.page_moved_to_draft
            PageStatus.PUBLISHED -> string.page_moved_to_published
            PageStatus.TRASHED -> string.page_moved_to_trash
            PageStatus.SCHEDULED -> string.page_moved_to_scheduled
            else -> throw NotImplementedError("Status change to ${newStatus.getTitle()} not supported")
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
        var id = 0L
        if (!pageUpdateContinuations.contains(id)) {
            id = event.post.remotePostId
        }

        pageUpdateContinuations[id]?.let { cont ->
            pageUpdateContinuations.remove(id)
            cont.resume(Unit)
        }
    }

    private suspend fun <T> MutableLiveData<T>.setOnUi(value: T) = withContext(coroutineContext) {
        setValue(value)
    }

    private fun <T> MutableLiveData<T>.postOnUi(value: T) {
        val liveData = this
        launch {
            liveData.value = value
        }
    }
}

@StringRes fun PageStatus.getTitle(): Int {
    return when (this) {
        PageStatus.PUBLISHED -> string.pages_published
        PageStatus.DRAFT -> string.pages_drafts
        PageStatus.SCHEDULED -> string.pages_scheduled
        PageStatus.TRASHED -> string.pages_trashed
        PageStatus.PENDING -> string.pages_pending
        PageStatus.PRIVATE -> string.pages_private
    }
}
