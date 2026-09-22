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
import org.wordpress.android.ui.rs.contentlist.ContentListDefaults.LOAD_MORE_THRESHOLD
import org.wordpress.android.ui.rs.contentlist.ContentListDefaults.REVEAL_TIMEOUT_MS
import org.wordpress.android.ui.rs.contentlist.ContentListDefaults.VISIBLE_ROWS_DEBOUNCE_MS

/**
 * Asks for the next page once the user scrolls within [LOAD_MORE_THRESHOLD] rows of the end.
 *
 * Keyed on [canLoadMore] so the flow is torn down rather than filtered when there is nothing left
 * to fetch, and de-duplicated so a scroll that stays inside the threshold asks only once.
 */
@Composable
fun LoadMoreOnScrollToEnd(
    listState: LazyListState,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - LOAD_MORE_THRESHOLD
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
