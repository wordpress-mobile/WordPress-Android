package org.wordpress.android.ui.pagesrs

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus as FluxCPostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.PostRsErrorUtils
import org.wordpress.android.ui.postsrs.SnackbarMessage
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile.PostService
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass", "LongParameterList")
internal class PagesRsListViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpServiceProvider,
    private val dispatcher: Dispatcher,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    private val postStore: PostStore,
    private val homepageSettings: PageRsHomepageSettings,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val fluxCBridge: PageRsFluxCBridge,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    private val _tabStates = MutableStateFlow<Map<PageRsListTab, PageTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<PageRsListTab, PageTabUiState>> = _tabStates.asStateFlow()

    private val _isOpeningPage = MutableStateFlow(false)
    val isOpeningPage: StateFlow<Boolean> = _isOpeningPage.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var activeSearchTab = PageRsListTab.PUBLISHED

    private val collections = mutableMapOf<PageRsListTab, ObservableMetadataCollection>()
    private var collectionsScope = createCollectionsScope()
    private val initializingTabs = mutableSetOf<PageRsListTab>()
    private val userRefreshingTabs = mutableSetOf<PageRsListTab>()
    private val resolveImageJobs = mutableMapOf<PageRsListTab, Job>()
    private val resolveAuthorJobs = mutableMapOf<PageRsListTab, Job>()
    private var lastTrackedTab: PageRsListTab? = null

    private val _events = Channel<PageRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val _pendingConfirmation = MutableStateFlow<PageRsListConfirmation?>(null)
    val pendingConfirmation: StateFlow<PageRsListConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _parentPicker = MutableStateFlow<PageRsParentPickerState?>(null)
    val parentPicker: StateFlow<PageRsParentPickerState?> = _parentPicker.asStateFlow()

    val site: SiteModel? = selectedSiteRepository.getSelectedSite()

    val avatarUrl: String? = accountStore.account?.avatarUrl

    val isAuthorFilterSupported: Boolean = site != null &&
        site.isUsingWpComRestApi &&
        site.hasCapabilityEditOthersPages &&
        site.isSingleUserSite == false

    private val _authorFilter = MutableStateFlow(
        if (isAuthorFilterSupported) {
            appPrefsWrapper.pagesListAuthorSelection
        } else {
            AuthorFilterSelection.EVERYONE
        }
    )
    val authorFilter: StateFlow<AuthorFilterSelection> = _authorFilter.asStateFlow()

    init {
        dispatcher.register(this)
        if (site == null) {
            _events.trySend(PageRsListEvent.ShowToast(R.string.blog_not_found))
            _events.trySend(PageRsListEvent.Finish)
        } else {
            @OptIn(FlowPreview::class)
            viewModelScope.launch {
                _searchQuery
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .filter { it.length >= MIN_SEARCH_QUERY_LENGTH }
                    .collect {
                        clearCollections()
                        initTab(activeSearchTab)
                    }
            }
        }
    }

    @MainThread
    fun onTabChanged(tab: PageRsListTab) {
        val site = this.site ?: return
        if (tab == lastTrackedTab) return
        lastTrackedTab = tab
        analyticsTracker.track(
            Stat.PAGES_TAB_PRESSED,
            site,
            mapOf(TRACKS_SELECTED_TAB to tab.name.lowercase())
        )
    }

    /**
     * Clears all cached collections and tab states so the list
     * appears empty while the user types a search query.
     */
    @MainThread
    fun onSearchOpen() {
        val site = this.site ?: return
        analyticsTracker.track(Stat.PAGES_LIST_SEARCH_ACCESSED, site)
        _isSearchActive.value = true
        clearCollections()
    }

    /**
     * Updates the search query. Non-blank queries are debounced before triggering an API call.
     * Blank queries immediately clear results so the idle state appears without delay.
     */
    @MainThread
    fun onSearchQueryChanged(query: String, activeTab: PageRsListTab) {
        activeSearchTab = activeTab
        _searchQuery.value = query
        if (query.isBlank()) clearCollections()
    }

    /**
     * Closes search mode: clears the query, tears down all collections, and immediately
     * re-initializes [activeTab] so the normal tab content appears without debounce delay.
     */
    @MainThread
    fun onSearchClose(activeTab: PageRsListTab) {
        _isSearchActive.value = false
        _searchQuery.value = ""
        clearCollections()
        initTab(activeTab)
    }

    /**
     * Changes the author filter, persists the preference, then tears down
     * and rebuilds all collections so the new filter takes effect.
     */
    @MainThread
    fun onAuthorFilterChanged(selection: AuthorFilterSelection, activeTab: PageRsListTab) {
        val site = this.site ?: return
        if (selection == _authorFilter.value) return
        analyticsTracker.track(
            Stat.PAGES_LIST_AUTHOR_FILTER_CHANGED,
            site,
            mapOf(TRACKS_SELECTED_AUTHOR_FILTER to selection.toString())
        )
        appPrefsWrapper.pagesListAuthorSelection = selection
        _authorFilter.value = selection
        clearCollections()
        initTab(activeTab)
    }

    @MainThread
    fun initTab(tab: PageRsListTab) {
        val site = this.site ?: return
        if (collections.containsKey(tab) || initializingTabs.contains(tab)) return

        initializingTabs.add(tab)
        // Reset to a loading state so a retry after a failed init clears the prior error UI.
        updateTabUiState(tab) { PageTabUiState(isLoading = true) }

        launchCollectionJob {
            @Suppress("TooGenericExceptionCaught")
            try {
                val collection = createCollection(site, tab)
                collections[tab] = collection
                initializingTabs.remove(tab)
                registerObservers(tab, collection)
                loadItemsForTab(tab)
                refreshTab(tab)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to init RS page list tab", e)
                initializingTabs.remove(tab)
                updateTabUiState(tab) {
                    PageTabUiState(
                        error = friendlyErrorMessage(e),
                        isAuthError = PostRsErrorUtils.isAuthError(e)
                    )
                }
            }
        }
    }

    /**
     * Creates the observable collection for [tab]. If the calling job is cancelled while the
     * creation call is in flight, [withContext] discards its result and rethrows, so the
     * orphaned collection is closed here before it can leak.
     */
    private suspend fun createCollection(
        site: SiteModel,
        tab: PageRsListTab
    ): ObservableMetadataCollection {
        var created: ObservableMetadataCollection? = null
        try {
            return withContext(Dispatchers.IO) {
                val service = serviceProvider.getService(site)
                val query = _searchQuery.value
                val authorIds = if (_authorFilter.value == AuthorFilterSelection.ME) {
                    accountStore.account?.userId?.let { listOf(it) } ?: emptyList()
                } else {
                    emptyList()
                }
                val filter = PostListFilter(
                    status = if (query.isNotBlank()) ALL_STATUSES else tab.statuses,
                    order = tab.order,
                    orderby = tab.orderBy,
                    search = query.ifBlank { null },
                    author = authorIds
                )
                service.posts().getObservablePostMetadataCollectionWithEditContext(
                    endpointType = PostEndpointType.Pages,
                    filter = filter,
                    perPage = PAGE_SIZE.toUInt()
                ).also { created = it }
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable + Dispatchers.IO) { created?.close() }
            throw e
        }
    }

    private fun registerObservers(tab: PageRsListTab, collection: ObservableMetadataCollection) {
        collection.addDataObserver {
            launchCollectionJob { loadItemsForTab(tab) }
        }
        collection.addListInfoObserver {
            launchCollectionJob { updateListInfoForTab(tab) }
        }
    }

    /**
     * Launches collection-scoped work in [collectionsScope] so [clearCollections] can cancel
     * anything in flight before closing the underlying collections. Without this, a late
     * failure (e.g. a refresh resuming on an already-closed collection) could write stale
     * error state into the freshly rebuilt tabs.
     */
    private fun launchCollectionJob(block: suspend CoroutineScope.() -> Unit) {
        collectionsScope.launch(block = block)
    }

    /**
     * A child scope of [viewModelScope] (so it is torn down with the ViewModel) that can
     * also be cancelled independently when the collections it serves are closed.
     */
    private fun createCollectionsScope() = CoroutineScope(
        viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job)
    )

    /**
     * Fired by FluxC when UploadService finishes uploading a post/page — e.g. publishing a
     * duplicated page from the editor, which happens in the background after the editor
     * closes. The wordpress-rs collections don't see FluxC uploads, so refresh the tabs to
     * pick up the change.
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        val post = event.post ?: return
        if (!post.isPage || post.localSiteId != site?.id || event.isError) return
        refreshAllTabs()
    }

    /** Refreshes all currently initialized tabs. */
    @MainThread
    fun refreshAllTabs() {
        restClient.clearCaches()
        collections.keys.toList().forEach { tab ->
            refreshTab(tab)
        }
    }

    @MainThread
    fun refreshTab(tab: PageRsListTab, isUserRefresh: Boolean = false) {
        val collection = collections[tab] ?: run {
            // The collection wasn't created (init failed or hasn't run). Re-attempt init so
            // a Retry tap from the error UI can recover instead of silently doing nothing.
            initTab(tab)
            return
        }

        if (isUserRefresh) {
            restClient.clearCaches()
            userRefreshingTabs.add(tab)
            updateTabUiState(tab) { copy(isRefreshing = true, error = null) }
        } else {
            updateTabUiState(tab) { copy(error = null) }
        }

        launchCollectionJob {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.refresh() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to refresh tab $tab", e)
                userRefreshingTabs.remove(tab)
                val message = friendlyErrorMessage(e)
                val authError = PostRsErrorUtils.isAuthError(e)
                if (getTabUiState(tab).pages.isNotEmpty()) {
                    updateTabUiState(tab) {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            isAuthError = authError
                        )
                    }
                    _snackbarMessages.trySend(
                        SnackbarMessage(
                            message = message,
                            actionLabel = if (authError) null
                                else resourceProvider.getString(R.string.retry),
                            onAction = if (authError) null
                                else ({ refreshTab(tab) })
                        )
                    )
                } else {
                    updateTabUiState(tab) {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = message,
                            isAuthError = authError
                        )
                    }
                }
            }
        }
    }

    @MainThread
    fun loadMorePages(tab: PageRsListTab) {
        val collection = collections[tab] ?: return
        val current = getTabUiState(tab)
        if (current.isLoadingMore || current.isRefreshing || !current.canLoadMore) return

        updateTabUiState(tab) { copy(isLoadingMore = true) }

        launchCollectionJob {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.loadNextPage() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to load more for tab $tab", e)
                updateTabUiState(tab) { copy(isLoadingMore = false) }
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e))
                )
            }
        }
    }

    /**
     * Bridges the page into FluxC's database and emits [PageRsListEvent.EditPage] to open
     * it in the editor. Trashed pages can't be edited, so tapping one asks the user to
     * move it back to drafts first.
     */
    @MainThread
    fun openPage(remotePageId: Long, tab: PageRsListTab) {
        val site = this.site
        if (site == null || _isOpeningPage.value) return

        val page = _tabStates.value[tab]
            ?.pages
            ?.firstOrNull { it.remotePageId == remotePageId }
            ?.page

        when {
            tab == PageRsListTab.TRASHED || page?.isTrashed == true ->
                _pendingConfirmation.value = PageRsListConfirmation.MoveToDraft(remotePageId)
            checkNetwork() -> proceedOpenPage(site, remotePageId, page?.lastModified)
        }
    }

    private fun proceedOpenPage(site: SiteModel, remotePageId: Long, lastModified: String?) {
        analyticsTracker.track(
            Stat.PAGES_LIST_ITEM_SELECTED,
            site,
            mapOf(
                TRACKS_ACTION to TRACKS_ACTION_EDIT,
                TRACKS_PAGE_ID to remotePageId
            )
        )

        _isOpeningPage.value = true
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val page = withContext(Dispatchers.IO) {
                    fluxCBridge.fetchAndBridge(remotePageId, site, lastModified)
                }
                _events.trySend(PageRsListEvent.EditPage(site, page))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Bridge page failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e, R.string.page_not_found))
                )
            } finally {
                _isOpeningPage.value = false
            }
        }
    }

    @MainThread
    fun onAddNewPage() {
        val site = this.site ?: return
        analyticsTracker.track(Stat.PAGES_ADD_PAGE, site)
        _events.trySend(PageRsListEvent.CreateNewPage)
    }

    /** Routes an overflow-menu action tap to the appropriate event, dialog, or mutation. */
    @MainThread
    @Suppress("ReturnCount")
    fun onPageMenuAction(remotePageId: Long, action: PageRsMenuAction) {
        val site = this.site ?: return
        analyticsTracker.track(
            Stat.PAGES_OPTIONS_PRESSED,
            site,
            mapOf(TRACKS_OPTION_NAME to action.toAnalyticsAction())
        )
        val page = findPage(remotePageId)

        when (action) {
            PageRsMenuAction.VIEW -> {
                val url = page?.link?.takeIf { it.isNotBlank() } ?: return logMissingLink(remotePageId)
                _events.trySend(PageRsListEvent.ViewPage(url))
            }
            PageRsMenuAction.SHARE -> {
                val url = page?.link?.takeIf { it.isNotBlank() } ?: return logMissingLink(remotePageId)
                _events.trySend(PageRsListEvent.SharePage(url, page.title))
            }
            PageRsMenuAction.COPY_URL -> {
                val url = page?.link?.takeIf { it.isNotBlank() } ?: return logMissingLink(remotePageId)
                _events.trySend(PageRsListEvent.CopyPageUrl(url))
            }
            PageRsMenuAction.SET_PARENT -> openParentPicker(remotePageId)
            PageRsMenuAction.SET_AS_HOMEPAGE -> setAsHomepage(site, remotePageId)
            PageRsMenuAction.SET_AS_POSTS_PAGE -> setAsPostsPage(site, remotePageId)
            PageRsMenuAction.PUBLISH_NOW -> publishPage(remotePageId)
            PageRsMenuAction.MOVE_TO_DRAFT -> movePageToDraft(remotePageId)
            PageRsMenuAction.DUPLICATE -> duplicatePage(site, remotePageId)
            PageRsMenuAction.BLAZE -> bridgeAndPromote(site, remotePageId)
            PageRsMenuAction.TRASH ->
                _pendingConfirmation.value = PageRsListConfirmation.Trash(remotePageId)
            PageRsMenuAction.DELETE_PERMANENTLY ->
                _pendingConfirmation.value =
                    PageRsListConfirmation.Delete(remotePageId, page?.title.orEmpty())
        }
    }

    @MainThread
    fun onConfirmPendingAction() {
        when (val confirmation = _pendingConfirmation.value) {
            is PageRsListConfirmation.Trash -> trashPage(confirmation.pageId)
            is PageRsListConfirmation.Delete -> deletePage(confirmation.pageId)
            is PageRsListConfirmation.MoveToDraft -> moveToDraftAndEdit(confirmation.pageId)
            null -> Unit
        }
        _pendingConfirmation.value = null
    }

    @MainThread
    fun onDismissPendingAction() {
        _pendingConfirmation.value = null
    }

    /**
     * Opens the "Set Parent" bottom sheet. Candidates are the published pages currently
     * loaded in any tab, excluding the page itself and its descendants (re-parenting a page
     * under its own subtree would create a cycle), matching the legacy parent picker rules.
     *
     * Known limitation: only pages already loaded into the tabs are offered, so on large
     * sites pages beyond the loaded ones can't be chosen and the sheet's search field only
     * filters the loaded candidates. A follow-up could page through the full list instead.
     */
    @MainThread
    fun openParentPicker(remotePageId: Long) {
        val allPages = _tabStates.value.values
            .flatMap { state -> state.pages.map { it.page } }
            .distinctBy { it.remotePageId }
        val published = allPages
            .filter { it.status is PostStatus.Publish || it.status is PostStatus.Private }
        val page = findPage(remotePageId) ?: return
        // Descendants are collected across pages of every status: a published descendant
        // reached through a draft intermediate must still be excluded to prevent a cycle.
        val descendantIds = collectDescendantIds(remotePageId, allPages)
        val candidates = published
            .filter { it.remotePageId != remotePageId && it.remotePageId !in descendantIds }
            .map { PageRsParentCandidate(it.remotePageId, it.title) }
        _parentPicker.value = PageRsParentPickerState(
            pageId = remotePageId,
            currentParentId = page.parentId,
            candidates = candidates
        )
    }

    @MainThread
    fun onParentPickerDismissed() {
        _parentPicker.value = null
    }

    @MainThread
    fun onParentSelected(parentId: Long) {
        val picker = _parentPicker.value ?: return
        _parentPicker.value = null
        val site = this.site
        if (parentId == picker.currentParentId || site == null) return
        executePageMutation(
            successMessageResId = R.string.page_parent_changed,
            errorMessageResId = R.string.page_parent_change_error,
            logTag = "Set parent",
            onSuccess = {
                analyticsTracker.track(
                    Stat.PAGES_SET_PARENT_CHANGES_SAVED,
                    site,
                    mapOf(
                        TRACKS_PAGE_ID to picker.pageId,
                        TRACKS_NEW_PARENT_ID to parentId
                    )
                )
            }
        ) { service ->
            service.updatePost(
                PostEndpointType.Pages, picker.pageId,
                PostUpdateParams(parent = parentId, meta = null)
            )
        }
    }

    private fun collectDescendantIds(rootId: Long, pages: List<PageRsUiModel>): Set<Long> {
        val childrenByParent = pages.groupBy { it.parentId }
        val descendants = mutableSetOf<Long>()
        val queue = ArrayDeque(listOf(rootId))
        while (queue.isNotEmpty()) {
            val parentId = queue.removeFirst()
            childrenByParent[parentId]?.forEach { child ->
                if (descendants.add(child.remotePageId)) queue.addLast(child.remotePageId)
            }
        }
        return descendants
    }

    private fun trashPage(pageId: Long) = executePageMutation(
        successMessageResId = R.string.page_moved_to_trash,
        errorMessageResId = R.string.page_status_change_error,
        logTag = "Trash"
    ) { service ->
        service.trashPost(PostEndpointType.Pages, pageId)
    }

    private fun deletePage(pageId: Long) = executePageMutation(
        successMessageResId = R.string.page_permanently_deleted,
        errorMessageResId = R.string.page_delete_error,
        logTag = "Delete"
    ) { service ->
        service.deletePostPermanently(PostEndpointType.Pages, pageId)
    }

    private fun publishPage(pageId: Long) = executePageMutation(
        successMessageResId = R.string.page_published,
        errorMessageResId = R.string.page_status_change_error,
        logTag = "Publish"
    ) { service ->
        service.updatePost(
            PostEndpointType.Pages, pageId,
            pageStatusUpdate(PostStatus.Publish)
        )
    }

    private fun movePageToDraft(pageId: Long) = executePageMutation(
        successMessageResId = R.string.page_moved_to_draft,
        errorMessageResId = R.string.page_status_change_error,
        logTag = "Move to draft"
    ) { service ->
        service.updatePost(
            PostEndpointType.Pages, pageId,
            pageStatusUpdate(PostStatus.Draft)
        )
    }

    /**
     * Moves a trashed page back to drafts and opens it in the editor — the flow behind
     * tapping a trashed page, which can't be edited in place.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun moveToDraftAndEdit(pageId: Long) {
        val site = this.site ?: return
        if (!checkNetwork()) return
        analyticsTracker.track(
            Stat.PAGES_LIST_ITEM_SELECTED,
            site,
            mapOf(
                TRACKS_ACTION to "move_to_draft",
                TRACKS_PAGE_ID to pageId
            )
        )
        updateTabUiState(PageRsListTab.TRASHED) { copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    serviceProvider.getService(site).posts().updatePost(
                        PostEndpointType.Pages, pageId,
                        pageStatusUpdate(PostStatus.Draft)
                    )
                }
                val page = bridgePageOrNull(site, pageId)
                if (page != null) {
                    _events.trySend(PageRsListEvent.EditPage(site, page))
                } else {
                    _events.trySend(PageRsListEvent.ShowToast(R.string.page_moved_to_draft))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Move to draft failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e, R.string.page_status_change_error))
                )
            } finally {
                updateTabUiState(PageRsListTab.TRASHED) { copy(isRefreshing = false) }
            }
        }
    }

    private fun setAsHomepage(site: SiteModel, pageId: Long) {
        if (!checkNetwork()) return
        updateHomepageSettings(
            successMessageResId = R.string.page_homepage_successfully_updated,
            cannotSetMessageResId = R.string.page_cannot_set_homepage,
            errorMessageResId = R.string.page_homepage_update_failed
        ) {
            homepageSettings.setHomepage(site, pageId)
        }
    }

    private fun setAsPostsPage(site: SiteModel, pageId: Long) {
        if (!checkNetwork()) return
        updateHomepageSettings(
            successMessageResId = R.string.page_posts_page_successfully_updated,
            cannotSetMessageResId = R.string.page_cannot_set_posts_page,
            errorMessageResId = R.string.page_posts_page_update_failed
        ) {
            homepageSettings.setPostsPage(site, pageId)
        }
    }

    /**
     * Runs a homepage-settings update via [PageRsHomepageSettings], which syncs the shared
     * [SiteModel] on success. The published tab is then re-rendered so the virtual
     * Homepage / Posts Page rows reflect the new assignment.
     */
    private fun updateHomepageSettings(
        successMessageResId: Int,
        cannotSetMessageResId: Int,
        errorMessageResId: Int,
        operation: suspend () -> PageRsHomepageSettings.Result
    ) {
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { operation() }) {
                is PageRsHomepageSettings.Result.Success -> {
                    _snackbarMessages.trySend(
                        SnackbarMessage(resourceProvider.getString(successMessageResId))
                    )
                    launchCollectionJob { loadItemsForTab(PageRsListTab.PUBLISHED) }
                }
                is PageRsHomepageSettings.Result.StaticHomepageDisabled ->
                    _snackbarMessages.trySend(
                        SnackbarMessage(resourceProvider.getString(cannotSetMessageResId))
                    )
                is PageRsHomepageSettings.Result.Error -> {
                    AppLog.w(AppLog.T.PAGES, "Homepage settings update failed: ${result.message}")
                    _snackbarMessages.trySend(
                        SnackbarMessage(resourceProvider.getString(errorMessageResId))
                    )
                }
            }
        }
    }

    /**
     * Duplicates a page by bridging it into FluxC and opening the editor with a new local
     * draft carrying the same title and content.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun duplicatePage(site: SiteModel, remotePageId: Long) {
        if (!checkNetwork()) return
        _isOpeningPage.value = true
        viewModelScope.launch {
            try {
                val lastModified = findPage(remotePageId)?.lastModified
                val pageToCopy = withContext(Dispatchers.IO) {
                    fluxCBridge.fetchAndBridge(remotePageId, site, lastModified)
                }
                val newPage = postStore.instantiatePostModel(
                    site,
                    true,
                    pageToCopy.title,
                    pageToCopy.content,
                    FluxCPostStatus.DRAFT.toString(),
                    pageToCopy.categoryIdList,
                    pageToCopy.postFormat,
                    true
                )
                _events.trySend(PageRsListEvent.EditPage(site, newPage))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Duplicate page failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e, R.string.page_not_found))
                )
            } finally {
                _isOpeningPage.value = false
            }
        }
    }

    /** Bridges the page into FluxC and opens the Blaze promotion flow for it. */
    @Suppress("TooGenericExceptionCaught")
    private fun bridgeAndPromote(site: SiteModel, remotePageId: Long) {
        if (!checkNetwork()) return
        _isOpeningPage.value = true
        viewModelScope.launch {
            try {
                val page = bridgePageOrNull(site, remotePageId)
                if (page != null) {
                    _events.trySend(PageRsListEvent.PromoteWithBlaze(site, page))
                }
            } finally {
                _isOpeningPage.value = false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun bridgePageOrNull(site: SiteModel, remotePageId: Long) = try {
        val lastModified = findPage(remotePageId)?.lastModified
        withContext(Dispatchers.IO) {
            fluxCBridge.fetchAndBridge(remotePageId, site, lastModified)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.e(AppLog.T.PAGES, "Bridge page failed", e)
        _snackbarMessages.trySend(
            SnackbarMessage(friendlyErrorMessage(e, R.string.page_not_found))
        )
        null
    }

    /** Creates a [PostUpdateParams] for changing a page's status. */
    private fun pageStatusUpdate(status: PostStatus) = PostUpdateParams(status = status, meta = null)

    /**
     * Executes a page mutation (trash, delete, status change, set parent) with standard
     * error handling, handing [operation] the page service for the selected site. The
     * wordpress-rs cache notifies the observable collections after the call, so the
     * affected tabs re-render without a manual refresh.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun executePageMutation(
        successMessageResId: Int,
        errorMessageResId: Int,
        logTag: String,
        onSuccess: () -> Unit = {},
        operation: suspend (PostService) -> Unit
    ) {
        val site = this.site ?: return
        if (!checkNetwork()) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { operation(serviceProvider.getService(site).posts()) }
                onSuccess()
                _snackbarMessages.trySend(
                    SnackbarMessage(resourceProvider.getString(successMessageResId))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "$logTag failed", e)
                _snackbarMessages.trySend(
                    SnackbarMessage(friendlyErrorMessage(e, errorMessageResId))
                )
            }
        }
    }

    /** Searches all tab states for a [PageRsUiModel] matching [remotePageId]. */
    private fun findPage(remotePageId: Long): PageRsUiModel? {
        for (state in _tabStates.value.values) {
            for (item in state.pages) {
                if (item.remotePageId == remotePageId) return item.page
            }
        }
        return null
    }

    private fun logMissingLink(remotePageId: Long) {
        AppLog.w(AppLog.T.PAGES, "No link for page $remotePageId")
    }

    private fun checkNetwork(): Boolean {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _snackbarMessages.trySend(
                SnackbarMessage(resourceProvider.getString(R.string.no_network_message))
            )
            return false
        }
        return true
    }

    private fun friendlyErrorMessage(
        e: Exception,
        defaultResId: Int? = null,
    ): String = PostRsErrorUtils.friendlyErrorMessage(
        e, defaultResId, resourceProvider, networkUtilsWrapper
    )

    private suspend fun loadItemsForTab(tab: PageRsListTab) {
        val collection = collections[tab] ?: return

        @Suppress("TooGenericExceptionCaught")
        try {
            val isSearch = _searchQuery.value.isNotBlank()
            val items = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toPageUiModel(item.id, showStatus = isSearch)
                }
            }
            val existingById = getTabUiState(tab).pages
                .associate { it.remotePageId to it.page }
            val uiModels = items.map { model ->
                val existing = existingById[model.remotePageId]
                var resolved = model
                if (model.authorId != 0L && model.authorId == existing?.authorId) {
                    resolved = resolved.copy(authorDisplayName = existing.authorDisplayName)
                }
                if (model.featuredImageId != 0L && model.featuredImageId == existing?.featuredImageId) {
                    resolved = resolved.copy(featuredImageUrl = existing.featuredImageUrl)
                }
                resolved
            }
            val applyHierarchy = tab == PageRsListTab.PUBLISHED &&
                !isSearch &&
                _authorFilter.value != AuthorFilterSelection.ME
            // Re-read the site here: homepage settings can change while this screen is alive,
            // and the construction-time [site] snapshot would pin stale pageOnFront /
            // pageForPosts values onto the virtual rows.
            val currentSite = selectedSiteRepository.getSelectedSite() ?: site
            val rows = buildRows(
                pages = uiModels,
                applyHierarchy = applyHierarchy,
                pageOnFront = currentSite?.pageOnFront ?: 0L,
                pageForPosts = currentSite?.pageForPosts ?: 0L
            ).map { row -> row.withMenuActions(currentSite) }
            updateTabUiState(tab) {
                copy(pages = rows, isLoading = false, error = null, isAuthError = false)
            }
            resolveAuthorNames(tab, uiModels)
            resolveFeaturedImages(tab, uiModels)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.PAGES, "Failed to load items for tab $tab", e)
        }
    }

    /**
     * Fetches featured image URLs for pages that have a non-zero
     * [PageRsUiModel.featuredImageId] but no resolved URL yet.
     * All URLs are fetched in a single batched network call.
     */
    private fun resolveFeaturedImages(
        tab: PageRsListTab,
        pages: List<PageRsUiModel>
    ) {
        val site = this.site ?: return
        val unresolvedIds = pages
            .filter { it.featuredImageId != 0L && it.featuredImageUrl == null }
            .map { it.featuredImageId }
            .distinct()
        if (unresolvedIds.isEmpty()) return

        resolveImageJobs[tab]?.cancel()
        resolveImageJobs[tab] = viewModelScope.launch {
            val urls = withContext(Dispatchers.IO) {
                restClient.fetchMediaUrls(site, unresolvedIds, THUMBNAIL_SIZE_DP)
            }
            if (urls.isEmpty()) return@launch
            updateTabUiState(tab) {
                copy(pages = this.pages.map { item -> item.withResolvedFeaturedImage(urls) })
            }
        }
    }

    /**
     * Fetches display names for pages that have a non-zero
     * [PageRsUiModel.authorId] but no resolved name yet.
     * Skipped when filtering by "Me" since the user already
     * knows their own name.
     */
    private fun resolveAuthorNames(
        tab: PageRsListTab,
        pages: List<PageRsUiModel>
    ) {
        val site = this.site
        if (site == null || !isAuthorFilterSupported || _authorFilter.value == AuthorFilterSelection.ME) return

        val unresolvedIds = pages
            .filter { it.authorId != 0L && it.authorDisplayName == null }
            .map { it.authorId }
            .distinct()
        if (unresolvedIds.isEmpty()) return

        resolveAuthorJobs[tab]?.cancel()
        resolveAuthorJobs[tab] = viewModelScope.launch {
            val names = withContext(Dispatchers.IO) {
                restClient.fetchUserDisplayNames(site, unresolvedIds)
            }
            if (names.isEmpty()) return@launch
            updateTabUiState(tab) {
                copy(pages = this.pages.map { item -> item.withResolvedAuthor(names) })
            }
        }
    }

    private fun PageRsListItem.withMenuActions(site: SiteModel?): PageRsListItem {
        val pageOnFront = site?.pageOnFront ?: 0L
        val pageForPosts = site?.pageForPosts ?: 0L
        // WP.com capabilities and showOnFront are synced reliably, so the homepage actions
        // are hidden when they can't succeed, matching the legacy list. For self-hosted
        // application-password sites neither field is reliably populated, so the actions
        // are offered and the server enforces the rules (a 403 or the static-homepage
        // check surfaces as a snackbar).
        val canManageHomepage = site != null && if (site.isUsingWpComRestApi) {
            site.hasCapabilityManageOptions && site.showOnFront == ShowOnFront.PAGE.value
        } else {
            true
        }
        val actions = computePageMenuActions(
            status = page.status,
            isHomepage = pageOnFront != 0L && page.remotePageId == pageOnFront,
            isPostsPage = pageForPosts != 0L && page.remotePageId == pageForPosts,
            hasPassword = page.hasPassword,
            isBlazeEligibleSite = site != null && blazeFeatureUtils.isSiteBlazeEligible(site),
            canManageHomepage = canManageHomepage
        )
        if (actions == page.actions) return this
        val updated = page.copy(actions = actions)
        return when (this) {
            is PageRsListItem.Real -> copy(page = updated)
            is PageRsListItem.Virtual -> copy(page = updated)
        }
    }

    private fun PageRsListItem.withResolvedAuthor(names: Map<Long, String>): PageRsListItem {
        val name = names[page.authorId] ?: return this
        val updated = page.copy(authorDisplayName = name)
        return when (this) {
            is PageRsListItem.Real -> copy(page = updated)
            is PageRsListItem.Virtual -> copy(page = updated)
        }
    }

    private fun PageRsListItem.withResolvedFeaturedImage(urls: Map<Long, String>): PageRsListItem {
        val url = urls[page.featuredImageId] ?: return this
        val updated = page.copy(featuredImageUrl = url)
        return when (this) {
            is PageRsListItem.Real -> copy(page = updated)
            is PageRsListItem.Virtual -> copy(page = updated)
        }
    }

    private suspend fun updateListInfoForTab(tab: PageRsListTab) {
        val collection = collections[tab] ?: return

        // Guard the Rust-backed call: an unhandled failure here (e.g. a late observer firing
        // against a collection mid-teardown) would otherwise crash the app, since this runs in
        // a scope with no exception handler.
        @Suppress("TooGenericExceptionCaught")
        val listInfo = try {
            withContext(Dispatchers.IO) { collection.listInfo() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.PAGES, "Failed to read list info for tab $tab", e)
            return
        }
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage = listInfo?.state == ListState.FETCHING_FIRST_PAGE
        val isUserRefresh = userRefreshingTabs.contains(tab)

        if (!fetchingFirstPage) userRefreshingTabs.remove(tab)

        val isError = listInfo?.state == ListState.ERROR
        val hasPages = getTabUiState(tab).pages.isNotEmpty()
        val errorMessage = if (isError) {
            PostRsErrorUtils.friendlyErrorMessage(null, null, resourceProvider, networkUtilsWrapper)
        } else null

        if (isError && hasPages) {
            // Just sync state here; the snackbar is emitted by whichever action's catch
            // block (refreshTab / loadMorePages) caused the ERROR, since it can classify
            // the exception (e.g. auth) and choose the right action label.
            updateTabUiState(tab) {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    canLoadMore = morePages,
                    error = null
                )
            }
        } else {
            updateTabUiState(tab) {
                copy(
                    isLoading = isLoading && fetchingFirstPage,
                    isRefreshing = isUserRefresh && fetchingFirstPage,
                    isLoadingMore = listInfo?.state == ListState.FETCHING_NEXT_PAGE,
                    canLoadMore = morePages,
                    error = errorMessage
                )
            }
        }
    }

    private fun getTabUiState(tab: PageRsListTab): PageTabUiState {
        return _tabStates.value[tab] ?: PageTabUiState(isLoading = true)
    }

    private fun updateTabUiState(tab: PageRsListTab, update: PageTabUiState.() -> PageTabUiState) {
        val current = getTabUiState(tab)
        val next = current.update()
        if (next == current) return
        _tabStates.value = _tabStates.value + (tab to next)
    }

    private fun clearCollections() {
        // Cancel in-flight collection work first so nothing can write stale state
        // (or touch a closed collection) after the teardown below.
        collectionsScope.cancel()
        collectionsScope = createCollectionsScope()
        collections.values.forEach { it.close() }
        collections.clear()
        initializingTabs.clear()
        userRefreshingTabs.clear()
        resolveImageJobs.values.forEach { it.cancel() }
        resolveImageJobs.clear()
        resolveAuthorJobs.values.forEach { it.cancel() }
        resolveAuthorJobs.clear()
        _tabStates.value = emptyMap()
    }

    public override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
        clearCollections()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 250L
        internal const val MIN_SEARCH_QUERY_LENGTH = 3
        private const val THUMBNAIL_SIZE_DP = 64
        private val ALL_STATUSES = PageRsListTab.entries.flatMap { it.statuses }.distinct()

        private const val TRACKS_SELECTED_TAB = "selected_tab"
        private const val TRACKS_SELECTED_AUTHOR_FILTER = "author_filter_selection"
        private const val TRACKS_ACTION = "action"
        private const val TRACKS_ACTION_EDIT = "edit"
        private const val TRACKS_PAGE_ID = "page_id"
        private const val TRACKS_OPTION_NAME = "option_name"
        private const val TRACKS_NEW_PARENT_ID = "new_parent_id"
    }
}

/** Tracks values matching the legacy pages list (PagesViewModel.trackMenuSelectionEvent). */
private fun PageRsMenuAction.toAnalyticsAction(): String = when (this) {
    PageRsMenuAction.VIEW -> "view"
    PageRsMenuAction.SET_PARENT -> "set_parent"
    PageRsMenuAction.SET_AS_HOMEPAGE -> "set_homepage"
    PageRsMenuAction.SET_AS_POSTS_PAGE -> "set_posts_page"
    PageRsMenuAction.PUBLISH_NOW -> "publish_now"
    PageRsMenuAction.MOVE_TO_DRAFT -> "move_to_draft"
    PageRsMenuAction.DUPLICATE -> "copy"
    PageRsMenuAction.SHARE -> "share"
    PageRsMenuAction.COPY_URL -> "copy_url"
    PageRsMenuAction.BLAZE -> "promote_with_blaze"
    PageRsMenuAction.TRASH -> "move_to_bin"
    PageRsMenuAction.DELETE_PERMANENTLY -> "delete_permanently"
}
