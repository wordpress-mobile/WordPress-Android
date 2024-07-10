package org.wordpress.android.viewmodel.pages

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_LIST_AUTHOR_FILTER_CHANGED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_OPTIONS_PRESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_SEARCH_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PAGES_TAB_PRESSED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront.PAGE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteOptionsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.VirtualHomepage
import org.wordpress.android.ui.pages.PagesAuthorFilterUIState
import org.wordpress.android.ui.pages.PagesListAction
import org.wordpress.android.ui.pages.PagesListAction.CANCEL_AUTO_UPLOAD
import org.wordpress.android.ui.pages.PagesListAction.COPY
import org.wordpress.android.ui.pages.PagesListAction.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PagesListAction.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PagesListAction.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PagesListAction.PROMOTE_WITH_BLAZE
import org.wordpress.android.ui.pages.PagesListAction.PUBLISH_NOW
import org.wordpress.android.ui.pages.PagesListAction.SET_AS_HOMEPAGE
import org.wordpress.android.ui.pages.PagesListAction.SET_AS_POSTS_PAGE
import org.wordpress.android.ui.pages.PagesListAction.SET_PARENT
import org.wordpress.android.ui.pages.PagesListAction.SHARE
import org.wordpress.android.ui.pages.PagesListAction.VIEW_PAGE
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.AuthorFilterListItemUIState
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.PostConflictResolutionFeatureUtils
import org.wordpress.android.ui.posts.PostInfoType
import org.wordpress.android.ui.posts.PostListRemotePreviewState
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.posts.PostResolutionOverlayActionEvent
import org.wordpress.android.ui.posts.PreviewStateHelper
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.posts.getAuthorFilterItems
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.PAGES
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.DialogHolder
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
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Named

private const val ACTION_DELAY = 100L
private const val SEARCH_DELAY = 200L
private const val SCROLL_DELAY = 200L
private const val SNACKBAR_DELAY = 500L
private const val SEARCH_COLLAPSE_DELAY = 500L
private const val TRACKS_SELECTED_AUTHOR_FILTER = "author_filter_selection"

typealias LoadAutoSaveRevision = Boolean

