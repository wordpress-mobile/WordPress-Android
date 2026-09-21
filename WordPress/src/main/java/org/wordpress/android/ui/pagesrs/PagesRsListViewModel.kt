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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EditorThemeActionBuilder
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus as FluxCPostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.fluxc.store.EditorThemeStore.FetchEditorThemePayload
import org.wordpress.android.fluxc.store.EditorThemeStore.OnEditorThemeChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.PostViewsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.PostRsErrorUtils
import org.wordpress.android.ui.postsrs.SnackbarMessage
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.rs.RsCollectionPrefetch
import org.wordpress.android.ui.rs.RsPostChangeListener
import org.wordpress.android.ui.rs.RsTabLoading
import org.wordpress.android.ui.rs.RsTabRefreshJobs
import org.wordpress.android.ui.rs.RsUploadedPost
import org.wordpress.android.ui.rs.toRsPostStatus
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteEditorMVPFeatureConfig
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_mobile.FetchException
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
    private val editorThemeStore: EditorThemeStore,
    private val siteEditorMVPFeatureConfig: SiteEditorMVPFeatureConfig,
    private val changeListener: RsPostChangeListener,
    private val statsDataSource: StatsDataSource,
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
    private val refreshJobs = RsTabRefreshJobs<PageRsListTab>()

    /** Tabs whose refresh is still paging the collection to the end; see [loadRemainingPages]. */
    private val fillingTabs = mutableSetOf<PageRsListTab>()

    private var isScreenVisible = false
    private var hasDeferredChange = false
    private var pendingReveal: PageRsReveal? = null

    /** Tabs whose collection has completed at least one fetch, so an empty list means empty. */
    private val fetchedTabs = mutableSetOf<PageRsListTab>()
    private val resolveImageJobs = mutableMapOf<PageRsListTab, Job>()
    private val resolveAuthorJobs = mutableMapOf<PageRsListTab, Job>()
    private var lastTrackedTab: PageRsListTab? = null

    /**
     * Outstanding view-count fetches, cancelled only at teardown.
     *
     * Deliberately not cancelled when the visible rows change: a cancelled fetch releases its
     * in-flight claim without filling the cache, and the next visible set would skip those ids as
     * "already in flight", stranding their rows on the loading skeleton with nothing left to
     * resolve them. Volume is bounded by [visiblePageIds] instead.
     */
    private val metricJobs = mutableSetOf<Job>()

    /**
     * The rows on screen right now, per tab, so work queued for rows scrolled past can be dropped.
     *
     * Keyed by tab rather than held as one set: the pager composes the neighbouring tab during a
     * drag, and its visible-row stream reports against that tab. A single field would be
     * overwritten by the neighbour, stranding the active tab's queued fetches - they re-check this
     * set once a permit frees and would find the wrong ids - and would later hand
     * [retryMetricsForVisibleRows] another tab's ids after a refresh.
     */
    private val visiblePageIds = mutableMapOf<PageRsListTab, Set<Long>>()

    /**
     * Caps concurrent view-count requests across the whole screen.
     *
     * Shared rather than created per call: [onRowsVisible] fires on every visible-set change, so a
     * per-call semaphore would cap each emission separately and a fling could still put a request
     * in flight for every row it passed.
     */
    private val viewCountGate = Semaphore(MAX_CONCURRENT_VIEW_FETCHES)

    /**
     * View counts keyed by remote page id, so scrolling back to a row does not refetch it and a
     * cache reload does not blank the number out. Only touched from the main dispatcher.
     *
     * A present key means "fetched"; a null value means the fetch came back with nothing usable.
     * Rows distinguish the two so a failure clears the loading skeleton rather than pinning it.
     */
    private val viewCountCache = mutableMapOf<Long, Long?>()
    private val inFlightViewCounts = mutableSetOf<Long>()

    /**
     * Featured media ids whose lookup came back without a URL. Rows use this to stop waiting: the
     * fetch is not retried on its own, so without it they shimmer indefinitely. Cleared by a
     * refresh, which is what gives a failed lookup another go.
     */
    private val unresolvableImageIds = mutableSetOf<Long>()

    private val _density = MutableStateFlow(
        ContentListDensity.of(appPrefsWrapper.isContentListCondensed)
    )
    val density: StateFlow<ContentListDensity> = _density.asStateFlow()

    private val _events = Channel<PageRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val _revealRequests = Channel<PageRsReveal>(Channel.BUFFERED)
    val revealRequests = _revealRequests.receiveAsFlow()

    private val _pendingConfirmation = MutableStateFlow<PageRsListConfirmation?>(null)
    val pendingConfirmation: StateFlow<PageRsListConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _parentPicker = MutableStateFlow<PageRsParentPickerState?>(null)
    val parentPicker: StateFlow<PageRsParentPickerState?> = _parentPicker.asStateFlow()

    // The parent picker has its own observable collection so it can page through (and search)
    // the full list of published pages independently of the four tab collections.
    private var parentPickerCollection: ObservableMetadataCollection? = null
    private var parentPickerJob: Job? = null
    private var parentPickerExcludedIds: Set<Long> = emptySet()
    private val _parentPickerQuery = MutableStateFlow("")

    val site: SiteModel? = selectedSiteRepository.getSelectedSite()

    val avatarUrl: String? = accountStore.account?.avatarUrl

    val isAuthorFilterSupported: Boolean = site != null &&
        site.isUsingWpComRestApi &&
        site.hasCapabilityEditOthersPages &&
        site.isSingleUserSite == false

    /**
     * View counts come from the WordPress.com stats endpoint, so they need a WordPress.com site ID
     * and the capability to read stats. Self-hosted sites reached over application passwords have
     * neither and show no view counts; the rest of the row is unaffected.
     */
    private val canFetchViewCounts: Boolean by lazy {
        site != null &&
            site.siteId > 0 &&
            SiteUtils.isAccessedViaWPComRest(site) &&
            site.hasCapabilityViewStats
    }

    private val _authorFilter = MutableStateFlow(
        if (isAuthorFilterSupported) {
            appPrefsWrapper.pagesListAuthorSelection
        } else {
            AuthorFilterSelection.EVERYONE
        }
    )
    val authorFilter: StateFlow<AuthorFilterSelection> = _authorFilter.asStateFlow()

    // Whether the site's homepage uses a block-based theme. Seeded from the local cache and kept
    // current via [onEditorThemeChanged]. When true (and the Site Editor MVP flag is on) the
    // published tab shows a single SITE_EDITOR virtual row that opens the Site Editor web view.
    private var isBlockBasedTheme = false

    // Guards against a rapid double-tap on the SITE_EDITOR row launching two web views.
    private var isLaunchingSiteEditor = false

    init {
        dispatcher.register(this)
        if (site == null) {
            _events.trySend(PageRsListEvent.ShowToast(R.string.blog_not_found))
            _events.trySend(PageRsListEvent.Finish)
        } else {
            // Only the SITE_EDITOR virtual row needs the block-theme state, so skip the fetch
            // entirely when the Site Editor MVP flag is off to avoid a request on every visit.
            if (siteEditorMVPFeatureConfig.isEnabled()) {
                refreshEditorTheme(site)
            }
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
            @OptIn(FlowPreview::class)
            viewModelScope.launch {
                _parentPickerQuery
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .distinctUntilChanged()
                    .collect { query -> onParentPickerQueryDebounced(query) }
            }
            // Subscribe before starting the listener - its flow has no replay, so a change
            // reported in between would be dropped.
            viewModelScope.launch {
                changeListener.changes.collect { onRemoteChangeDetected() }
            }
            viewModelScope.launch {
                changeListener.uploads.collect { onPageUploaded(it) }
            }
            changeListener.start(site, isPages = true)
        }
    }

    /** Called when the screen becomes visible, and again whenever it returns from the background. */
    @MainThread
    fun onScreenVisible() {
        isScreenVisible = true
        if (hasDeferredChange) onRemoteChangeDetected()
        emitPendingReveal()
    }

    @MainThread
    fun onScreenHidden() {
        isScreenVisible = false
    }

    /**
     * Refreshes the list after FluxC reported a change the rs collections can't see - a page saved
     * in the editor, or a duplicated page that publishes after the editor closes - or remembers to.
     *
     * Most of these arrive while the editor covers the list, and refreshing a screen nobody is
     * looking at spends a request per open tab on a result that may be superseded before it is
     * seen. A refresh while offline could only fail, and [refreshAllTabs] would then mark every
     * tab with an error the user never asked for. Either way the change is remembered, however
     * many arrive, and the list catches up with a single refresh in [onScreenVisible].
     */
    private fun onRemoteChangeDetected() {
        hasDeferredChange = true
        if (!isScreenVisible || !networkUtilsWrapper.isNetworkAvailable()) return
        hasDeferredChange = false
        refreshAllTabs()
    }

    /**
     * Remembers to point the user at a page the editor just saved: it lands on whichever tab its
     * status belongs to, not necessarily the one being looked at. Held until [onScreenVisible],
     * because the upload usually finishes while the editor still covers the list.
     */
    private fun onPageUploaded(upload: RsUploadedPost) {
        val status = upload.status.toRsPostStatus() ?: return
        val tab = PageRsListTab.entries.firstOrNull { status in it.statuses } ?: return
        pendingReveal = PageRsReveal(tab, upload.remotePostId)
        emitPendingReveal()
    }

    private fun emitPendingReveal() {
        if (!isScreenVisible) return
        val reveal = pendingReveal ?: return
        pendingReveal = null
        _revealRequests.trySend(reveal)
    }

    /**
     * Rebuilds the parent picker's collection for a (debounced) search query. Queries shorter
     * than [MIN_SEARCH_QUERY_LENGTH] are treated as blank so the full list is shown. No-ops if
     * the picker has been dismissed while the debounce was pending.
     */
    private fun onParentPickerQueryDebounced(query: String) {
        val site = this.site ?: return
        if (_parentPicker.value == null) return
        val effective = if (query.length >= MIN_SEARCH_QUERY_LENGTH) query else ""
        closeParentPickerCollection()
        updateParentPicker { copy(candidates = emptyList(), isLoading = true, error = null) }
        initParentPickerCollection(site, effective)
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
    private fun launchCollectionJob(block: suspend CoroutineScope.() -> Unit): Job =
        collectionsScope.launch(block = block)

    /**
     * A child scope of [viewModelScope] (so it is torn down with the ViewModel) that can
     * also be cancelled independently when the collections it serves are closed.
     */
    private fun createCollectionsScope() = CoroutineScope(
        viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job)
    )

    /** Seeds [isBlockBasedTheme] from the local cache and dispatches a remote refresh. */
    private fun refreshEditorTheme(site: SiteModel) {
        isBlockBasedTheme = editorThemeStore.getIsBlockBasedTheme(site)
        dispatcher.dispatch(
            EditorThemeActionBuilder.newFetchEditorThemeAction(
                FetchEditorThemePayload(site, gssEnabled = true)
            )
        )
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onEditorThemeChanged(event: OnEditorThemeChanged) {
        val site = this.site ?: return
        val isBlockBased = event.editorTheme?.themeSupport?.isEditorThemeBlockBased()
        if (site.id != event.siteId || isBlockBased == null || isBlockBased == isBlockBasedTheme) {
            return
        }
        isBlockBasedTheme = isBlockBased
        // Rebuild the published tab from cache so the SITE_EDITOR row appears/disappears.
        if (collections.containsKey(PageRsListTab.PUBLISHED)) {
            viewModelScope.launch { loadItemsForTab(PageRsListTab.PUBLISHED) }
        }
    }

    /** Refreshes all currently initialized tabs. */
    @MainThread
    fun refreshAllTabs() {
        restClient.clearCaches()
        val tabs = collections.keys.toList()
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            // Every tab would report the same connection failure, so record the failure on each
            // and send one message for the whole fan-out. It has to be sent here rather than by
            // a nominated tab: a tab only offers a snackbar when it has content to keep, so
            // picking one that turned out to be empty would swallow the message entirely.
            val anyTabKeepsItsPages = tabs.any { getTabUiState(it).pages.hasRealPages }
            tabs.forEach { onRefreshFailed(it, e = null, showSnackbar = false) }
            if (anyTabKeepsItsPages) {
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = friendlyErrorMessage(null),
                        actionLabel = resourceProvider.getString(R.string.retry),
                        onAction = { refreshAllTabs() }
                    )
                )
            }
            return
        }
        tabs.forEach { tab ->
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
            updateTabUiState(tab) {
                copy(
                    isLoading = RsTabLoading.onRefreshStarted(
                        hasItems = pages.hasRealPages,
                        hasFetched = tab in fetchedTabs
                    ),
                    error = null
                )
            }
        }

        when {
            // A refresh with no connection can only fail, so report it without the round trip.
            !networkUtilsWrapper.isNetworkAvailable() ->
                onRefreshFailed(tab, e = null, showSnackbar = isUserRefresh)

            // The tab is already refreshing. Its progress state is set above either way, and
            // the request is replayed by [startRefresh] once the running one finishes.
            refreshJobs.deferIfRunning(tab) -> Unit

            else -> startRefresh(tab, collection, isUserRefresh)
        }
    }

    /** Runs the one refresh a tab is allowed at a time, then replays any request it deferred. */
    private fun startRefresh(
        tab: PageRsListTab,
        collection: ObservableMetadataCollection,
        isUserRefresh: Boolean
    ) {
        val job = launchCollectionJob {
            val fill = needsCompleteSet(tab)
            if (fill) fillingTabs.add(tab)
            @Suppress("TooGenericExceptionCaught")
            try {
                val sync = withContext(Dispatchers.IO) { collection.refresh() }
                if (fill) loadRemainingPages(tab, collection, sync.hasMorePages)
                fetchedTabs.add(tab)
                // Drop only the "nothing to show" entries so a transient failure is retried,
                // while numbers already fetched stay put.
                viewCountCache.entries.removeAll { it.value == null }
                unresolvableImageIds.clear()
                userRefreshingTabs.remove(tab)
                fillingTabs.remove(tab)
                // Read the fetched items and end both progress states here rather than relying
                // on the collection observers, which aren't guaranteed to fire for a refresh.
                loadItemsForTab(tab)
                // The observers were held off during a fill, so its paging state is read now.
                updateListInfoForTab(tab)
                updateTabUiState(tab) { copy(isLoading = false, isRefreshing = false) }
                // Clearing the cache above puts those rows back into the pending state, and the
                // list only asks for view counts when its visible rows change - which a refresh in
                // place does not do. Without this the skeletons would spin with nothing to
                // resolve them.
                retryMetricsForVisibleRows(tab)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onRefreshFailed(tab, e, showSnackbar = isUserRefresh)
            } finally {
                fillingTabs.remove(tab)
            }
            if (refreshJobs.onFinished(tab)) refreshTab(tab)
        }
        refreshJobs.onStarted(tab, job)
    }

    /**
     * Whether [tab] is rendered as a tree, and so needs every page loaded before it can be right:
     * [flattenToTree] can only nest a child under a parent that has arrived, and a list sorted by
     * title delivers the two in unrelated pages.
     */
    private fun needsCompleteSet(tab: PageRsListTab) =
        tab == PageRsListTab.PUBLISHED &&
            _searchQuery.value.isBlank() &&
            _authorFilter.value != AuthorFilterSelection.ME

    /**
     * Pages [collection] through to the end as part of the refresh that fetched its first page.
     *
     * The tab is in [fillingTabs] throughout, which holds the observers off: pushing each page as
     * it landed would render the children as roots and then re-nest them a moment later. What
     * was on screen before the refresh stays put - the placeholders on a cold start, otherwise
     * the previously loaded tree - and the complete set replaces it in one go, as it did in the
     * legacy list. A fill that stops short is logged, not reported: the user asked for a refresh,
     * not for this, and whatever did load is shown with the scroll-driven load-more as fallback.
     */
    private suspend fun loadRemainingPages(
        tab: PageRsListTab,
        collection: ObservableMetadataCollection,
        hasMorePages: Boolean?
    ) {
        val outcome = RsCollectionPrefetch.loadRemainingPages(
            hasMorePages = hasMorePages,
            maxPages = MAX_FILL_PAGES,
            shouldRetry = { !PostRsErrorUtils.isAuthError(it) }
        ) {
            withContext(Dispatchers.IO) { collection.loadNextPage() }.hasMorePages
        }
        AppLog.d(AppLog.T.PAGES, "Fill of tab $tab ended: $outcome")
    }

    /**
     * A tab that already has pages on screen keeps them and offers a retry snackbar; an empty
     * one shows the full-screen error state instead.
     *
     * [e] is null when no request was made because the device is offline. The message is the same
     * either way - [friendlyErrorMessage] reports the network error whenever the device is offline,
     * regardless of what failed.
     *
     * [showSnackbar] is false when the user didn't ask for this refresh - [initTab] refreshes each
     * tab as the pager settles on it, and interrupting someone browsing cached pages with an error
     * they didn't provoke is noise. The full-screen error still covers a tab with nothing to show.
     * It's also false when another tab is already reporting the same failure.
     */
    private fun onRefreshFailed(tab: PageRsListTab, e: Exception?, showSnackbar: Boolean = true) {
        e?.let { AppLog.e(AppLog.T.PAGES, "Failed to refresh tab $tab", it) }
        userRefreshingTabs.remove(tab)
        val message = friendlyErrorMessage(e)
        val authError = PostRsErrorUtils.isAuthError(e)
        if (getTabUiState(tab).pages.hasRealPages) {
            updateTabUiState(tab) {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    isAuthError = authError
                )
            }
            if (showSnackbar) {
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = message,
                        actionLabel = if (authError) null
                            else resourceProvider.getString(R.string.retry),
                        // Tapping retry is the user asking, so the result has to be reported -
                        // a silent second failure looks like the button did nothing.
                        onAction = if (authError) null
                            else ({ refreshTab(tab, isUserRefresh = true) })
                    )
                )
            }
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

    @MainThread
    fun loadMorePages(tab: PageRsListTab) {
        val collection = collections[tab] ?: return
        // A refresh is already paging this tab to the end, and the observers that would say so
        // through isLoadingMore are held off while it does.
        if (tab in fillingTabs) return
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

        if (remotePageId == SITE_EDITOR_PAGE_ID) {
            openSiteEditor(site)
            return
        }

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

    /** Opens the block-theme homepage in the Site Editor web view, matching the legacy pages list. */
    private fun openSiteEditor(site: SiteModel) {
        if (isLaunchingSiteEditor) return
        isLaunchingSiteEditor = true
        analyticsTracker.track(Stat.PAGES_EDIT_HOMEPAGE_ITEM_PRESSED, site)
        val useWpComCredentials = site.isWPCom || site.isWPComAtomic || site.isPrivateWPComAtomic
        _events.trySend(
            PageRsListEvent.OpenSiteEditor(
                url = PageItem.VirtualHomepage.Action.OpenSiteEditor.getUrl(site),
                useWpComCredentials = useWpComCredentials
            )
        )
        // The web view opens in a separate activity with no completion callback, so clear the
        // guard after a short debounce: a rapid double-tap is dropped, but the row stays tappable
        // when the user returns.
        viewModelScope.launch {
            delay(SITE_EDITOR_LAUNCH_DEBOUNCE_MS)
            isLaunchingSiteEditor = false
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
     * Opens the "Set Parent" bottom sheet, backed by its own observable collection so the user
     * can page through (and server-search) every eligible published page rather than only the
     * pages already loaded into the tabs.
     *
     * The page itself and its descendants known from loaded data are excluded so re-parenting
     * can't form a cycle. Descendants on not-yet-loaded pages can't be excluded up front; if
     * such a page were chosen, WordPress core's loop check resets the parent to top level rather
     * than creating a cycle, and the subsequent refresh re-renders the real parent.
     */
    @MainThread
    fun openParentPicker(remotePageId: Long) {
        val site = this.site ?: return
        val page = findPage(remotePageId) ?: return
        // Descendants are collected across pages of every status: a published descendant
        // reached through a draft intermediate must still be excluded to prevent a cycle.
        val allPages = _tabStates.value.values
            .flatMap { state -> state.pages.map { it.page } }
            .distinctBy { it.remotePageId }
        parentPickerExcludedIds = collectDescendantIds(remotePageId, allPages) + remotePageId
        _parentPickerQuery.value = ""
        _parentPicker.value = PageRsParentPickerState(
            pageId = remotePageId,
            currentParentId = page.parentId,
            candidates = emptyList(),
            isLoading = true
        )
        initParentPickerCollection(site, query = "")
    }

    private fun initParentPickerCollection(site: SiteModel, query: String) {
        // Cancel any in-flight init so a stale, out-of-order query can't overwrite the
        // collection assigned for the latest query (see createParentPickerCollection for the
        // cancellation-safe cleanup that closes the half-built collection).
        parentPickerJob?.cancel()
        parentPickerJob = launchCollectionJob {
            @Suppress("TooGenericExceptionCaught")
            try {
                val collection = createParentPickerCollection(site, query)
                parentPickerCollection = collection
                registerParentPickerObservers(collection)
                loadParentPickerItems()
                withContext(Dispatchers.IO) { collection.refresh() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to init parent picker", e)
                updateParentPicker { copy(isLoading = false, error = friendlyErrorMessage(e)) }
            }
        }
    }

    /**
     * Creates the parent picker's observable collection (published + private pages, ordered by
     * title). Mirrors [createCollection]'s cancellation-safe cleanup so a collection created
     * after the job was cancelled is closed instead of leaking.
     */
    private suspend fun createParentPickerCollection(
        site: SiteModel,
        query: String
    ): ObservableMetadataCollection {
        var created: ObservableMetadataCollection? = null
        try {
            return withContext(Dispatchers.IO) {
                val service = serviceProvider.getService(site)
                val filter = PostListFilter(
                    status = PageRsListTab.PUBLISHED.statuses,
                    order = PageRsListTab.PUBLISHED.order,
                    orderby = PageRsListTab.PUBLISHED.orderBy,
                    search = query.ifBlank { null },
                    author = emptyList()
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

    private fun registerParentPickerObservers(collection: ObservableMetadataCollection) {
        collection.addDataObserver {
            launchCollectionJob { loadParentPickerItems() }
        }
        collection.addListInfoObserver {
            launchCollectionJob { updateParentPickerListInfo() }
        }
    }

    private suspend fun loadParentPickerItems() {
        val collection = parentPickerCollection ?: return
        @Suppress("TooGenericExceptionCaught")
        try {
            val nowLabel = resourceProvider.getString(R.string.rs_date_now)
            val (items, listInfo) = withContext(Dispatchers.IO) {
                collection.loadItems().map { it.state.toPageUiModel(it.id, nowLabel) } to collection.listInfo()
            }
            val candidates = items
                .filter { it.remotePageId !in parentPickerExcludedIds }
                .filter { it.status is PostStatus.Publish || it.status is PostStatus.Private }
                .map { PageRsParentCandidate(it.remotePageId, it.title) }
            // Don't publish an empty result while a load is still in progress: loadItems() emits
            // transient empty/partial sets during a refresh (and once before it starts), and
            // flipping to the "no results" / spinner state on each of those makes the list blink.
            // Wait until results arrive, or the fetch finishes and the list is genuinely empty
            // (isLoading is cleared by updateParentPickerListInfo when fetching ends).
            val loadInProgress = listInfo?.state == ListState.FETCHING_FIRST_PAGE ||
                _parentPicker.value?.isLoading == true
            if (candidates.isEmpty() && loadInProgress) return
            updateParentPicker { copy(candidates = candidates, isLoading = false, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.PAGES, "Failed to load parent picker items", e)
        }
    }

    private suspend fun updateParentPickerListInfo() {
        val collection = parentPickerCollection ?: return
        @Suppress("TooGenericExceptionCaught")
        val listInfo = try {
            withContext(Dispatchers.IO) { collection.listInfo() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.PAGES, "Failed to read parent picker list info", e)
            return
        }
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage = listInfo?.state == ListState.FETCHING_FIRST_PAGE
        val isError = listInfo?.state == ListState.ERROR
        val hasData = _parentPicker.value?.candidates?.isNotEmpty() == true
        updateParentPicker {
            copy(
                isLoading = isLoading && fetchingFirstPage,
                isLoadingMore = listInfo?.state == ListState.FETCHING_NEXT_PAGE,
                canLoadMore = morePages,
                error = if (isError && !hasData) {
                    PostRsErrorUtils.friendlyErrorMessage(
                        null, null, resourceProvider, networkUtilsWrapper
                    )
                } else null
            )
        }
    }

    @MainThread
    fun onParentSearchChanged(query: String) {
        if (_parentPicker.value == null) return
        _parentPickerQuery.value = query
        updateParentPicker { copy(query = query) }
    }

    @MainThread
    fun onLoadMoreParents() {
        val collection = parentPickerCollection ?: return
        val current = _parentPicker.value
        if (current == null || current.isLoadingMore || !current.canLoadMore) return

        updateParentPicker { copy(isLoadingMore = true) }
        launchCollectionJob {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.loadNextPage() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FetchException.StaleLoadMore) {
                // A concurrent refresh superseded this page request. The list info observer
                // reconciles paging state, so just clear the spinner — this isn't a user error.
                AppLog.d(AppLog.T.PAGES, "Ignoring stale parent picker load-more: ${e.message}")
                updateParentPicker { copy(isLoadingMore = false) }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to load more parents", e)
                updateParentPicker { copy(isLoadingMore = false) }
                _snackbarMessages.trySend(SnackbarMessage(friendlyErrorMessage(e)))
            }
        }
    }

    @MainThread
    fun onParentPickerDismissed() {
        _parentPicker.value = null
        _parentPickerQuery.value = ""
        closeParentPickerCollection()
    }

    private fun closeParentPickerCollection() {
        parentPickerJob?.cancel()
        parentPickerJob = null
        parentPickerCollection?.close()
        parentPickerCollection = null
    }

    private inline fun updateParentPicker(
        update: PageRsParentPickerState.() -> PageRsParentPickerState
    ) {
        _parentPicker.value = _parentPicker.value?.update()
    }

    @MainThread
    fun onParentSelected(parentId: Long) {
        val picker = _parentPicker.value ?: return
        _parentPicker.value = null
        _parentPickerQuery.value = ""
        closeParentPickerCollection()
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
        e: Exception?,
        defaultResId: Int? = null,
    ): String = PostRsErrorUtils.friendlyErrorMessage(
        e, defaultResId, resourceProvider, networkUtilsWrapper
    )

    private suspend fun loadItemsForTab(tab: PageRsListTab) {
        val collection = collections[tab] ?: return
        // Mid-fill the collection holds a partial tree; the refresh reads it once it is complete.
        if (tab in fillingTabs) return

        @Suppress("TooGenericExceptionCaught")
        try {
            val isSearch = _searchQuery.value.isNotBlank()
            val nowLabel = resourceProvider.getString(R.string.rs_date_now)
            val items = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toPageUiModel(item.id, nowLabel, showStatus = isSearch)
                }
            }
            val uiModels = mergeCachedFields(tab, items, isSearch)
            val applyHierarchy = needsCompleteSet(tab)
            // Re-read the site here: homepage settings can change while this screen is alive,
            // and the construction-time [site] snapshot would pin stale pageOnFront /
            // pageForPosts values onto the virtual rows.
            val currentSite = selectedSiteRepository.getSelectedSite() ?: site
            val showSiteEditorHomepage = siteEditorMVPFeatureConfig.isEnabled() && isBlockBasedTheme
            val rows = buildRows(
                pages = uiModels,
                applyHierarchy = applyHierarchy,
                pageOnFront = currentSite?.pageOnFront ?: 0L,
                pageForPosts = currentSite?.pageForPosts ?: 0L,
                showSiteEditorHomepage = showSiteEditorHomepage
            ).map { row -> row.withMenuActions(currentSite) }
            updateTabUiState(tab) {
                copy(
                    pages = rows,
                    isLoading = RsTabLoading.onItemsLoaded(
                        wasLoading = isLoading,
                        hasItems = rows.hasRealPages
                    ),
                    error = null,
                    isAuthError = false
                )
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
     * Carries over already-resolved author names and featured image URLs from the
     * current tab state onto freshly loaded items, so they don't visibly re-resolve.
     */
    private fun mergeCachedFields(
        tab: PageRsListTab,
        items: List<PageRsUiModel>,
        isSearch: Boolean
    ): List<PageRsUiModel> {
        val existingById = getTabUiState(tab).pages
            .associate { it.remotePageId to it.page }
        return items.map { model ->
            val existing = existingById[model.remotePageId]
            var resolved = model
            if (model.authorId != 0L && model.authorId == existing?.authorId) {
                resolved = resolved.copy(authorDisplayName = existing.authorDisplayName)
            }
            if (model.featuredImageId != 0L && model.featuredImageId == existing?.featuredImageId) {
                resolved = resolved.copy(featuredImageUrl = existing.featuredImageUrl)
            }
            resolved.copy(
                isFeaturedImageUnresolvable = model.featuredImageId in unresolvableImageIds,
                // Read straight from the metrics cache: rebuilding from the collection would
                // otherwise blank out numbers already fetched on every change it reports.
                viewCount = viewCountCache[model.remotePageId],
                // Search mixes statuses into one list and view counts are only fetched for
                // published pages, so skeletons there would never resolve.
                areMetricsPending = expectsMetrics(tab) &&
                    !isSearch &&
                    isMetricOutstanding(model.remotePageId)
            )
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
                restClient.fetchMediaUrls(
                    site, unresolvedIds, THUMBNAIL_SIZE_DP, THUMBNAIL_ASPECT
                )
            }
            // Anything the lookup did not answer for is recorded so its row stops waiting. The
            // request is a batch, so one unreadable item leaves every id in it unanswered. Anything
            // it did answer for is evicted, so an id that failed once and then resolved is not
            // still reported as unresolvable on the next reload.
            unresolvableImageIds.removeAll(urls.keys)
            unresolvableImageIds.addAll(unresolvedIds.filterNot { urls.containsKey(it) })
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

    /**
     * Flips the list between its two densities, persisting the choice for the posts list too.
     *
     * Condensed shows no metrics and so fetches none; returning to comfortable has to ask for them,
     * because the rows on screen have not changed and the visible-row stream is what normally
     * triggers a fetch.
     */
    @MainThread
    fun onDensityToggled(tab: PageRsListTab) {
        val next = ContentListDensity.of(!_density.value.isCondensed)
        _density.value = next
        appPrefsWrapper.isContentListCondensed = next.isCondensed
        launchCollectionJob {
            // Re-map the rows so their pending flags match the new density before anything fetches.
            loadItemsForTab(tab)
            // Published is the only tab whose mapping depends on density, because it is the only
            // one that shows metrics - so it is re-mapped too when the toggle was tapped from
            // somewhere else, or its rows keep pending flags computed under the old density.
            // Costs nothing when Published was never opened: loadItemsForTab has no collection to
            // read and returns.
            if (tab != PageRsListTab.PUBLISHED) loadItemsForTab(PageRsListTab.PUBLISHED)
            if (!next.isCondensed) retryMetricsForVisibleRows(tab)
        }
    }

    /**
     * Whether rows on [tab] should expect a view count at all, and so whether to show a skeleton.
     *
     * Published pages only, and only at comfortable density - a condensed row shows no metrics, so
     * it does not fetch them either. A draft or trashed page has no view history to report.
     */
    private fun expectsMetrics(tab: PageRsListTab) =
        tab == PageRsListTab.PUBLISHED && !_density.value.isCondensed

    /**
     * Search results mix statuses into one list and view counts are only fetched for published
     * pages, so anything shown while searching would wait on a fetch that never comes.
     */
    private val isSearching: Boolean get() = _searchQuery.value.isNotBlank()

    /** Whether a view count is still expected for [pageId] but has not arrived. */
    private fun isMetricOutstanding(pageId: Long) =
        canFetchViewCounts && !viewCountCache.containsKey(pageId)

    /**
     * Fetches view counts for the rows currently on screen.
     *
     * Driven by scroll position rather than by the page load because the stats API answers for one
     * page at a time. Volume is held down by debouncing on the caller's side, a single
     * [viewCountGate] shared across all calls, and re-checking the tab's visible rows once a permit is
     * granted - not by cancelling earlier batches, which would strand rows on their skeletons.
     */
    @MainThread
    fun onRowsVisible(tab: PageRsListTab, pageIds: List<Long>) {
        // Recorded before the guard, not after: a list opened condensed does not fetch, but it
        // still has to know what is on screen so that switching to comfortable can ask for it.
        // Otherwise retryMetricsForVisibleRows finds an empty set and the rows shimmer for good.
        visiblePageIds[tab] = pageIds.toSet()
        if (!expectsMetrics(tab) || isSearching) return
        fetchViewCounts(tab, pageIds)
    }

    @MainThread
    private fun fetchViewCounts(tab: PageRsListTab, pageIds: List<Long>) {
        // The stats data source authenticates with the WP.com bearer token and throws if it is
        // asked for data before being given one.
        val accessToken = accountStore.accessToken.takeUnless { it.isNullOrEmpty() }
        val siteId = site?.siteId ?: return
        val wanted = pageIds.filter {
            !viewCountCache.containsKey(it) && it !in inFlightViewCounts
        }
        if (!canFetchViewCounts || accessToken == null || wanted.isEmpty()) return

        track(viewModelScope.launch {
            statsDataSource.init(accessToken)
            wanted.forEach { pageId ->
                launch {
                    viewCountGate.withPermit {
                        // Re-checked after waiting for a permit rather than before queuing: by the
                        // time a slot frees up the user may have scrolled well past this row, and
                        // fetching it would spend a request on something off screen.
                        if (pageId in visiblePageIds[tab].orEmpty()) {
                            fetchViewCountFor(tab, siteId, pageId)
                        }
                    }
                }
            }
        })
    }

    /** Keeps a job around so teardown can cancel it, and forgets it once it finishes. */
    private fun track(job: Job) {
        metricJobs.add(job)
        job.invokeOnCompletion { metricJobs.remove(job) }
    }

    /**
     * Fetches one page's view count.
     *
     * Metrics decorate the rows; the list is perfectly usable without them, so nothing in this path
     * is allowed to take the screen down.
     */
    private suspend fun fetchViewCountFor(tab: PageRsListTab, siteId: Long, pageId: Long) {
        // Re-checked here, not just when the batch was queued: ids waiting on [viewCountGate] are
        // not yet recorded as in flight, so the same page can be queued twice and the first fetch
        // can land before the second gets its permit.
        if (viewCountCache.containsKey(pageId)) return
        if (!inFlightViewCounts.add(pageId)) return
        try {
            // A null either way: the fetch failed, or it answered with nothing usable. Both mean
            // the row has no number to show and should stop waiting for one.
            @Suppress("TooGenericExceptionCaught")
            val views = try {
                val result = withContext(Dispatchers.IO) {
                    statsDataSource.fetchPostViews(siteId = siteId, postId = pageId)
                }
                (result as? PostViewsDataResult.Success)?.data?.totalViews
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Failed to fetch view count for page $pageId", e)
                null
            }
            viewCountCache[pageId] = views
            applyMetrics(tab, listOf(pageId))
        } finally {
            inFlightViewCounts.remove(pageId)
        }
    }

    /**
     * Re-requests view counts for the rows already on screen, for when something other than
     * scrolling put them back into the pending state.
     */
    @MainThread
    private fun retryMetricsForVisibleRows(tab: PageRsListTab) {
        val visible = visiblePageIds[tab].orEmpty()
        if (visible.isEmpty()) return
        onRowsVisible(tab, visible.toList())
    }

    /** Pushes whatever the cache now holds for [pageIds] onto the tab's rows. */
    @MainThread
    private fun applyMetrics(tab: PageRsListTab, pageIds: List<Long>) {
        val touched = pageIds.toSet()
        updateTabUiState(tab) {
            copy(
                pages = pages.map { item ->
                    if (item.remotePageId in touched) {
                        item.withPage(
                            item.page.copy(
                                viewCount = viewCountCache[item.remotePageId],
                                // Mirrors the guard in loadItemsForTab: without it a late-landing
                                // fetch could raise a skeleton over a search result.
                                areMetricsPending = !isSearching &&
                                    isMetricOutstanding(item.remotePageId)
                            )
                        )
                    } else {
                        item
                    }
                }
            )
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
        // The SITE_EDITOR virtual has no backing page, so it gets no overflow menu. Its synthetic
        // page already has empty actions, so this leaves it unchanged below.
        val isSiteEditor = this is PageRsListItem.Virtual &&
            kind == PageRsListItem.Virtual.Kind.SITE_EDITOR
        val actions = if (isSiteEditor) {
            emptyList()
        } else {
            computePageMenuActions(
                status = page.status,
                isHomepage = pageOnFront != 0L && page.remotePageId == pageOnFront,
                isPostsPage = pageForPosts != 0L && page.remotePageId == pageForPosts,
                hasPassword = page.hasPassword,
                isBlazeEligibleSite = site != null && blazeFeatureUtils.isSiteBlazeEligible(site),
                canManageHomepage = canManageHomepage
            )
        }
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
        val url = urls[page.featuredImageId]
        val updated = when {
            url != null -> page.copy(
                featuredImageUrl = url,
                isFeaturedImageUnresolvable = false
            )
            // A row whose image the batch could not answer for stops shimmering rather than
            // waiting on a lookup that is not retried until the next refresh.
            page.featuredImageId in unresolvableImageIds ->
                page.copy(isFeaturedImageUnresolvable = true)
            else -> return this
        }
        return withPage(updated)
    }

    private suspend fun updateListInfoForTab(tab: PageRsListTab) {
        val collection = collections[tab] ?: return
        // Mid-fill the paging state flickers page by page under rows that aren't changing, and
        // its errors are the refresh's to report.
        if (tab in fillingTabs) return

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
        val hasPages = getTabUiState(tab).pages.hasRealPages
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
                    isLoading = RsTabLoading.onListInfoChanged(
                        wasLoading = isLoading,
                        isFetchingFirstPage = fetchingFirstPage,
                        hasItems = pages.hasRealPages,
                        hasFetched = tab in fetchedTabs
                    ),
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
        refreshJobs.clear()
        fillingTabs.clear()
        fetchedTabs.clear()
        resolveImageJobs.values.forEach { it.cancel() }
        resolveImageJobs.clear()
        resolveAuthorJobs.values.forEach { it.cancel() }
        resolveAuthorJobs.clear()
        // Snapshot first: each job's completion handler removes it from metricJobs, and a job
        // parked on the view-count gate completes inline on Main.immediate during cancel().
        // Iterating the live set would throw as soon as a non-last job did.
        metricJobs.toList().forEach { it.cancel() }
        metricJobs.clear()
        visiblePageIds.clear()
        viewCountCache.clear()
        inFlightViewCounts.clear()
        unresolvableImageIds.clear()
        closeParentPickerCollection()
        parentPickerExcludedIds = emptySet()
        _parentPicker.value = null
        _parentPickerQuery.value = ""
        _tabStates.value = emptyMap()
    }

    public override fun onCleared() {
        super.onCleared()
        changeListener.stop()
        dispatcher.unregister(this)
        clearCollections()
    }

    companion object {
        /** The REST maximum, and what iOS uses for pages. */
        private const val PAGE_SIZE = 100

        /**
         * A bound on the published tab's page-through, not a product limit: it only exists so a
         * server that always reports another page can't keep the loop going.
         */
        private const val MAX_FILL_PAGES = 50
        private const val SEARCH_DEBOUNCE_MS = 250L
        private const val SITE_EDITOR_LAUNCH_DEBOUNCE_MS = 1000L
        internal const val MIN_SEARCH_QUERY_LENGTH = 3
        private const val THUMBNAIL_SIZE_DP = 64

        /**
         * View counts are one request each, so a screenful is fetched a few at a time rather than
         * all at once.
         */
        private const val MAX_CONCURRENT_VIEW_FETCHES = 4

        /** Rows show the thumbnail in a square slot, cropped to fill. */
        private const val THUMBNAIL_ASPECT = 1f
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
