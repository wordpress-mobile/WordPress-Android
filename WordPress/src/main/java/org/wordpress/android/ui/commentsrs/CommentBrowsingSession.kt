package org.wordpress.android.ui.commentsrs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsCommentsPageResult
import uniffi.wp_api.CommentListParams
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory handoff that lets the comment detail pager swipe through the same comments the list is
 * showing, and keep loading more as the user reaches the end.
 *
 * The list can't pass its pagination cursor to the detail Activity through an Intent — the
 * wordpress-rs [CommentListParams] cursor is neither Serializable nor Parcelable — so the list
 * seeds this shared singleton at tap time and the detail reads it. Only the list entry point seeds
 * a session; the notifications detail opens a single comment and leaves this inactive (so its pager
 * shows one non-swipeable page).
 */
@Singleton
class CommentBrowsingSession @Inject constructor(
    private val commentsRsDataSource: CommentsRsDataSource
) {
    private val _commentIds = MutableStateFlow<List<Long>>(emptyList())

    /** The ordered comment ids to page through, growing as [loadMore] fetches further pages. */
    val commentIds: StateFlow<List<Long>> = _commentIds.asStateFlow()

    private var site: SiteModel? = null
    private var nextPageParams: CommentListParams? = null
    private var isLoadingMore = false

    /** Whether the session was seeded (list entry point) vs. a bare single-comment open. */
    val isActive: Boolean get() = site != null

    /** Whether there is another page to fetch. */
    val canLoadMore: Boolean get() = nextPageParams != null

    /**
     * Seeds the session from the list's current tab: the visible comment [ids] (in display order)
     * and the tab's live [nextPageParams] cursor, so the detail continues paging from where the
     * list left off.
     */
    fun start(site: SiteModel, ids: List<Long>, nextPageParams: CommentListParams?) {
        this.site = site
        this.nextPageParams = nextPageParams
        this.isLoadingMore = false
        _commentIds.value = ids
    }

    /**
     * Fetches the next page(s) and appends their comment ids, deduping against the ids already
     * shown. Returns true once new ids were added. Skips empty/fully-deduped pages up to
     * [MAX_AUTO_ADVANCE_PAGES] (mirroring the list's cursor auto-advance) so a server-side shift
     * doesn't silently stall paging. No-op when there is no next page or a fetch is already running.
     */
    suspend fun loadMore(): Boolean {
        val site = this.site
        if (site == null || isLoadingMore) return false
        isLoadingMore = true
        return try {
            fetchUntilNewIds(site)
        } finally {
            isLoadingMore = false
        }
    }

    private suspend fun fetchUntilNewIds(site: SiteModel): Boolean {
        var attempts = 0
        while (attempts < MAX_AUTO_ADVANCE_PAGES) {
            val result = nextPageParams?.let { commentsRsDataSource.fetchCommentsPage(site, it) }
            // Null cursor (no more pages) or a fetch error: stop advancing.
            if (result !is RsCommentsPageResult.Success) break
            attempts++
            nextPageParams = result.nextPageParams
            val before = _commentIds.value
            _commentIds.value = (before + result.comments.map { it.remoteCommentId }).distinct()
            if (_commentIds.value.size > before.size) return true
        }
        return false
    }

    /** Clears the session so a later single-comment open (e.g. from a notification) doesn't inherit it. */
    fun clear() {
        site = null
        nextPageParams = null
        isLoadingMore = false
        _commentIds.value = emptyList()
    }

    companion object {
        // How many pages a single load-more may fetch unattended when pages dedupe away to
        // nothing. Shared with CommentsRsListViewModel so both auto-advance loops stay in step.
        internal const val MAX_AUTO_ADVANCE_PAGES = 3
    }
}
