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
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
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
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass", "LongParameterList")
internal class PagesRsListViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpServiceProvider,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
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
    private val resolveAuthorJobs = mutableMapOf<PageRsListTab, Job>()
    private var lastTrackedTab: PageRsListTab? = null

    private val _events = Channel<PageRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

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
     * it in the editor. Trashed pages are ignored for Phase 1 (Phase 4 will add the
     * "move to draft" confirmation flow).
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
                _events.trySend(PageRsListEvent.ShowToast(R.string.pages_list_item_trashed))
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
                if (model.authorId != 0L && model.authorId == existing?.authorId) {
                    model.copy(authorDisplayName = existing.authorDisplayName)
                } else {
                    model
                }
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
            )
            updateTabUiState(tab) {
                copy(pages = rows, isLoading = false, error = null, isAuthError = false)
            }
            resolveAuthorNames(tab, uiModels)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.PAGES, "Failed to load items for tab $tab", e)
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

    private fun PageRsListItem.withResolvedAuthor(names: Map<Long, String>): PageRsListItem {
        val name = names[page.authorId] ?: return this
        val updated = page.copy(authorDisplayName = name)
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
        resolveAuthorJobs.values.forEach { it.cancel() }
        resolveAuthorJobs.clear()
        _tabStates.value = emptyMap()
    }

    override fun onCleared() {
        super.onCleared()
        clearCollections()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 250L
        internal const val MIN_SEARCH_QUERY_LENGTH = 3
        private val ALL_STATUSES = PageRsListTab.entries.flatMap { it.statuses }.distinct()

        private const val TRACKS_SELECTED_TAB = "selected_tab"
        private const val TRACKS_SELECTED_AUTHOR_FILTER = "author_filter_selection"
        private const val TRACKS_ACTION = "action"
        private const val TRACKS_ACTION_EDIT = "edit"
        private const val TRACKS_PAGE_ID = "page_id"
    }
}
