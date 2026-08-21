package org.wordpress.android.ui.rs

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Scrolls [listState] until the item [targetKey] identifies is on screen, waiting for it to arrive
 * if the list is still being refreshed. Gives up quietly if it never does.
 *
 * [indexOfTarget] and [targetKey] read the list the UI is rendering, and return -1 / null until the
 * item is in it.
 *
 * The list moves while this runs, which is what makes it more than a single scroll call. A
 * LazyColumn keyed by post id re-anchors on its previous first item when one is prepended, so a
 * scroll issued the moment a new item reaches the state - before the list has been measured with
 * it - is undone a frame later, leaving the item just above the viewport: the list appears to
 * scroll, but not far enough. [LazyListState.requestScrollToItem] avoids that by applying at the
 * next measurement, against the content that caused it. The request is repeated for as long as the
 * item's position keeps changing, and stops once the list has actually rendered it.
 */
internal suspend fun revealListItem(
    listState: LazyListState,
    indexOfTarget: () -> Int,
    targetKey: () -> Any?
) {
    withTimeoutOrNull(REVEAL_TIMEOUT_MS) {
        coroutineScope {
            val requests = launch {
                snapshotFlow(indexOfTarget)
                    .filter { it >= 0 }
                    .collect { index -> listState.requestScrollToItem(index) }
            }
            snapshotFlow {
                val key = targetKey()
                key != null && listState.layoutInfo.visibleItemsInfo.any { it.key == key }
            }.first { it }
            requests.cancel()
        }
    }
}

/**
 * How long a reveal waits for the refreshed list to include the item before giving up. Generous
 * because the refresh behind it is a network round trip that started moments earlier.
 */
private const val REVEAL_TIMEOUT_MS = 15_000L
