package org.wordpress.android.ui.commentsrs

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
import org.wordpress.android.ui.postsrs.PostRsErrorUtils
import org.wordpress.android.ui.postsrs.SnackbarMessage
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPAvatarUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.CommentListParams
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
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val avatarUtilsWrapper: WPAvatarUtilsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _tabStates = MutableStateFlow<Map<CommentsRsListTab, CommentsTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<CommentsRsListTab, CommentsTabUiState>> = _tabStates.asStateFlow()

    private val _events = Channel<CommentsRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    // Selection mode is active while this is non-empty.
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    // Pagination cursors: the next-page params returned with each fetched page, per tab.
    private val nextPageParams = mutableMapOf<CommentsRsListTab, CommentListParams?>()

    // Bumped whenever a first page is applied. A load-more result whose fetch started before the
    // bump paged from a cursor that predates the new page 1, so it's stale and gets discarded.
    private val pageGenerations = mutableMapOf<CommentsRsListTab, Int>()

    // The in-flight first-page fetch per tab, so overlapping refreshes don't duplicate it.
    private val firstPageJobs = mutableMapOf<CommentsRsListTab, Job>()

    private var lastTrackedTab: CommentsRsListTab? = null

    // The in-flight title-resolve job per tab.
    private val resolveTitleJobs = mutableMapOf<CommentsRsListTab, Job>()

    private val _site: SiteModel? = selectedSiteRepository.getSelectedSite()
    private val site: SiteModel
        get() = requireNotNull(_site) { "No selected site — Activity should have finished" }

    init {
        if (_site == null) {
            _events.trySend(CommentsRsListEvent.ShowToast(R.string.blog_not_found))
            _events.trySend(CommentsRsListEvent.Finish)
        }
    }

    /** Loads the first page for [tab] unless it's already initialized. */
    @MainThread
    fun initTab(tab: CommentsRsListTab) {
        // updateTabUiState inserts the tab synchronously, so this also blocks re-entry while
        // the first fetch is in flight.
        if (_tabStates.value.containsKey(tab)) return
        updateTabUiState(tab) { copy(isLoading = true) }
        fetchFirstPage(tab)
    }

    /**
     * Re-fetches the first page for [tab]. The pull-to-refresh indicator is only shown for a
     * user-initiated refresh; silent refreshes (after moderation, returning from the detail)
     * keep the current list on screen until the new page arrives.
     */
    @MainThread
    fun refreshTab(tab: CommentsRsListTab, isUserRefresh: Boolean = false) {
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

        updateTabUiState(tab) { copy(isLoadingMore = true) }
        val generation = pageGenerations[tab]
        viewModelScope.launch {
            val result = withContext(bgDispatcher) { commentsRsDataSource.fetchCommentsPage(site, params) }
            if (pageGenerations[tab] != generation) {
                // A refresh replaced the list while this page was in flight, so its cursor is
                // stale; appending it would mix old and new pages and corrupt the cursor chain.
                updateTabUiState(tab) { copy(isLoadingMore = false) }
                return@launch
            }
            when (result) {
                is RsCommentsPageResult.Success -> {
                    val sizeBefore = getTabUiState(tab).comments.size
                    applyPage(tab, result, append = true)
                    // When a page adds no rows (it deduped away after a server-side shift, or
                    // came back empty mid-shrink), the scroll position hasn't changed so the
                    // load-more trigger won't re-fire; advance to the next page directly. The
                    // depth cap keeps a pathological cursor chain (e.g. a proxy ignoring the
                    // page param) from firing unbounded unattended requests.
                    if (getTabUiState(tab).comments.size == sizeBefore &&
                        nextPageParams[tab] != null &&
                        autoAdvanceDepth < MAX_AUTO_ADVANCE_PAGES
                    ) {
                        loadMoreInternal(tab, autoAdvanceDepth + 1)
                    }
                }
                is RsCommentsPageResult.Error -> {
                    updateTabUiState(tab) { copy(isLoadingMore = false) }
                    _snackbarMessages.trySend(
                        SnackbarMessage(
                            message = errorMessage(result.message),
                            actionLabel = resourceProvider.getString(R.string.retry),
                            onAction = { loadMore(tab) }
                        )
                    )
                }
            }
        }
    }

    /** Opens the rs comment detail for the tapped row, or toggles it while selecting. */
    @MainThread
    fun onCommentClick(remoteCommentId: Long) {
        if (_selectedIds.value.isNotEmpty()) {
            toggleSelection(remoteCommentId)
        } else {
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
        if (!checkNetwork()) return
        trackBatchModeration(newStatus)
        onClearSelection()
        updateTabUiState(tab) { copy(isRefreshing = true) }
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
                // lets the user retry immediately instead of hunting them down again.
                _selectedIds.value = failedIds.toSet()
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
        onClearSelection()
        if (tab == lastTrackedTab) return
        lastTrackedTab = tab
        analyticsTracker.track(
            Stat.COMMENT_FILTER_CHANGED,
            mapOf(SELECTED_FILTER_PROPERTY to resourceProvider.getString(tab.trackingLabelResId))
        )
    }

    private fun fetchFirstPage(tab: CommentsRsListTab, showErrorSnackbar: Boolean = true) {
        firstPageJobs[tab] = viewModelScope.launch {
            val params = commentsRsDataSource.firstPageParams(status = tab.queryStatus)
            val result = withContext(bgDispatcher) { commentsRsDataSource.fetchCommentsPage(site, params) }
            when (result) {
                is RsCommentsPageResult.Success -> applyPage(tab, result, append = false)
                is RsCommentsPageResult.Error -> onFirstPageError(tab, result.message, showErrorSnackbar)
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
        val newRows = result.comments.map { it.toUiModel() }
        updateTabUiState(tab) {
            copy(
                // A comment can shift pages if the list changed server-side between requests,
                // so dedupe on append.
                comments = if (append) (comments + newRows).distinctBy { it.remoteCommentId } else newRows,
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
     * First-page failure: when the tab already shows comments keep them and offer a retry
     * snackbar; when it's empty, show the full-screen error state. Silent refreshes (returning
     * from the detail) skip the snackbar — a failed background refresh shouldn't interrupt a
     * user who's still looking at a perfectly good list.
     */
    private fun onFirstPageError(tab: CommentsRsListTab, message: String?, showErrorSnackbar: Boolean) {
        val friendly = errorMessage(message)
        if (getTabUiState(tab).comments.isNotEmpty()) {
            updateTabUiState(tab) { copy(isLoading = false, isRefreshing = false, error = null) }
            if (showErrorSnackbar) {
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = friendly,
                        actionLabel = resourceProvider.getString(R.string.retry),
                        onAction = { refreshTab(tab, isUserRefresh = true) }
                    )
                )
            }
        } else {
            updateTabUiState(tab) {
                copy(comments = emptyList(), isLoading = false, isRefreshing = false, error = friendly)
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

    /** The server message when it sent one, otherwise a friendly offline/generic message. */
    private fun errorMessage(serverMessage: String?): String =
        serverMessage?.takeIf { it.isNotBlank() }
            ?: PostRsErrorUtils.friendlyErrorMessage(null, null, resourceProvider, networkUtilsWrapper)

    private fun RsComment.toUiModel() = CommentRsUiModel(
        remoteCommentId = remoteCommentId,
        authorName = authorName.ifBlank { resourceProvider.getString(R.string.anonymous) },
        avatarUrl = avatarUtilsWrapper.rewriteAvatarUrlWithResource(authorAvatarUrl, R.dimen.avatar_sz_medium),
        snippet = HtmlUtils.fastStripHtml(contentHtml).trim(),
        relativeDate = dateTimeUtilsWrapper.javaDateToTimeSpan(dateGmt),
        status = status,
        postId = postId
    )

    private fun getTabUiState(tab: CommentsRsListTab): CommentsTabUiState =
        _tabStates.value[tab] ?: CommentsTabUiState(isLoading = true)

    private fun updateTabUiState(tab: CommentsRsListTab, update: CommentsTabUiState.() -> CommentsTabUiState) {
        _tabStates.value += (tab to getTabUiState(tab).update())
    }

    companion object {
        // How many extra pages a single load-more may fetch unattended when applied pages
        // add no rows, before waiting for the next user scroll.
        private const val MAX_AUTO_ADVANCE_PAGES = 3

        // Same property key as the legacy list's tracking (UnifiedCommentsActivity).
        private const val SELECTED_FILTER_PROPERTY = "selected_filter"
    }
}
