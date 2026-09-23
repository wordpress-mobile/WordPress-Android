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
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.rs.RsCollectionPrefetch
import org.wordpress.android.ui.rs.RsErrorUtils
import org.wordpress.android.ui.rs.RsFeaturedImages
import org.wordpress.android.ui.rs.RsFluxCBridge
import org.wordpress.android.ui.rs.RsMetricJobs
import org.wordpress.android.ui.rs.RsPostChangeListener
import org.wordpress.android.ui.rs.RsReveal
import org.wordpress.android.ui.rs.RsSnackbarMessage
import org.wordpress.android.ui.rs.RsTabLoading
import org.wordpress.android.ui.rs.RsTabRefreshJobs
import org.wordpress.android.ui.rs.RsTabUiState
import org.wordpress.android.ui.rs.RsUploadedPost
import org.wordpress.android.ui.rs.RsViewCounts
import org.wordpress.android.ui.rs.RsVisibleRows
import org.wordpress.android.ui.rs.checkNetwork
import org.wordpress.android.ui.rs.contentlist.ContentListDefaults.MIN_SEARCH_QUERY_LENGTH
import org.wordpress.android.ui.rs.contentlist.ContentListDefaults.SEARCH_DEBOUNCE_MS
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.toContentItemUiModel
import org.wordpress.android.ui.rs.data.FeaturedImageUrls
import org.wordpress.android.ui.rs.data.RsSiteRestClient
import org.wordpress.android.ui.rs.data.WpServiceProvider
import org.wordpress.android.ui.rs.sendWithRetry
import org.wordpress.android.ui.rs.toRsPostStatus
import org.wordpress.android.ui.prefs.AppPrefsWrapper
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
import uniffi.wp_api.WpApiParamPostsSearchColumn
import uniffi.wp_mobile.FetchException
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile.PostService
import uniffi.wp_mobile.SyncResult
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

