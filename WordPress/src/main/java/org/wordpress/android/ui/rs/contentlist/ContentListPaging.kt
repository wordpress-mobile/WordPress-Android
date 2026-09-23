package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Asks for the next page once the user scrolls within [LOAD_MORE_THRESHOLD] rows of the end.
 *
 * Keyed on [canLoadMore] so the flow is torn down rather than filtered when there is nothing left
 * to fetch, and de-duplicated so a scroll that stays inside the threshold asks only once.
 *
 * Also keyed on [itemCount]: a refresh that truncates the list, or an appended page, restarts the
 * flow, so a `true` latched by the de-duplication before the change can't suppress the re-fire
 * needed to resume paging. The view models' busy guards make those re-fires safe.
 */
@Composable
fun LoadMoreOnScrollToEnd(
    listState: LazyListState,
    itemCount: Int,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, canLoadMore, itemCount) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            // total > 0 keeps the pre-layout pass (empty layoutInfo, 0 >= -threshold) from asking
            // for the next page before the user has scrolled at all.
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - LOAD_MORE_THRESHOLD
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadMore()
        }
    }
}

/**
 * Reports which rows are on screen, once the scroll settles.
 *
 * Per-row metrics are one request each, so the view model is told what is actually visible rather
 * than fetching for the whole loaded page. [visibleRowIds] is the caller's because the two lists
 * key their rows differently - posts on the remote id, pages on an index into their own entries.
 *
 * [enabled] is a [LaunchedEffect] key rather than a guard inside it, so turning it off tears the
 * flow down instead of leaving it collecting into nothing.
 */
@OptIn(FlowPreview::class)
@Composable
fun ReportVisibleRows(
    listState: LazyListState,
    enabled: Boolean,
    onRowsVisible: (List<Long>) -> Unit,
    visibleRowIds: () -> List<Long>,
) {
    val currentOnRowsVisible by rememberUpdatedState(onRowsVisible)
    val currentVisibleRowIds by rememberUpdatedState(visibleRowIds)
    LaunchedEffect(listState, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow { currentVisibleRowIds() }
            // A fling changes the visible set on nearly every frame. Without settling first, each
            // of those emissions would start fetching for rows already gone from the screen.
            .debounce(VISIBLE_ROWS_DEBOUNCE_MS)
            .distinctUntilChanged()
            .collect { currentOnRowsVisible(it) }
    }
}

/**
 * Scrolls to a row the user has just saved, once the refresh carrying it lands.
 *
 * [rowIndexOf] takes the id to a position in the list rather than in the data, because the posts
 * list interleaves date-group headers and those occupy list positions of their own. It returns a
 * negative value until the row arrives.
 *
 * `requestScrollToItem` applies at the next measurement rather than to the content currently laid
 * out, which matters because a keyed LazyColumn re-anchors on its old first item when one is
 * prepended: a plain `scrollToItem` would leave a newly published row just above the viewport.
 */
@Composable
fun RevealRow(
    revealId: Long?,
    listState: LazyListState,
    onRevealHandled: () -> Unit,
    rowIndexOf: (Long) -> Int,
) {
    val currentRowIndexOf by rememberUpdatedState(rowIndexOf)
    LaunchedEffect(revealId) {
        if (revealId == null) return@LaunchedEffect
        val index = withTimeoutOrNull(REVEAL_TIMEOUT_MS) {
            snapshotFlow { currentRowIndexOf(revealId) }.first { it >= 0 }
        }
        if (index != null) listState.requestScrollToItem(index)
        // Disarm either way. Most tabs refresh to page 1 only (the pages list's published tab is
        // the exception, refreshing to its complete set), so a row that sorts beyond it never
        // arrives here; leaving the request armed would fire it much later, when load-more
        // finally paged it in and the user was reading something else.
        onRevealHandled()
    }
}

/**
 * How long the visible-row set must settle before metrics are fetched for it. Tuned against
 * [LOAD_MORE_THRESHOLD]: short enough that a stopped scroll fetches promptly, long enough that a
 * fling does not fetch for every row it passes, and the next page has to be asked for first.
 */
private const val VISIBLE_ROWS_DEBOUNCE_MS = 300L

/** How close to the end of the list the user has to scroll before the next page is asked for. */
private const val LOAD_MORE_THRESHOLD = 5

/**
 * How long a reveal waits for the refresh carrying the new post or page to land before giving up.
 * Long enough for a slow site, short enough that it cannot scroll the list under someone who has
 * moved on.
 */
private const val REVEAL_TIMEOUT_MS = 15_000L