class PagesViewModel
@Inject constructor(
    private val pageStore: PageStore,
    private val postStore: PostStore,
    private val dispatcher: Dispatcher,
    private val actionPerformer: ActionPerformer,
    private val networkUtils: NetworkUtilsWrapper,
    private val eventBusWrapper: EventBusWrapper,
    private val siteStore: SiteStore,
    private val previewStateHelper: PreviewStateHelper,
    private val uploadStarter: UploadStarter,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    val uploadStatusTracker: PostModelUploadStatusTracker,
    private val pageListEventListenerFactory: PageListEventListener.Factory,
    private val siteOptionsStore: SiteOptionsStore,
    private val appLogWrapper: AppLogWrapper,
    private val accountStore: AccountStore,
    private val prefs: AppPrefsWrapper,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val defaultDispatcher: CoroutineDispatcher,
    private val postConflictResolutionFeatureUtils: PostConflictResolutionFeatureUtils,
    private val uploadStore: UploadStore,
    private val pageConflictDetector: PageConflictDetector
) : ScopedViewModel(uiDispatcher) {
    private val _isSearchExpanded = MutableLiveData<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded

    private val _listState = MutableLiveData<PageListState>()
    val listState: LiveData<PageListState> = _listState

    private val _isNewPageButtonVisible = MutableLiveData<Boolean>()
    val isNewPageButtonVisible: LiveData<Boolean> = _isNewPageButtonVisible

    private val _pages = MutableLiveData<List<PageModel>>()
    val pages: LiveData<List<PageModel>> = _pages

    private val _searchPages: MutableLiveData<SortedMap<PageListType, List<PageModel>>?> = MutableLiveData()
    val searchPages: LiveData<SortedMap<PageListType, List<PageModel>>?> = _searchPages

    private val _createNewPage = SingleLiveEvent<Unit?>()
    val createNewPage: LiveData<Unit?> = _createNewPage

    private val _editPage = SingleLiveEvent<Triple<SiteModel, PostModel?, LoadAutoSaveRevision>>()
    val editPage: LiveData<Triple<SiteModel, PostModel?, LoadAutoSaveRevision>> = _editPage

    private val _previewPage = SingleLiveEvent<PostModel?>()
    val previewPage: LiveData<PostModel?> = _previewPage

    private val _browsePreview = SingleLiveEvent<BrowsePreview?>()
    val browsePreview: LiveData<BrowsePreview?> = _browsePreview

    private val _previewState = SingleLiveEvent<PostListRemotePreviewState>()
    val previewState = _previewState as LiveData<PostListRemotePreviewState>

    private val _setPageParent = SingleLiveEvent<PageModel?>()
    val setPageParent: LiveData<PageModel?> = _setPageParent

    private val _scrollToPage = SingleLiveEvent<PageModel>()
    val scrollToPage: LiveData<PageModel?> = _scrollToPage

    private val _invalidateUploadStatus = MutableLiveData<List<LocalId>>()
    val invalidateUploadStatus: LiveData<List<LocalId>> = _invalidateUploadStatus

    private val _postUploadAction = SingleLiveEvent<Triple<PostModel, SiteModel, Intent>>()
    val postUploadAction: LiveData<Triple<PostModel, SiteModel, Intent>> = _postUploadAction

    private val _uploadFinishedAction = SingleLiveEvent<Triple<PageModel, PageUploadErrorWrapper, Boolean>>()
    val uploadFinishedAction: LiveData<Triple<PageModel, PageUploadErrorWrapper, Boolean>> = _uploadFinishedAction

    private val _publishAction = SingleLiveEvent<PageModel>()
    val publishAction = _publishAction

    private val _launchPageListType = SingleLiveEvent<PageListType>()
    val launchPageListType = _launchPageListType

    private val _navigateToBlazeOverlay = SingleLiveEvent<PageModel>()
    val navigateToBlazeOverlay = _navigateToBlazeOverlay

    private val _openExternalLink = SingleLiveEvent<String>()
    val openExternalLink: LiveData<String> = _openExternalLink

    private val _openSiteEditorWebView = SingleLiveEvent<SiteEditorData>()
    val openSiteEditorWebView: LiveData<SiteEditorData> = _openSiteEditorWebView

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

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogAction: LiveData<DialogHolder> = _dialogAction

    private val _conflictResolutionAction = SingleLiveEvent<PostResolutionOverlayActionEvent.ShowDialogAction>()
    val conflictResolutionAction: LiveData<PostResolutionOverlayActionEvent.ShowDialogAction> =
        _conflictResolutionAction

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
    private var currentPageType = PUBLISHED

    private lateinit var pageListEventListener: PageListEventListener

    private val pageListDialogHelper: PageListDialogHelper by lazy {
        PageListDialogHelper(
            showDialog = { _dialogAction.postValue(it) },
            analyticsTracker = analyticsTracker,
            isPostConflictResolutionEnabled = postConflictResolutionFeatureUtils.isPostConflictResolutionEnabled(),
            showConflictResolutionOverlay = { _conflictResolutionAction.postValue(it) },
        )
    }

    private val pageConflictResolver: PageConflictResolver by lazy {
        PageConflictResolver(
            dispatcher = dispatcher,
            site = site,
            postStore = postStore,
            uploadStore = uploadStore,
            invalidateList = this::invalidateAllLists,
            checkNetworkConnection = this::checkNetworkConnection,
            showSnackBar = { _showSnackbarMessage.postValue(it) }
        )
    }

    private val _authorSelectionUpdated = MutableLiveData<AuthorFilterSelection>()
    val authorSelectionUpdated = _authorSelectionUpdated

    private val _authorUIState = MutableLiveData<PagesAuthorFilterUIState>()
    val authorUIState: LiveData<PagesAuthorFilterUIState> = _authorUIState

    data class BrowsePreview(
        val post: PostModel,
        val previewType: RemotePreviewType
    )

    data class SiteEditorData(
        val url: String,
        val useWpComCredentials: Boolean,
    )

    fun start(site: SiteModel) {
        // Check if VM is not already initialized
        if (_site == null) {
            _site = site

            loadPagesAsync()
            uploadStarter.queueUploadFromSite(site)
        }

        pageListEventListener = pageListEventListenerFactory.createAndStartListening(
            dispatcher = dispatcher,
            bgDispatcher = defaultDispatcher,
            postStore = postStore,
            eventBusWrapper = eventBusWrapper,
            siteStore = siteStore,
            site = site,
            invalidateUploadStatus = this::handleInvalidateUploadStatus,
            handleRemoteAutoSave = this::handleRemoveAutoSaveEvent,
            handlePostUploadFinished = this::postUploadedFinished,
            handleHomepageSettingsChange = this::handleHomepageSettingsChange,
            handlePageUpdatedWithoutError = pageConflictResolver::onPageSuccessfullyUpdated
        )

        val authorFilterSelection: AuthorFilterSelection = if (isFilteringByAuthorSupported) {
            prefs.postListAuthorSelection
        } else {
            AuthorFilterSelection.EVERYONE
        }

        _authorSelectionUpdated.value = authorFilterSelection
        _authorUIState.value = PagesAuthorFilterUIState(
            isAuthorFilterVisible = isFilteringByAuthorSupported,
            authorFilterSelection = authorFilterSelection,
            authorFilterItems = getAuthorFilterItems(authorFilterSelection, accountStore.account?.avatarUrl)
        )
    }

    override fun onCleared() {
        actionPerformer.onCleanup()
        if (::pageListEventListener.isInitialized) {
            pageListEventListener.onDestroy()
        }
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

    private suspend fun reloadPages(state: PageListState = REFRESHING, forced: Boolean = false) {
        if (performIfNetworkAvailableAsync {
                _listState.setOnUi(state)

                val result = pageStore.requestPagesFromServer(site, forced)
                if (result.isError) {
                    _listState.setOnUi(ERROR)
                    showSnackbar(SnackbarMessageHolder(UiStringRes(R.string.error_refresh_pages)))
                    AppLog.e(PAGES, "An error occurred while fetching the Pages")
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

    fun onPageEditFinished(localPageId: Int, data: Intent) {
        launch {
            updatePageInMap(localPageId) // show local changes immediately
            withContext(defaultDispatcher) {
                pageStore.getPageByLocalId(pageId = localPageId, site = site)?.let {
                    _scrollToPage.postOnUi(it)
                    _postUploadAction.postValue(Triple(it.post, it.site, data))
                }
            }
        }
    }

    fun onPageParentSet(pageId: Long, parentId: Long) {
        launch {
            pageMap[pageId]?.let { page ->
                setParent(page, parentId)
            }
        }
    }

    fun onSpecificPageListTypeRequested(pageListType: PageListType) {
        _launchPageListType.postValue(pageListType)
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
            TRASHED -> "binned"
        }
        val properties = mutableMapOf("tab_name" to tab as Any)
        AnalyticsUtils.trackWithSiteDetails(PAGES_TAB_PRESSED, site, properties)
    }

    fun checkIfNewPageButtonShouldBeVisible() {
        _isNewPageButtonVisible.postOnUi(_isSearchExpanded.value != true)
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

    @SuppressLint("NullSafeMutableLiveData")
    fun onSpecificPageRequested(remotePageId: Long) {
        if (isInitialized) {
            val page = pageMap[remotePageId]
            if (page != null) {
                _scrollToPage.postValue(page)
            } else {
                _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(R.string.pages_open_page_error)))
            }
        } else {
            scrollToPageId = remotePageId
        }
    }

    fun updatePreviewAndDialogState(newState: PostListRemotePreviewState, postInfo: PostInfoType) {
        // We need only transitions, so...
        if (_previewState.value == newState) return

        // update the state
        val prevState = _previewState.value
        _previewState.postValue(newState)

        // take care of exit actions on state transition
        previewStateHelper.managePreviewStateTransitions(
            newState,
            prevState,
            postInfo,
            this::handleRemotePreview
        )
    }

    private suspend fun groupedSearch(
        site: SiteModel,
        searchQuery: String
    ): SortedMap<PageListType, List<PageModel>> = withContext(defaultDispatcher) {
        val list = pageStore.search(site, searchQuery)
            .groupBy { PageListType.fromPageStatus(it.status) }

        return@withContext list.toSortedMap { previous, next ->
            when {
                previous == next -> 0
                previous == PUBLISHED -> -1
                next == PUBLISHED -> 1
                previous == DRAFTS -> -1
                next == DRAFTS -> 1
                previous == SCHEDULED -> -1
                next == SCHEDULED -> 1
                else -> throw IllegalArgumentException("Unexpected page type")
            }
        }
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

    fun onMenuAction(action: PagesListAction, page: Page, context: Context? = null): Boolean {
        when (action) {
            VIEW_PAGE -> previewPage(page)
            SET_PARENT -> setParent(page)
            MOVE_TO_DRAFT -> changePageStatus(page.remoteId, PageStatus.DRAFT)
            MOVE_TO_TRASH -> changePageStatus(page.remoteId, PageStatus.TRASHED)
            PUBLISH_NOW -> publishPageNow(page.remoteId)
            DELETE_PERMANENTLY -> deletePage(page)
            CANCEL_AUTO_UPLOAD -> cancelPendingAutoUpload(LocalId(page.localId))
            SET_AS_HOMEPAGE -> setHomepage(page.remoteId)
            SET_AS_POSTS_PAGE -> setPostsPage(page.remoteId)
            COPY -> onCopyPage(page)
            SHARE -> context?.let { copyPageLink(page, it) }
            PROMOTE_WITH_BLAZE -> navigateToBlazeOverlay(page.remoteId)
        }
        return true
    }

    private fun navigateToBlazeOverlay(remoteId: Long) {
        blazeFeatureUtils.trackEntryPointTapped(BlazeFlowSource.PAGES_LIST)
        val page = pageMap[remoteId]
        page?.let { _navigateToBlazeOverlay.postValue(it) }
    }

    private fun deletePage(page: Page) {
        performIfNetworkAvailable {
            pageListDialogHelper.showDeletePageConfirmationDialog(RemoteId(page.remoteId), page.title)
        }
    }

    private fun cancelPendingAutoUpload(pageId: LocalId) {
        trackMenuSelectionEvent(CANCEL_AUTO_UPLOAD)
        val page = postStore.getPostByLocalPostId(pageId.value)
        val msgRes = UploadUtils.cancelPendingAutoUpload(page, dispatcher)
        _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(msgRes)))
    }

    private fun setParent(page: Page) {
        performIfNetworkAvailable {
            trackMenuSelectionEvent(SET_PARENT)

            _setPageParent.postValue(pageMap[page.remoteId])
        }
    }

    private fun setHomepage(homepageId: Long) {
        performIfNetworkAvailable {
            trackMenuSelectionEvent(SET_AS_HOMEPAGE)

            if (site.showOnFront == PAGE.value) {
                launch {
                    val result = siteOptionsStore.updatePageOnFront(
                        site,
                        homepageId
                    )
                    val message = when (result.isError) {
                        true -> {
                            appLogWrapper.d(PAGES, "${result.error.type}: ${result.error.message}")
                            R.string.page_homepage_update_failed
                        }

                        false -> {
                            R.string.page_homepage_successfully_updated
                        }
                    }
                    _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(message)))
                }
            } else {
                _showSnackbarMessage.postValue(
                    SnackbarMessageHolder(
                        message = UiStringRes(R.string.page_cannot_set_homepage)
                    )
                )
            }
        }
    }

    private fun setPostsPage(remoteId: Long) {
        performIfNetworkAvailable {
            trackMenuSelectionEvent(SET_AS_POSTS_PAGE)

            if (site.showOnFront == PAGE.value) {
                launch {
                    val result = siteOptionsStore.updatePageForPosts(
                        site,
                        remoteId
                    )
                    val message = when (result.isError) {
                        true -> {
                            appLogWrapper.d(PAGES, "${result.error.type}: ${result.error.message}")
                            R.string.page_posts_page_update_failed
                        }

                        false -> {
                            R.string.page_posts_page_successfully_updated
                        }
                    }
                    _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(message)))
                }
            } else {
                _showSnackbarMessage.postValue(
                    SnackbarMessageHolder(
                        message = UiStringRes(R.string.page_cannot_set_posts_page)
                    )
                )
            }
        }
    }

    private fun copyPageLink(page: Page, context: Context) {
        // Get the link to the page
        trackMenuSelectionEvent(SHARE)
        val pageLink = postStore.getPostByLocalPostId(page.localId)?.link ?: return
        ActivityLauncher.openShareIntent(
            context,
            pageLink,
            page.title
        )
    }

    private fun previewPage(page: Page) {
        launch(defaultDispatcher) {
            trackMenuSelectionEvent(VIEW_PAGE)
            val pageModel = pageMap[page.remoteId]
            val post = if (pageModel != null) postStore.getPostByLocalPostId(pageModel.pageId) else null
            _previewPage.postValue(post)
        }
    }

    private fun onCopyPage(page: Page) {
        launch(defaultDispatcher) {
            trackMenuSelectionEvent(COPY)
            copyPage(page.remoteId, performChecks = true)
        }
    }

    private fun copyPage(pageId: Long, performChecks: Boolean = false) {
        pageMap[pageId]?.let {
            if (performChecks && pageConflictDetector.hasUnhandledAutoSave(it.post)) {
                pageListDialogHelper.showCopyConflictDialog(it.post)
                return
            }

            val post = postStore.instantiatePostModel(
                site,
                true,
                it.title,
                it.post.content,
                PostStatus.DRAFT.toString(),
                it.post.categoryIdList,
                it.post.postFormat,
                false
            )

            _editPage.postValue(Triple(site, post, false))
        }
    }

    private fun onCopyPageLocal(pageId: RemoteId) {
        copyPage(pageId.value, performChecks = false)
    }

    private fun onEditPageFirst(pageId: RemoteId) {
        pageMap[pageId.value]?.let { checkAndEdit(it) }
    }

    private fun performIfNetworkAvailable(performAction: () -> Unit): Boolean {
        return if (networkUtils.isNetworkAvailable()) {
            performAction()
            true
        } else {
            _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            false
        }
    }

    private fun checkNetworkConnection(): Boolean =
        if (networkUtils.isNetworkAvailable()) {
            true
        } else {
            _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            false
        }

    private suspend fun performIfNetworkAvailableAsync(performAction: suspend () -> Unit): Boolean {
        return if (networkUtils.isNetworkAvailable()) {
            performAction()
            true
        } else {
            _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            false
        }
    }

    private fun trackMenuSelectionEvent(action: PagesListAction) {
        val menu = when (action) {
            VIEW_PAGE -> "view"
            CANCEL_AUTO_UPLOAD -> "cancel_auto_upload"
            SET_PARENT -> "set_parent"
            SET_AS_HOMEPAGE -> "set_homepage"
            SET_AS_POSTS_PAGE -> "set_posts_page"
            COPY -> "copy"
            PUBLISH_NOW -> "publish_now"
            MOVE_TO_DRAFT -> "move_to_draft"
            DELETE_PERMANENTLY -> "delete_permanently"
            MOVE_TO_TRASH -> "move_to_bin"
            SHARE -> "share"
            PROMOTE_WITH_BLAZE -> "promote_with_blaze"
        }
        val properties = mutableMapOf("option_name" to menu as Any)
        AnalyticsUtils.trackWithSiteDetails(PAGES_OPTIONS_PRESSED, site, properties)
    }

    private fun publishPageNow(remoteId: Long) {
        trackMenuSelectionEvent(PUBLISH_NOW)
        _publishAction.value = pageMap[remoteId]
        launch(uiDispatcher) {
            delay(SCROLL_DELAY)
            pageMap[remoteId]?.let {
                _scrollToPage.postValue(it)
            }
        }
    }

    fun onImagesChanged() {
        launch {
            refreshPages()
        }
    }

    fun onItemTapped(pageItem: Page) {
        if (pageItem.remoteId == site.pageForPosts) {
            launch(defaultDispatcher) {
                _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(R.string.page_is_posts_page_warning)))
            }
        } else {
            pageMap[pageItem.remoteId]?.let { checkAndEdit(it) }
        }
    }

    private fun checkAndEdit(page: PageModel) {
        if (pageConflictDetector.hasUnhandledAutoSave(page.post)) {
            pageListDialogHelper.showAutoSaveRevisionDialog(page.post)
            return
        }

        if (postConflictResolutionFeatureUtils.isPostConflictResolutionEnabled() &&
            pageConflictDetector.hasUnhandledConflict(page.post)) {
            pageListDialogHelper.showConflictedPostResolutionDialog(page.post)
            return
        }

        editPage(RemoteId(page.remoteId))
    }

    fun onVirtualHomepageAction(action: VirtualHomepage.Action) {
        trackVirtualHomepageAction(action)
        when (action) {
            is VirtualHomepage.Action.OpenExternalLink -> {
                _openExternalLink.postValue(action.url)
            }

            VirtualHomepage.Action.OpenSiteEditor -> {
                _openSiteEditorWebView.postValue(
                    SiteEditorData(
                        VirtualHomepage.Action.OpenSiteEditor.getUrl(site),
                        useWpComCredentials = site.isWPCom || site.isWPComAtomic || site.isPrivateWPComAtomic
                    )
                )
            }
        }.exhaustive
    }

    private fun trackVirtualHomepageAction(action: VirtualHomepage.Action) {
        val stat = when (action) {
            VirtualHomepage.Action.OpenExternalLink.TemplateSupport ->
                AnalyticsTracker.Stat.PAGES_EDIT_HOMEPAGE_INFO_PRESSED

            VirtualHomepage.Action.OpenSiteEditor -> AnalyticsTracker.Stat.PAGES_EDIT_HOMEPAGE_ITEM_PRESSED
        }
        analyticsTracker.track(stat, site)
    }

    fun onNewPageButtonTapped() {
        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.PAGES_ADD_PAGE, site)

        _createNewPage.asyncCall()
    }

    fun onPullToRefresh() {
        uploadStarter.queueUploadFromSite(site)
        launch {
            reloadPages(FETCHING, forced = true)
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
                        SnackbarMessageHolder(
                            UiStringRes(R.string.page_parent_changed),
                            UiStringRes(R.string.undo),
                            action.undo!!
                        )
                    } else {
                        SnackbarMessageHolder(UiStringRes(R.string.page_parent_changed))
                    }
                )
            }
        }
        action.onError = {
            launch(defaultDispatcher) {
                refreshPages()

                showSnackbar(SnackbarMessageHolder(UiStringRes(R.string.page_parent_change_error)))
            }
        }

        launch {
            _arePageActionsEnabled = false
            actionPerformer.performAction(action)
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
        trackMenuSelectionEvent(DELETE_PERMANENTLY)
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

                showSnackbar(SnackbarMessageHolder(UiStringRes(R.string.page_permanently_deleted)))
            }
        }
        action.onError = {
            launch(defaultDispatcher) {
                refreshPages()

                showSnackbar(SnackbarMessageHolder(UiStringRes(R.string.page_delete_error)))
            }
        }

        launch {
            actionPerformer.performAction(action)
        }
    }

    private fun changePageStatus(remoteId: Long, status: PageStatus) {
        performIfNetworkAvailable {
            if (status == PageStatus.DRAFT) {
                trackMenuSelectionEvent(MOVE_TO_DRAFT)
            } else if (status == PageStatus.TRASHED) {
                trackMenuSelectionEvent(MOVE_TO_TRASH)
            }

            pageMap[remoteId]?.let { page ->
                val oldStatus = page.status

                val action = if (status != PageStatus.TRASHED || remoteId > 0) {
                    // this is executed when a page is moved to
                    PageAction(remoteId, UPLOAD) {
                        val updatedPage = updatePageStatus(page, status)
                        pageStore.updatePageInDb(updatedPage)
                        _scrollToPage.postOnUi(updatedPage)
                        pageStore.uploadPageToServer(updatedPage)
                    }
                } else {
                    // Local pages are trashed locally
                    PageAction(remoteId, UPDATE) {
                        val updatedPage = updatePageStatus(page, status)
                        pageStore.updatePageInDb(updatedPage)
                        // should we scroll to the trash page - probably
                        _scrollToPage.postOnUi(updatedPage)
                    }
                }

                action.undo = {
                    val updatedPage = updatePageStatus(page.copy(remoteId = action.remoteId), oldStatus)
                    launch(defaultDispatcher) {
                        pageStore.updatePageInDb(updatedPage)
                        updatePageMap(updatedPage)
                        pageStore.uploadPageToServer(updatedPage)
                    }
                }

                action.onSuccess = {
                    launch(defaultDispatcher) {
                        delay(ACTION_DELAY)

                        val message = prepareStatusChangeSnackbar(status, action.undo)
                        showSnackbar(message)
                    }
                }
                action.onError = {
                    launch(defaultDispatcher) {
                        action.undo?.let { it() }

                        showSnackbar(SnackbarMessageHolder(UiStringRes(R.string.page_status_change_error)))
                    }
                }

                launch {
                    _arePageActionsEnabled = false
                    actionPerformer.performAction(action)
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
            PageStatus.DRAFT -> R.string.page_moved_to_draft
            PageStatus.PUBLISHED -> R.string.page_moved_to_published
            PageStatus.TRASHED -> R.string.page_moved_to_trash
            PageStatus.SCHEDULED -> R.string.page_moved_to_scheduled
            else -> throw NotImplementedError("Status change to ${newStatus.getTitle()} not supported")
        }

        return if (undo != null) {
            SnackbarMessageHolder(UiStringRes(message), UiStringRes(R.string.undo), undo)
        } else {
            SnackbarMessageHolder(UiStringRes(message))
        }
    }

    private fun clearSearch() {
        _lastSearchQuery = ""
        _searchPages.postValue(null)
    }

    private fun handleRemotePreview(localPostId: Int, remotePreviewType: RemotePreviewType) {
        launch(defaultDispatcher) {
            val post = postStore.getPostByLocalPostId(localPostId)
            _browsePreview.postValue(BrowsePreview(post, remotePreviewType))
        }
    }

    private fun handleRemoteAutoSave(post: PostModel, isError: Boolean) {
        if (isError || hasRemoteAutoSavePreviewError()) {
            updatePreviewAndDialogState(PostListRemotePreviewState.NONE, PostInfoType.PostNoInfo)
            _showSnackbarMessage.postValue(SnackbarMessageHolder(UiStringRes(R.string.remote_preview_operation_error)))
        } else {
            updatePreviewAndDialogState(
                PostListRemotePreviewState.PREVIEWING,
                PostInfoType.PostInfo(
                    post = post,
                    hasError = isError
                )
            )
        }
    }

    fun updateAuthorFilterSelection(selectionId: Long) {
        val selection = AuthorFilterSelection.fromId(selectionId)

        updateViewStateTriggerPagerChange(
            authorFilterSelection = selection,
            authorFilterItems = getAuthorFilterItems(selection, accountStore.account?.avatarUrl)
        )
        if (isFilteringByAuthorSupported) {
            prefs.postListAuthorSelection = selection
        }
    }

    /**
     * Filtering by author is disable on:
     * 1) Self-hosted sites - The XMLRPC api doesn't support filtering by author.
     * 2) Jetpack sites - we need to pass in the self-hosted user id to be able to filter for authors
     * which we currently can't
     * 3) Sites on which the user doesn't have permissions to edit posts of other users.
     * 4) Single user sites - there is no point in filtering by author on single user sites.
     *
     * This behavior is consistent with Calypso and Posts as of 11/4/2019.
     */
    private val isFilteringByAuthorSupported: Boolean by lazy {
        site.isUsingWpComRestApi && site.hasCapabilityEditOthersPages
                && (site.isSingleUserSite != null && !site.isSingleUserSite)
    }

    @SuppressLint("NullSafeMutableLiveData")
    private fun updateViewStateTriggerPagerChange(
        isAuthorFilterVisible: Boolean? = null,
        authorFilterSelection: AuthorFilterSelection? = null,
        authorFilterItems: List<AuthorFilterListItemUIState>? = null
    ) {
        val currentState = requireNotNull(authorUIState.value) {
            "updateViewStateTriggerPagerChange should not be called before the initial state is set"
        }

        _authorUIState.value = _authorUIState.value?.copy(
            isAuthorFilterVisible = isAuthorFilterVisible ?: currentState.isAuthorFilterVisible,
            authorFilterSelection = authorFilterSelection ?: currentState.authorFilterSelection,
            authorFilterItems = authorFilterItems ?: currentState.authorFilterItems
        )

        if (authorFilterSelection != null && currentState.authorFilterSelection != authorFilterSelection) {
            _authorSelectionUpdated.value = authorFilterSelection

            AnalyticsUtils.trackWithSiteDetails(
                PAGES_LIST_AUTHOR_FILTER_CHANGED,
                site,
                mutableMapOf(TRACKS_SELECTED_AUTHOR_FILTER to authorFilterSelection.toString() as Any)
            )
        }
    }

    // BasicFragmentDialog Events

    fun onPositiveClickedForBasicDialog(instanceTag: String) {
        pageListDialogHelper.onPositiveClickedForBasicDialog(
            instanceTag = instanceTag,
            editPage = this::editPage,
            deletePage = this::onDeleteConfirmed,
            editPageFirst = this::onEditPageFirst
        )
    }

    fun onNegativeClickedForBasicDialog(instanceTag: String) {
        pageListDialogHelper.onNegativeClickedForBasicDialog(
            instanceTag = instanceTag,
            editPage = this::editPage,
            copyPage = this::onCopyPageLocal
        )
    }

    private fun onDeleteConfirmed(pageId: RemoteId) {
        launch(defaultDispatcher) {
            pageMap[pageId.value]?.let { deletePage(it) }
        }
    }

    // Post Resolution Overlay Actions
    fun onPostResolutionConfirmed(event: PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent) {
        pageListDialogHelper.onPostResolutionConfirmed(
            event = event,
            editPage = this::editPage,
            updateConflictedPostWithRemoteVersion = pageConflictResolver::updateConflictedPageWithRemoteVersion,
            updateConflictedPostWithLocalVersion = pageConflictResolver::updateConflictedPageWithLocalVersion
        )
    }

    private fun editPage(pageId: RemoteId, loadAutoSaveRevision: LoadAutoSaveRevision = false) {
        val page = pageMap.getValue(pageId.value)
        val result = if (page.post.isLocalDraft) {
            postStore.getPostByLocalPostId(page.pageId)
        } else {
            postStore.getPostByRemotePostId(page.remoteId, site)
        }
        _editPage.postValue(Triple(site, result, loadAutoSaveRevision))
    }

    private fun invalidateAllLists() {
        val listTypeIdentifier = PostListDescriptor.calculateTypeIdentifier(site.id)
        dispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
    }

    private fun isRemotePreviewingFromPostsList() = _previewState.value != null &&
            _previewState.value != PostListRemotePreviewState.NONE

    private fun hasRemoteAutoSavePreviewError() = _previewState.value != null &&
            _previewState.value == PostListRemotePreviewState.REMOTE_AUTO_SAVE_PREVIEW_ERROR

    private fun handleRemoveAutoSaveEvent(pageId: LocalId, isError: Boolean) {
        val post = postStore.getPostByLocalPostId(pageId.value)

        if (isRemotePreviewingFromPostsList()) {
            handleRemoteAutoSave(post, isError)
        }
    }

    private fun handleInvalidateUploadStatus(ids: List<LocalId>) {
        launch {
            _invalidateUploadStatus.value = ids
            ids.forEach { updatePageInMap(it.value)}
        }
    }

    private fun updatePageMap(page: PageModel) {
        val updatedMap = pageMap.toMutableMap()
        updatedMap[page.remoteId] = page
        pageMap = updatedMap
    }

    private fun updatePageInMap(localId: Int){
        launch {
            pageStore.getPageByLocalId(localId, site)?.let {
                updatePageMap(it)
            }
        }
    }

    private fun postUploadedFinished(
        remoteId: RemoteId,
        errorWrapper: PageUploadErrorWrapper,
        isFirstTimePublish: Boolean
    ) {
        pageMap[remoteId.value]?.let {
            _uploadFinishedAction.postValue(Triple(it, errorWrapper, isFirstTimePublish))
        }
    }

    private fun handleHomepageSettingsChange(siteModel: SiteModel) {
        launch {
            _site = siteModel
            refreshPages()
        }
    }

    private suspend fun <T> MutableLiveData<T>.setOnUi(value: T) = withContext(uiDispatcher) {
        setValue(value)
    }

    @SuppressLint("NullSafeMutableLiveData")
    private fun <T> MutableLiveData<T>.postOnUi(value: T) {
        val liveData = this
        launch {
            liveData.value = value
        }
    }

    fun shouldFilterByAuthor(): Boolean {
        return authorUIState.value?.authorFilterSelection == AuthorFilterSelection.ME
    }
}

@StringRes
fun PageStatus.getTitle(): Int {
    return when (this) {
        PageStatus.PUBLISHED -> R.string.pages_published
        PageStatus.DRAFT -> R.string.pages_drafts
        PageStatus.SCHEDULED -> R.string.pages_scheduled
        PageStatus.TRASHED -> R.string.pages_trashed
        PageStatus.PENDING -> R.string.pages_pending
        PageStatus.PRIVATE -> R.string.pages_private
    }
}