@HiltViewModel
@Suppress("LargeClass", "LongParameterList")
internal class PagesRsListViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpServiceProvider,
    private val dispatcher: Dispatcher,
    private val restClient: RsSiteRestClient,
    private val resourceProvider: ResourceProvider,
    private val postStore: PostStore,
    private val homepageSettings: PageRsHomepageSettings,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val fluxCBridge: RsFluxCBridge,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val editorThemeStore: EditorThemeStore,
    private val siteEditorMVPFeatureConfig: SiteEditorMVPFeatureConfig,
    private val changeListener: RsPostChangeListener,
    private val statsDataSource: StatsDataSource,
) : ViewModel() {
    private val _tabStates = MutableStateFlow<Map<PageRsListTab, RsTabUiState<PageRsListItem>>>(emptyMap())
    val tabStates: StateFlow<Map<PageRsListTab, RsTabUiState<PageRsListItem>>> = _tabStates.asStateFlow()

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
    private var pendingReveal: RsReveal<PageRsListTab>? = null

    /** Tabs whose collection has completed at least one fetch, so an empty list means empty. */
    private val fetchedTabs = mutableSetOf<PageRsListTab>()
    private val resolveAuthorJobs = mutableMapOf<PageRsListTab, Job>()
    private var lastTrackedTab: PageRsListTab? = null

    private val metricJobs = RsMetricJobs()
    private val visiblePageIds = RsVisibleRows<PageRsListTab>()
    private val viewCounts = RsViewCounts(
        scope = viewModelScope,
        statsDataSource = statsDataSource,
        visibleRows = visiblePageIds,
        jobs = metricJobs,
        logTag = AppLog.T.PAGES,
        onCountsChanged = ::applyMetrics,
    )
    private val featuredImages = RsFeaturedImages(
        scope = viewModelScope,
        restClient = restClient,
        onImagesResolved = ::applyFeaturedImages,
    )

    private val _density = MutableStateFlow(
        ContentListDensity.of(appPrefsWrapper.isContentListCondensed)
    )
    val density: StateFlow<ContentListDensity> = _density.asStateFlow()

    private val _events = Channel<PageRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<RsSnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val _revealRequests = Channel<RsReveal<PageRsListTab>>(Channel.BUFFERED)
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
        pendingReveal = RsReveal(tab, upload.remotePostId)
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
        updateTabUiState(tab) { RsTabUiState(isLoading = true) }

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
                    RsTabUiState(
                        error = friendlyErrorMessage(e),
                        isAuthError = RsErrorUtils.isAuthError(e)
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
                val perPage = if (needsCompleteSet(tab)) FILL_PAGE_SIZE else PAGE_SIZE
                service.posts().getObservablePostMetadataCollectionWithEditContext(
                    endpointType = PostEndpointType.Pages,
                    filter = filter,
                    perPage = perPage.toUInt()
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
            val anyTabKeepsItsPages = tabs.any { getTabUiState(it).items.hasRealPages }
            tabs.forEach { onRefreshFailed(it, e = null, showSnackbar = false) }
            if (anyTabKeepsItsPages) {
                _snackbarMessages.trySend(
                    RsSnackbarMessage(
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
                        hasItems = items.hasRealPages,
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
            refreshJobs.deferIfRunning(tab, isUserRefresh) -> Unit

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
                if (fill) fillTab(tab, collection, sync)
                fetchedTabs.add(tab)
                // Drop only the "nothing to show" entries so a transient failure is retried,
                // while numbers already fetched stay put.
                viewCounts.invalidateUnresolved()
                featuredImages.invalidateUnresolved()
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
                if (fill) {
                    // A fill that fails part-way leaves the collection holding the pages it did
                    // load, and the next rebuild for any other reason would render those as a
                    // complete-looking tree. Show them now instead, with the paging state that
                    // lets scrolling finish the job, so the error below explains what is seen.
                    fillingTabs.remove(tab)
                    loadItemsForTab(tab)
                    updateListInfoForTab(tab)
                }
                onRefreshFailed(tab, e, showSnackbar = isUserRefresh)
            } finally {
                // A job cancelled by clearCollections unwinds here after the tab has been rebuilt,
                // so only release the guard while it is still this collection's to release.
                if (collections[tab] === collection) fillingTabs.remove(tab)
            }
            refreshJobs.onFinished(tab)?.let { replayAsUser -> refreshTab(tab, replayAsUser) }
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
     * legacy list. A page that fails past its retries throws, and the refresh reports it exactly
     * as it would a failed first page: the user asked for the whole tab, and a partial tree with
     * children shown as roots would look complete when it is not.
     *
     * Whether another page follows normally comes from the server's page count. When that is
     * missing - a proxy or plugin that strips `X-WP-TotalPages` - the page itself answers, as the
     * legacy list did: a full page may be followed by another, a short one is the last. A site
     * whose last page is exactly full then costs one request past the end, which the API refuses
     * as an invalid page number; that refusal is the answer, not an error.
     */
    private suspend fun fillTab(
        tab: PageRsListTab,
        collection: ObservableMetadataCollection,
        firstPage: SyncResult
    ) {
        val pageSize = FILL_PAGE_SIZE.toULong()
        var loaded = firstPage.totalItems
        val outcome = RsCollectionPrefetch.loadRemainingPages(
            hasMorePages = firstPage.hasMorePages ?: (loaded >= pageSize),
            maxPages = MAX_FILL_PAGES,
            shouldRetry = { !RsErrorUtils.isAuthError(it) }
        ) {
            val next = try {
                withContext(Dispatchers.IO) { collection.loadNextPage() }
            } catch (e: FetchException) {
                if (RsErrorUtils.isPastLastPage(e)) return@loadRemainingPages false
                throw e
            }
            val pageWasFull = next.totalItems - loaded >= pageSize
            loaded = next.totalItems
            next.hasMorePages ?: pageWasFull
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
        val authError = RsErrorUtils.isAuthError(e)
        if (getTabUiState(tab).items.hasRealPages) {
            updateTabUiState(tab) {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    isAuthError = authError
                )
            }
            if (showSnackbar) {
                // Tapping retry is the user asking, so the result has to be reported -
                // a silent second failure looks like the button did nothing.
                _snackbarMessages.sendWithRetry(message, authError, resourceProvider) {
                    refreshTab(tab, isUserRefresh = true)
                }
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
        val current = getTabUiState(tab)
        // A tab in fillingTabs is already being paged to the end by its refresh, and the observers
        // that would say so through isLoadingMore are held off while it is.
        val isBusy = tab in fillingTabs || current.isLoadingMore || current.isRefreshing
        if (isBusy || !current.canLoadMore) return

        updateTabUiState(tab) { copy(isLoadingMore = true) }

        launchCollectionJob {
            @Suppress("TooGenericExceptionCaught")
            try {
                withContext(Dispatchers.IO) { collection.loadNextPage() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FetchException) {
                if (RsErrorUtils.isPastLastPage(e)) {
                    // The server never said how many pages there are, so the list info kept
                    // offering another; the refusal is the answer, not a failure to report.
                    updateTabUiState(tab) { copy(isLoadingMore = false, canLoadMore = false) }
                } else {
                    onLoadMoreFailed(tab, e)
                }
            } catch (e: Exception) {
                onLoadMoreFailed(tab, e)
            }
        }
    }

    private fun onLoadMoreFailed(tab: PageRsListTab, e: Exception) {
        AppLog.e(AppLog.T.PAGES, "Failed to load more for tab $tab", e)
        updateTabUiState(tab) { copy(isLoadingMore = false) }
        _snackbarMessages.trySend(RsSnackbarMessage(friendlyErrorMessage(e)))
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
            ?.items
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
                    fluxCBridge.fetchAndBridgePage(remotePageId, site, lastModified)
                }
                _events.trySend(PageRsListEvent.EditPage(site, page))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "Bridge page failed", e)
                _snackbarMessages.trySend(
                    RsSnackbarMessage(friendlyErrorMessage(e, R.string.page_not_found))
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
            .flatMap { state -> state.items.map { it.page } }
            .distinctBy { it.remoteId }
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
     *
     * The filter searches titles only. That is what a parent picker should match, and it also
     * keeps the picker off the published tab's stored list: rs keys a list by its filter alone,
     * not its page size, and without this the two would share one list - each refresh resetting
     * the other's paging, and the differing page sizes making rs delete the list outright.
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
                    searchColumns = listOf(WpApiParamPostsSearchColumn.POST_TITLE),
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
                val candidates = collection.loadItems()
                    .map { it.state.toContentItemUiModel<PageRsMenuAction>(it.id, nowLabel) }
                candidates to collection.listInfo()
            }
            val candidates = items
                .filter { it.remoteId !in parentPickerExcludedIds }
                .filter { it.status is PostStatus.Publish || it.status is PostStatus.Private }
                .map { PageRsParentCandidate(it.remoteId, it.title) }
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
                    RsErrorUtils.friendlyErrorMessage(
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
                _snackbarMessages.trySend(RsSnackbarMessage(friendlyErrorMessage(e)))
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
                if (descendants.add(child.remoteId)) queue.addLast(child.remoteId)
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
                    RsSnackbarMessage(friendlyErrorMessage(e, R.string.page_status_change_error))
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
                        RsSnackbarMessage(resourceProvider.getString(successMessageResId))
                    )
                    launchCollectionJob { loadItemsForTab(PageRsListTab.PUBLISHED) }
                }
                is PageRsHomepageSettings.Result.StaticHomepageDisabled ->
                    _snackbarMessages.trySend(
                        RsSnackbarMessage(resourceProvider.getString(cannotSetMessageResId))
                    )
                is PageRsHomepageSettings.Result.Error -> {
                    AppLog.w(AppLog.T.PAGES, "Homepage settings update failed: ${result.message}")
                    _snackbarMessages.trySend(
                        RsSnackbarMessage(resourceProvider.getString(errorMessageResId))
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
                    fluxCBridge.fetchAndBridgePage(remotePageId, site, lastModified)
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
                    RsSnackbarMessage(friendlyErrorMessage(e, R.string.page_not_found))
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
            fluxCBridge.fetchAndBridgePage(remotePageId, site, lastModified)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.e(AppLog.T.PAGES, "Bridge page failed", e)
        _snackbarMessages.trySend(
            RsSnackbarMessage(friendlyErrorMessage(e, R.string.page_not_found))
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
                    RsSnackbarMessage(resourceProvider.getString(successMessageResId))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(AppLog.T.PAGES, "$logTag failed", e)
                _snackbarMessages.trySend(
                    RsSnackbarMessage(friendlyErrorMessage(e, errorMessageResId))
                )
            }
        }
    }

    /** Searches all tab states for a [PageRsUiModel] matching [remotePageId]. */
    private fun findPage(remotePageId: Long): PageRsUiModel? {
        for (state in _tabStates.value.values) {
            for (item in state.items) {
                if (item.remotePageId == remotePageId) return item.page
            }
        }
        return null
    }

    private fun logMissingLink(remotePageId: Long) {
        AppLog.w(AppLog.T.PAGES, "No link for page $remotePageId")
    }

    private fun checkNetwork(): Boolean =
        _snackbarMessages.checkNetwork(networkUtilsWrapper, resourceProvider)

    private fun friendlyErrorMessage(
        e: Exception?,
        defaultResId: Int? = null,
    ): String = RsErrorUtils.friendlyErrorMessage(
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
                    item.state.toContentItemUiModel<PageRsMenuAction>(item.id, nowLabel, showStatus = isSearch)
                }
            }
            val uiModels = mergeCachedFields(tab, items, isSearch)
            val applyHierarchy = needsCompleteSet(tab)
            // Re-read the site here: homepage settings can change while this screen is alive,
            // and the construction-time [site] snapshot would pin stale pageOnFront /
            // pageForPosts values onto the virtual rows.
            val currentSite = selectedSiteRepository.getSelectedSite() ?: site
            val showSiteEditorHomepage = siteEditorMVPFeatureConfig.isEnabled() && isBlockBasedTheme
            // Site-wide, so decided once rather than once per row of a list that can be long.
            val isBlazeEligibleSite = currentSite != null && blazeFeatureUtils.isSiteBlazeEligible(currentSite)
            val rows = buildRows(
                pages = uiModels,
                applyHierarchy = applyHierarchy,
                pageOnFront = currentSite?.pageOnFront ?: 0L,
                pageForPosts = currentSite?.pageForPosts ?: 0L,
                showSiteEditorHomepage = showSiteEditorHomepage
            ).map { row -> row.withMenuActions(currentSite, isBlazeEligibleSite) }
            updateTabUiState(tab) {
                copy(
                    items = rows,
                    isLoading = RsTabLoading.onItemsLoaded(
                        wasLoading = isLoading,
                        hasItems = rows.hasRealPages
                    ),
                    error = null,
                    isAuthError = false
                )
            }
            resolveAuthorNames(tab, uiModels)
            site?.let { featuredImages.resolve(tab, it, uiModels) }
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
        val existingById = getTabUiState(tab).items
            .associate { it.remotePageId to it.page }
        return items.map { model ->
            val existing = existingById[model.remoteId]
            var resolved = featuredImages.carryOver(model, existing)
            if (model.authorId != 0L && model.authorId == existing?.authorId) {
                resolved = resolved.copy(authorDisplayName = existing.authorDisplayName)
            }
            resolved.copy(
                // Read straight from the metrics cache: rebuilding from the collection would
                // otherwise blank out numbers already fetched on every change it reports.
                viewCount = viewCounts.countFor(model.remoteId),
                // Search mixes statuses into one list and view counts are only fetched for
                // published pages, so skeletons there would never resolve.
                areMetricsPending = expectsMetrics(tab) &&
                    !isSearch &&
                    isMetricOutstanding(model.remoteId)
            )
        }
    }

    private fun applyFeaturedImages(tab: PageRsListTab, images: Map<Long, FeaturedImageUrls>) {
        updateTabUiState(tab) {
            copy(items = items.map { it.withResolvedFeaturedImage(images) })
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
                copy(items = this.items.map { item -> item.withResolvedAuthor(names) })
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
        canFetchViewCounts && viewCounts.isOutstanding(pageId)

    /**
     * Records the rows on screen and asks [RsViewCounts] for the counts they are missing.
     *
     * Driven by scroll position rather than by the page load because the stats API answers for one
     * page at a time.
     */
    @MainThread
    fun onRowsVisible(tab: PageRsListTab, pageIds: List<Long>) {
        // Recorded before the guard, not after: a list opened condensed does not fetch, but it
        // still has to know what is on screen so that switching to comfortable can ask for it.
        // Otherwise retryMetricsForVisibleRows finds an empty set and the rows shimmer for good.
        visiblePageIds.record(tab, pageIds)
        if (!expectsMetrics(tab) || isSearching) return
        fetchViewCounts(tab, pageIds)
    }

    @MainThread
    private fun fetchViewCounts(tab: PageRsListTab, pageIds: List<Long>) {
        // The counts come from WordPress.com, so there is nothing to ask for without a bearer token.
        val hasAccessToken = !accountStore.accessToken.isNullOrEmpty()
        val siteId = site?.siteId
        if (!canFetchViewCounts || !hasAccessToken || siteId == null) return
        viewCounts.fetch(tab, siteId, pageIds)
    }

    /**
     * Re-requests view counts for the rows already on screen, for when something other than
     * scrolling put them back into the pending state.
     */
    @MainThread
    private fun retryMetricsForVisibleRows(tab: PageRsListTab) {
        val visible = visiblePageIds.visible(tab)
        if (visible.isEmpty()) return
        onRowsVisible(tab, visible.toList())
    }

    /** Pushes whatever the cache now holds for [pageIds] onto the tab's rows. */
    @MainThread
    private fun applyMetrics(tab: PageRsListTab, pageIds: List<Long>) {
        val touched = pageIds.toSet()
        updateTabUiState(tab) {
            copy(
                items = items.map { item ->
                    if (item.remotePageId in touched) {
                        item.withPage(
                            item.page.copy(
                                viewCount = viewCounts.countFor(item.remotePageId),
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

    private fun PageRsListItem.withMenuActions(
        site: SiteModel?,
        isBlazeEligibleSite: Boolean
    ): PageRsListItem {
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
                isHomepage = pageOnFront != 0L && page.remoteId == pageOnFront,
                isPostsPage = pageForPosts != 0L && page.remoteId == pageForPosts,
                hasPassword = page.hasPassword,
                isBlazeEligibleSite = isBlazeEligibleSite,
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

    private fun PageRsListItem.withResolvedFeaturedImage(
        images: Map<Long, FeaturedImageUrls>
    ): PageRsListItem {
        val updated = featuredImages.withImage(page, images)
        return if (updated === page) this else withPage(updated)
    }

    private suspend fun updateListInfoForTab(tab: PageRsListTab) {
        val collection = collections[tab]
        // Mid-fill the paging state flickers page by page under rows that aren't changing, and
        // its errors are the refresh's to report.
        if (collection == null || tab in fillingTabs) return

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
        val hasPages = getTabUiState(tab).items.hasRealPages
        val errorMessage = if (isError) {
            RsErrorUtils.friendlyErrorMessage(null, null, resourceProvider, networkUtilsWrapper)
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
                        hasItems = items.hasRealPages,
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

    private fun getTabUiState(tab: PageRsListTab): RsTabUiState<PageRsListItem> {
        return _tabStates.value[tab] ?: RsTabUiState(isLoading = true)
    }

    private fun updateTabUiState(
        tab: PageRsListTab,
        update: RsTabUiState<PageRsListItem>.() -> RsTabUiState<PageRsListItem>
    ) {
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
        resolveAuthorJobs.values.forEach { it.cancel() }
        resolveAuthorJobs.clear()
        metricJobs.cancelAll()
        visiblePageIds.clear()
        viewCounts.clear()
        featuredImages.clear()
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
        private const val PAGE_SIZE = 20

        /**
         * The REST maximum, used only by the collection that is paged to the end: the other tabs,
         * the search collection and the parent picker are read a screen at a time, and each page
         * also fetches the entities behind it.
         */
        private const val FILL_PAGE_SIZE = 100

        /**
         * A bound on the published tab's page-through, not a product limit: it only exists so a
         * server that always reports another page can't keep the loop going.
         */
        private const val MAX_FILL_PAGES = 50
        private const val SITE_EDITOR_LAUNCH_DEBOUNCE_MS = 1000L

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
