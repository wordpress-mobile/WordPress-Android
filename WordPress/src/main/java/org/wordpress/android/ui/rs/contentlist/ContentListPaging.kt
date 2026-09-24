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
 * Asks for the next page once the user scrolls within [LOAD_MORE_THRESHOLD] rows of the end. Keyed
 * on [itemCount] so a `true` latched before the list changed can't suppress the next trigger.
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
            // total > 0 skips the pre-layout pass, which would otherwise ask for page 2 at once.
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - LOAD_MORE_THRESHOLD
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadMore()
        }
    }
}

/** Reports which rows are on screen once the scroll settles, so metrics are fetched only for those. */
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
            .debounce(VISIBLE_ROWS_DEBOUNCE_MS)
            .distinctUntilChanged()
            .collect { currentOnRowsVisible(it) }
    }
}

/**
 * Scrolls to a row the user has just saved, once the refresh carrying it lands. [rowIndexOf]
 * returns a list position (headers included), negative until the row arrives. Uses
 * `requestScrollToItem` because a keyed LazyColumn re-anchors when a row is prepended.
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
        // Disarm either way: a row beyond page 1 may never arrive, and firing later would be a surprise.
        onRevealHandled()
    }
}

private const val VISIBLE_ROWS_DEBOUNCE_MS = 300L
private const val LOAD_MORE_THRESHOLD = 5
private const val REVEAL_TIMEOUT_MS = 15_000L
