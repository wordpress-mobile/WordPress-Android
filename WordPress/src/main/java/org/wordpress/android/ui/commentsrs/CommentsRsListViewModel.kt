package org.wordpress.android.ui.commentsrs

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsComment
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsCommentsPageResult
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsResult
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.items.listitem.SiteCapabilityChecker
import org.wordpress.android.ui.postsrs.PostRsErrorUtils
import org.wordpress.android.ui.postsrs.SnackbarMessage
import org.wordpress.android.ui.rs.RsDateFormatter
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPAvatarUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.CommentListParams
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpErrorCode
import javax.inject.Inject
import javax.inject.Named

/**
 * ViewModel for the wordpress-rs comments list. Unlike the postsrs/pagesrs screens there is no
 * rs cache support for comments yet, so each tab pages directly against `/wp/v2/comments`,
 * keeping the response's `nextPageParams` as the pagination cursor.
 */
@HiltViewModel
class CommentsRsListViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    private val commentsRsDataSource: CommentsRsDataSource,
    private val siteCapabilityChecker: SiteCapabilityChecker,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val avatarUtilsWrapper: WPAvatarUtilsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val commentBrowsingSession: CommentBrowsingSession,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _tabStates = MutableStateFlow<Map<CommentsRsListTab, CommentsTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<CommentsRsListTab, CommentsTabUiState>> = _tabStates.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Whether the current query is long enough to search. The single owner of the minimum-length
     * policy — the debounce filter, the [initTab] guard, and the screen's idle state all derive
     * from it rather than re-implementing the trim-and-compare rule.
     */
    val isQuerySearchable: StateFlow<Boolean> = _searchQuery
        .map { isSearchable(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _events = Channel<CommentsRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    // Selection mode is active while this is non-empty.
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    // Whether the current user may moderate comments on this site (moderate_comments capability).
    // Fetched once on init; false until it resolves so the batch actions start disabled and enable
    // once confirmed, rather than briefly offering actions the server would reject.
    private val _canModerate = MutableStateFlow(false)
    val canModerate: StateFlow<Boolean> = _canModerate.asStateFlow()

    // The current user's WP user id, resolved once on init (from the same "me" fetch as
    // canModerate); null until then, or if it couldn't be fetched. Used only to tell the user's
    // own replies apart when threading the Unreplied tab. Read and written only on the main thread.
    private var currentUserId: Long? = null

    // Raw comments fetched per tab, accumulated across pages. The Unreplied tab derives its
    // displayed rows from this via client-side threading, so a reply and the parent it answers can
    // arrive on different pages and still be matched.
    private val rawComments = mutableMapOf<CommentsRsListTab, List<RsComment>>()

    // Pagination cursors: the next-page params returned with each fetched page, per tab.
    private val nextPageParams = mutableMapOf<CommentsRsListTab, CommentListParams?>()

    // Bumped whenever a first page is applied. A load-more result whose fetch started before the
    // bump paged from a cursor that predates the new page 1, so it's stale and gets discarded.
    private val pageGenerations = mutableMapOf<CommentsRsListTab, Int>()

    // The in-flight first-page fetch per tab, so overlapping refreshes don't duplicate it.
    private val firstPageJobs = mutableMapOf<CommentsRsListTab, Job>()

    // The in-flight load-more fetch per tab, so clearTabs() can cancel it: a load-more that
    // survived a clear could otherwise land on a reset page-generation counter and append its
    // stale rows into the freshly fetched list.
    private val loadMoreJobs = mutableMapOf<CommentsRsListTab, Job>()

    // The tab the pager last settled on. Kept separate from [lastTrackedTab] (the analytics
    // dedupe key) so changes to tracking behavior can't silently change which tab a debounced
    // search targets.
    private var currentTab: CommentsRsListTab? = null

    private var lastTrackedTab: CommentsRsListTab? = null

    // The in-flight title-resolve job per tab.
    private val resolveTitleJobs = mutableMapOf<CommentsRsListTab, Job>()

    // Bumped by clearTabs(). Long-lived jobs (batch moderation) capture it at launch and skip
    // their completion-time state writes when it moved: the rows they refer to are gone.
    private var clearGeneration = 0

    private val _site: SiteModel? = selectedSiteRepository.getSelectedSite()
    private val site: SiteModel
        get() = requireNotNull(_site) { "No selected site — Activity should have finished" }

    init {
        if (_site == null) {
            _events.trySend(CommentsRsListEvent.ShowToast(R.string.blog_not_found))
            _events.trySend(CommentsRsListEvent.Finish)
        } else {
            viewModelScope.launch {
                // Both values come from the same cached "me" fetch inside SiteCapabilityChecker.
                _canModerate.value = withContext(bgDispatcher) { siteCapabilityChecker.canModerateComments(site) }
                currentUserId = withContext(bgDispatcher) { siteCapabilityChecker.currentUserId(site) }
                // If the Unreplied tab loaded before the id resolved, re-thread it now that the
                // user's own replies can be identified.
                rethreadUnreplied()
            }
            @OptIn(FlowPreview::class)
            viewModelScope.launch {
                _searchQuery
                    // One shared normalization decides both halves of the search policy below:
                    // trim + dedupe so whitespace-only edits ("abc" -> "abc ") neither clear nor
                    // refetch, and so a whitespace-only query can't pass the length filter and
                    // fetch (and present) the full unfiltered list as search results.
                    .map { it.trim() }
                    .distinctUntilChanged()
                    // Skip the initial "" so opening the screen doesn't clear the loading tabs.
                    .drop(1)
                    // Clear immediately on any change: displayed rows always belong to the last
                    // executed search and must not linger as apparent matches for a different
                    // query — whether edited below the minimum or replaced wholesale (select-all
                    // + paste, a keyboard suggestion, voice input). The isSearchActive guard
                    // keeps the close transition intact: onSearchClose resets the query itself
                    // and rebuilds the tabs synchronously.
                    .onEach { if (_isSearchActive.value) clearTabs() }
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .filter { isSearchable(it) }
                    .collect {
                        // No clear here: every mutation path cleared at mutation time, so any
                        // surviving tab content was already fetched with this query (e.g. by a
                        // pager settle inside the debounce window) and initTab's containsKey
                        // guard keeps it. currentTab follows the pager, so this targets the tab
                        // the user is actually looking at.
                        initTab(currentTab ?: CommentsRsListTab.ALL)
                    }
            }
        }
    }

    /** Clears all tab states so the list appears empty while the user types a query. */
    @MainThread
    fun onSearchOpen() {
        onClearSelection()
        _isSearchActive.value = true
        clearTabs()
    }

    /**
     * Updates the search query. The pipeline in `init` reacts to it: an immediate clear on any
     * trimmed change, then a debounced fetch once the query is searchable.
     */
    @MainThread
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Closes search mode: clears the query and immediately re-initializes [activeTab] so the
     * normal tab content appears without debounce delay.
     */
    @MainThread
    fun onSearchClose(activeTab: CommentsRsListTab) {
        onClearSelection()
        _isSearchActive.value = false
        _searchQuery.value = ""
        clearTabs()
        initTab(activeTab)
    }

    /**
     * Drops every tab's rows and pagination bookkeeping so tabs re-initialize from scratch
     * (entering, changing, or leaving a search). Every in-flight fetch is cancelled: a fetch
     * started before the clear must not resurrect a stale tab, and a surviving load-more would
     * land on a reset generation counter and mix its stale page into the fresh list.
     */
    private fun clearTabs() {
        clearGeneration++
        firstPageJobs.values.forEach { it.cancel() }
        firstPageJobs.clear()
        loadMoreJobs.values.forEach { it.cancel() }
        loadMoreJobs.clear()
        resolveTitleJobs.values.forEach { it.cancel() }
        resolveTitleJobs.clear()
        rawComments.clear()
        nextPageParams.clear()
        pageGenerations.clear()
        _tabStates.value = emptyMap()
    }

    /** Loads the first page for [tab] unless it's already initialized. */
    @MainThread
    @Suppress("ReturnCount")
    fun initTab(tab: CommentsRsListTab) {
        // No selected site: init already queued a toast + Finish. Bail before registering the tab
        // so we neither strand a permanently-loading tab nor let a later refreshTab reach the
        // site getter (requireNotNull) via clearPostTitles.
        if (_site == null) return
        // updateTabUiState inserts the tab synchronously, so this also blocks re-entry while
        // the first fetch is in flight.
        if (_tabStates.value.containsKey(tab)) return
        // While a search is open with a below-minimum query, the screen shows the idle state and
        // the debounce collector hasn't fired — an init here (pager settle, Activity recreation)
        // would fetch unfiltered or below-minimum results behind the idle screen.
        if (_isSearchActive.value && !isSearchable(_searchQuery.value)) return
        updateTabUiState(tab) { copy(isLoading = true) }
        fetchFirstPage(tab)
    }

    /**
     * Re-fetches the first page for [tab]. The pull-to-refresh indicator is only shown for a
     * user-initiated refresh; silent refreshes (after moderation, returning from the detail)
     * keep the current list on screen until the new page arrives.
     */
    @MainThread
    @Suppress("ReturnCount")
    fun refreshTab(tab: CommentsRsListTab, isUserRefresh: Boolean = false) {
        // As in initTab: with no selected site the clearPostTitles(site) call below would hit the
        // requireNotNull. Guard the entry point rather than deeper down.
        if (_site == null) return
        if (!_tabStates.value.containsKey(tab)) return
        // A first page is already on its way (initial load or another refresh); a second fetch
        // would only duplicate the request and double-bump the page generation.
        if (firstPageJobs[tab]?.isActive == true) return
        if (isUserRefresh) {
            // A refresh can remove selected comments from the list (they changed server-side),
            // which would leave the global selection out of sync with the visible checkmarks —
            // batch actions acting on off-screen comments, back presses that appear to do
            // nothing. Ending selection mode keeps the two in agreement.
            onClearSelection()
            // An explicit refresh retries every post title for the site: ones that previously
            // resolved as unresolvable (e.g. a post published since it was first seen) and ones
            // that may have gone stale (e.g. a post renamed since it was cached).
            commentsRsDataSource.clearPostTitles(site)
            // A resolve job that started before the clear may apply pre-clear titles; cancel it
            // so the post-refresh pass fetches fresh results.
            resolveTitleJobs[tab]?.cancel()
        }
        updateTabUiState(tab) { copy(isRefreshing = isUserRefresh, error = null) }
        fetchFirstPage(tab, showErrorSnackbar = isUserRefresh)
    }

    /** Refreshes every initialized tab; comments move between tabs when moderated. */
    @MainThread
    fun refreshAllTabs() {
        _tabStates.value.keys.forEach { refreshTab(it) }
    }

    /** Loads the next page for [tab] if there is one and nothing else is in flight. */
    @MainThread
    fun loadMore(tab: CommentsRsListTab) {
        loadMoreInternal(tab, autoAdvanceDepth = 0)
    }

    private fun loadMoreInternal(tab: CommentsRsListTab, autoAdvanceDepth: Int) {
        val current = getTabUiState(tab)
        val params = nextPageParams[tab]
        val isBusy = current.isLoading || current.isRefreshing || current.isLoadingMore
        if (params == null || isBusy) return
        // As in fetchFirstPage: don't start a page request that can't reach the server.
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            onLoadMoreError(tab, message = null, reason = null)
            return
        }

        updateTabUiState(tab) { copy(isLoadingMore = true) }
        val generation = pageGenerations[tab]
        loadMoreJobs[tab] = viewModelScope.launch {
            val result = withContext(bgDispatcher) { commentsRsDataSource.fetchCommentsPage(site, params) }
            if (pageGenerations[tab] != generation) {
                // A refresh replaced the list while this page was in flight, so its cursor is
                // stale; appending it would mix old and new pages and corrupt the cursor chain.
                // Only touch tabs that still exist: writing through updateTabUiState after a
                // clearTabs() would resurrect the tab as a ghost entry that blocks initTab.
                if (_tabStates.value.containsKey(tab)) {
                    updateTabUiState(tab) { copy(isLoadingMore = false) }
                }
                return@launch
            }
            when (result) {
                is RsCommentsPageResult.Success -> {
                    val sizeBefore = getTabUiState(tab).comments.size
                    applyPage(tab, result, append = true)
                    // When a page doesn't grow the visible list, the scroll position hasn't moved
                    // so the load-more trigger won't re-fire; advance to the next page directly.
                    // `<=` (not `==`) also covers the shrink-to-empty case: an Unreplied page can
                    // carry the user's reply to the last visible comment, dropping the visible
                    // count to zero — with no rows there's no scroll trigger left to resume paging.
                    // The depth cap keeps a pathological cursor chain (e.g. a proxy ignoring the
                    // page param) from firing unbounded unattended requests.
                    if (getTabUiState(tab).comments.size <= sizeBefore &&
                        nextPageParams[tab] != null &&
                        autoAdvanceDepth < CommentBrowsingSession.MAX_AUTO_ADVANCE_PAGES
                    ) {
                        loadMoreInternal(tab, autoAdvanceDepth + 1)
                    }
                }
                is RsCommentsPageResult.Error ->
                    onLoadMoreError(tab, result.message, result.reason, result.errorCode)
            }
        }
    }

    /** Opens the rs comment detail for the tapped row, or toggles it while selecting. */
    @MainThread
    fun onCommentClick(remoteCommentId: Long) {
        if (_selectedIds.value.isNotEmpty()) {
            toggleSelection(remoteCommentId)
        } else {
            // Seed the detail's swipe pager with the current tab's comments and paging cursor so
            // it can swipe between them and keep loading more (the cursor can't cross an Intent).
            val tab = currentTab ?: CommentsRsListTab.ALL
            val ids = _tabStates.value[tab]?.comments?.map { it.remoteCommentId } ?: listOf(remoteCommentId)
            // Unreplied's rows are a client-side-threaded subset of the raw stream; the session
            // pages the raw stream, so handing it the cursor would let the pager swipe into the
            // replied-to and own comments the tab hides. Seed only the visible ids, no cursor.
            val cursor = if (tab == CommentsRsListTab.UNREPLIED) null else nextPageParams[tab]
            commentBrowsingSession.start(site, ids, cursor)
            _events.trySend(CommentsRsListEvent.OpenCommentDetail(site, remoteCommentId))
        }
    }

    /** Enters selection mode with the long-pressed row selected. */
    @MainThread
    fun onCommentLongClick(remoteCommentId: Long) {
        toggleSelection(remoteCommentId)
    }

    @MainThread
    fun onClearSelection() {
        _selectedIds.value = emptySet()
    }

    /**
     * Runs [action] on the selected comments, first asking for confirmation for the destructive
     * ones (trash, delete) like the legacy list.
     */
    @MainThread
    fun onBatchAction(action: CommentsRsBatchAction, tab: CommentsRsListTab) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        if (action.confirmation != null) {
            _pendingConfirmation.value = PendingConfirmation(action, ids)
        } else {
            performBatchModeration(ids, action.targetStatus, tab)
        }
    }

    @MainThread
    fun onConfirmPendingAction(tab: CommentsRsListTab) {
        _pendingConfirmation.value?.let { performBatchModeration(it.commentIds, it.action.targetStatus, tab) }
        _pendingConfirmation.value = null
    }

    @MainThread
    fun onDismissPendingAction() {
        _pendingConfirmation.value = null
    }

    private fun toggleSelection(remoteCommentId: Long) {
        _selectedIds.update { ids ->
            if (remoteCommentId in ids) ids - remoteCommentId else ids + remoteCommentId
        }
    }

    /**
     * Applies [newStatus] to every comment in [ids] in parallel, then refreshes all initialized
     * tabs (moderated comments move between tabs). Failures are aggregated into one snackbar.
     */
    private fun performBatchModeration(ids: List<Long>, newStatus: CommentStatus, tab: CommentsRsListTab) {
        // Batch actions are disabled without moderation rights; guard here too so nothing can reach
        // the server with a request it would only reject with a 403.
        if (!_canModerate.value) return
        if (!checkNetwork()) return
        trackBatchModeration(newStatus)
        onClearSelection()
        updateTabUiState(tab) { copy(isRefreshing = true) }
        val generationAtStart = clearGeneration
        viewModelScope.launch {
            val results = withContext(bgDispatcher) {
                ids.map { commentId ->
                    async {
                        if (newStatus == CommentStatus.DELETED) {
                            commentsRsDataSource.delete(site, commentId)
                        } else {
                            commentsRsDataSource.updateStatus(site, commentId, newStatus)
                        }
                    }
                }.awaitAll()
            }
            // awaitAll preserves order, so results line up with ids.
            val failedIds = ids.filterIndexed { index, _ -> results[index] is RsResult.Error }
            if (failedIds.isNotEmpty()) {
                val message = if (ids.size == 1) {
                    resourceProvider.getString(R.string.comments_rs_moderation_failed_single)
                } else {
                    resourceProvider.getString(
                        R.string.comments_rs_moderation_failed_multiple, failedIds.size, ids.size
                    )
                }
                _snackbarMessages.trySend(SnackbarMessage(message))
                // Failed comments kept their status and place in the list, so re-selecting them
                // lets the user retry immediately instead of hunting them down again — unless
                // clearTabs() ran while the batch was in flight (search opened, closed, or query
                // changed): this job outlives the clear, and re-selecting rows that may no longer
                // be displayed would leave an invisible selection intercepting back presses and
                // row taps. A batch run inside an unchanged search keeps the affordance: its
                // failed rows are still in the refreshed results.
                if (clearGeneration == generationAtStart) {
                    _selectedIds.value = failedIds.toSet()
                }
            }
            refreshAllTabs()
        }
    }

    private fun trackBatchModeration(newStatus: CommentStatus) {
        val stat = when (newStatus) {
            CommentStatus.APPROVED -> Stat.COMMENT_BATCH_APPROVED
            CommentStatus.UNAPPROVED -> Stat.COMMENT_BATCH_UNAPPROVED
            CommentStatus.SPAM -> Stat.COMMENT_BATCH_SPAMMED
            CommentStatus.TRASH -> Stat.COMMENT_BATCH_TRASHED
            CommentStatus.DELETED -> Stat.COMMENT_BATCH_DELETED
            else -> return
        }
        analyticsTracker.track(stat)
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

    /**
     * The pager settled on a new tab: clears any active selection (like the legacy action mode)
     * and tracks the same COMMENT_FILTER_CHANGED event (including the initial selection) the
     * legacy list emits, so filter metrics stay comparable.
     */
    @MainThread
    fun onTabChanged(tab: CommentsRsListTab) {
        currentTab = tab
        // A re-emission of the same tab (Activity recreation restarting the pager's settled-page
        // flow) is not a tab change: it must neither re-track nor clear an active selection.
        if (tab == lastTrackedTab) return
        onClearSelection()
        lastTrackedTab = tab
        analyticsTracker.track(
            Stat.COMMENT_FILTER_CHANGED,
            mapOf(SELECTED_FILTER_PROPERTY to resourceProvider.getString(tab.trackingLabelResId))
        )
    }

    private fun fetchFirstPage(tab: CommentsRsListTab, showErrorSnackbar: Boolean = true) {
        // A fetch with no connection can only fail, so report it without the round trip.
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            onFirstPageError(
                tab,
                message = null,
                reason = null,
                errorCode = null,
                showErrorSnackbar = showErrorSnackbar
            )
            return
        }
        // Callers (initTab, refreshTab) guard against a null site, so the site getter below is safe.
        firstPageJobs[tab] = viewModelScope.launch {
            val base = commentsRsDataSource.firstPageParams(
                status = tab.queryStatus,
                search = _searchQuery.value.trim().ifBlank { null }
            )
            // Unreplied over-fetches (like the legacy list) since client-side threading discards
            // most of each page, so a single page still yields visible rows.
            val params = if (tab == CommentsRsListTab.UNREPLIED) base.copy(perPage = UNREPLIED_PAGE_SIZE) else base
            val result = withContext(bgDispatcher) { commentsRsDataSource.fetchCommentsPage(site, params) }
            when (result) {
                is RsCommentsPageResult.Success -> {
                    applyPage(tab, result, append = false)
                    advanceIfFilteredEmpty(tab)
                }
                is RsCommentsPageResult.Error ->
                    onFirstPageError(tab, result.message, result.reason, result.errorCode, showErrorSnackbar)
            }
        }
    }

    /**
     * A fetched page: stores the next-page cursor, applies the rows to the tab state, and kicks
     * off post-title resolution. [append] is true when the page extends the list (load more)
     * rather than replacing it (first page / refresh).
     */
    private fun applyPage(tab: CommentsRsListTab, result: RsCommentsPageResult.Success, append: Boolean) {
        if (!append) pageGenerations[tab] = (pageGenerations[tab] ?: 0) + 1
        nextPageParams[tab] = result.nextPageParams
        // A comment can shift pages if the list changed server-side between requests, so dedupe
        // the accumulated raw set on append. Displayed rows are always derived from this raw set,
        // which lets Unreplied thread a reply against a parent fetched on an earlier page.
        val raw = if (append) {
            (rawComments[tab].orEmpty() + result.comments).distinctBy { it.remoteCommentId }
        } else {
            result.comments
        }
        rawComments[tab] = raw
        updateTabUiState(tab) {
            copy(
                comments = displayRows(tab, raw),
                isLoading = false,
                isRefreshing = false,
                // A replacing page must clear isLoadingMore too: a load-more in flight when the
                // refresh landed is discarded by the generation check, so nothing else resets
                // the flag promptly and paging would stay blocked behind the isBusy guard.
                isLoadingMore = false,
                canLoadMore = result.nextPageParams != null,
                error = null
            )
        }
        resolvePostTitles(tab)
    }

    /**
     * Maps a tab's accumulated raw comments to displayed rows. The Unreplied tab is threaded
     * client-side to only the comments the user hasn't replied to; every other tab shows its
     * comments as-is.
     */
    private fun displayRows(tab: CommentsRsListTab, raw: List<RsComment>): List<CommentRsUiModel> {
        // Rows are rebuilt from title-less RsComments, so carry over any post titles already
        // resolved for the tab (keyed by post — a title is the same for every comment on a post).
        // Without this, each rebuild (load-more, re-thread) would blank titles until resolvePostTitles
        // re-fetches them, and re-thread — which never re-resolves — would lose them for good.
        val knownTitles = getTabUiState(tab).comments
            .filter { it.postTitle != null }
            .associate { it.postId to it.postTitle }
        val visible = if (tab == CommentsRsListTab.UNREPLIED) filterUnreplied(raw, currentUserId) else raw
        val nowLabel = resourceProvider.getString(R.string.rs_date_now)
        return visible.map { it.toUiModel(nowLabel).copy(postTitle = knownTitles[it.postId]) }
    }

    /** Re-derives the Unreplied tab's rows from its raw comments (e.g. once [currentUserId] resolves). */
    private fun rethreadUnreplied() {
        val tab = CommentsRsListTab.UNREPLIED
        val raw = rawComments[tab] ?: return
        val rows = displayRows(tab, raw)
        updateTabUiState(tab) { copy(comments = rows) }
        // Re-threading can drop rows that were over-reported while currentUserId was still null;
        // clear any selection for comments no longer shown so an off-screen id can't intercept
        // back presses or target a batch action (the same hazard refreshTab guards against).
        val visibleIds = rows.mapTo(mutableSetOf()) { it.remoteCommentId }
        _selectedIds.update { it intersect visibleIds }
        // Re-threading with the now-known id can empty a page that previously over-reported, so
        // keep paging rather than stranding matches behind a false empty state.
        advanceIfFilteredEmpty(tab)
    }

    /**
     * Keeps paging while a filtered tab (Unreplied) shows no rows but the raw stream has more:
     * a full page can thread away to nothing, and the list's load-more trigger only fires once
     * rows are on screen. Bounded by the same auto-advance cap as load-more.
     */
    private fun advanceIfFilteredEmpty(tab: CommentsRsListTab) {
        if (getTabUiState(tab).comments.isEmpty() && nextPageParams[tab] != null) {
            loadMoreInternal(tab, autoAdvanceDepth = 0)
        }
    }

    /**
     * A page that didn't arrive leaves the list as it is and offers a retry snackbar. All-null
     * failure details mean no request was made because the device is offline.
     */
    private fun onLoadMoreError(
        tab: CommentsRsListTab,
        message: String?,
        reason: RequestExecutionErrorReason?,
        errorCode: WpErrorCode? = null
    ) {
        val authError = PostRsErrorUtils.isAuthError(reason, errorCode)
        updateTabUiState(tab) { copy(isLoadingMore = false) }
        _snackbarMessages.trySend(
            SnackbarMessage(
                message = errorMessage(message, reason, errorCode),
                actionLabel = if (authError) null else resourceProvider.getString(R.string.retry),
                onAction = if (authError) null else ({ loadMore(tab) })
            )
        )
    }

    /**
     * First-page failure: when the tab already shows comments keep them and offer a retry
     * snackbar; when it's empty, show the full-screen error state. Silent refreshes (returning
     * from the detail) skip the snackbar — a failed background refresh shouldn't interrupt a
     * user who's still looking at a perfectly good list.
     */
    private fun onFirstPageError(
        tab: CommentsRsListTab,
        message: String?,
        reason: RequestExecutionErrorReason?,
        errorCode: WpErrorCode?,
        showErrorSnackbar: Boolean
    ) {
        val friendly = errorMessage(message, reason, errorCode)
        val authError = PostRsErrorUtils.isAuthError(reason, errorCode)
        if (getTabUiState(tab).comments.isNotEmpty()) {
            updateTabUiState(tab) {
                copy(isLoading = false, isRefreshing = false, error = null, isAuthError = authError)
            }
            if (showErrorSnackbar) {
                // Retrying an auth failure just fails again, so offer the message without
                // the action.
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = friendly,
                        actionLabel = if (authError) null
                            else resourceProvider.getString(R.string.retry),
                        onAction = if (authError) null
                            else ({ refreshTab(tab, isUserRefresh = true) })
                    )
                )
            }
        } else {
            updateTabUiState(tab) {
                copy(isLoading = false, isRefreshing = false, error = friendly, isAuthError = authError)
            }
        }
    }

    /**
     * Resolves the "commented on {post}" titles for rows that don't have one yet, in a single
     * batched request per tab. Rows keep an author-only title until (or if ever) resolved.
     */
    private fun resolvePostTitles(tab: CommentsRsListTab) {
        val unresolvedIds = getTabUiState(tab).comments
            .filter { it.postTitle == null && it.postId > 0 }
            .map { it.postId }
            .distinct()
        if (unresolvedIds.isEmpty()) return

        // Cancel-and-relaunch is cheap even when the ids haven't changed: the data source
        // caches resolved titles, so a re-fired request skips them.
        resolveTitleJobs[tab]?.cancel()
        resolveTitleJobs[tab] = viewModelScope.launch {
            val titles = withContext(bgDispatcher) { commentsRsDataSource.fetchPostTitles(site, unresolvedIds) }
            if (titles.isEmpty()) return@launch
            updateTabUiState(tab) {
                copy(comments = comments.map { it.copy(postTitle = titles[it.postId] ?: it.postTitle) })
            }
        }
    }

    /**
     * The server message when it sent one, otherwise a friendly offline/auth/generic message.
     *
     * Auth failures ignore the server's wording: the retry action is withheld for those, so the
     * message is all the user gets and a raw 401 body won't tell them their credentials are the
     * problem. Posts and pages show the same string for the same failure.
     */
    private fun errorMessage(
        serverMessage: String?,
        reason: RequestExecutionErrorReason? = null,
        errorCode: WpErrorCode? = null
    ): String =
        serverMessage?.takeIf { it.isNotBlank() && !PostRsErrorUtils.isAuthError(reason, errorCode) }
            ?: PostRsErrorUtils.friendlyErrorMessage(
                resourceProvider = resourceProvider,
                networkUtilsWrapper = networkUtilsWrapper,
                reason = reason,
                errorCode = errorCode
            )

    private fun RsComment.toUiModel(nowLabel: String) = CommentRsUiModel(
        remoteCommentId = remoteCommentId,
        authorName = authorName.ifBlank { resourceProvider.getString(R.string.anonymous) },
        avatarUrl = avatarUtilsWrapper.rewriteAvatarUrlWithResource(authorAvatarUrl, R.dimen.avatar_sz_medium),
        snippet = HtmlUtils.fastStripHtml(contentHtml).trim(),
        relativeDate = RsDateFormatter.format(dateGmt, nowLabel),
        status = status,
        postId = postId
    )

    private fun isSearchable(query: String) = query.trim().length >= MIN_SEARCH_QUERY_LENGTH

    private fun getTabUiState(tab: CommentsRsListTab): CommentsTabUiState =
        _tabStates.value[tab] ?: CommentsTabUiState(isLoading = true)

    private fun updateTabUiState(tab: CommentsRsListTab, update: CommentsTabUiState.() -> CommentsTabUiState) {
        _tabStates.value += (tab to getTabUiState(tab).update())
    }

    companion object {
        // Same property key as the legacy list's tracking (UnifiedCommentsActivity).
        private const val SELECTED_FILTER_PROPERTY = "selected_filter"

        private const val SEARCH_DEBOUNCE_MS = 250L
        private const val MIN_SEARCH_QUERY_LENGTH = 3

        // Larger page for the Unreplied tab (matching the legacy list) to offset the rows that
        // client-side threading filters out.
        private const val UNREPLIED_PAGE_SIZE = 100u
    }
}
