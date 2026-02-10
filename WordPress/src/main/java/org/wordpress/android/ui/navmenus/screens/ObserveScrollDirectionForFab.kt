package org.wordpress.android.ui.navmenus.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

/**
 * Observes the scroll direction of a [LazyListState] and calls
 * [onFabVisibilityChange] with `false` when scrolling down
 * (to hide the FAB) and `true` when scrolling up (to show it).
 */
@Composable
fun ObserveScrollDirectionForFab(
    listState: LazyListState,
    onFabVisibilityChange: (Boolean) -> Unit
) {
    LaunchedEffect(listState) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to
                listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val scrollingDown = index > prevIndex ||
                (index == prevIndex && offset > prevOffset)
            val scrollingUp = index < prevIndex ||
                (index == prevIndex && offset < prevOffset)
            if (scrollingDown) {
                onFabVisibilityChange(false)
            } else if (scrollingUp) {
                onFabVisibilityChange(true)
            }
            prevIndex = index
            prevOffset = offset
        }
    }
}

/**
 * Observes scroll position and triggers [onLoadMore] when the
 * last visible item is near the end of the list.
 */
@Composable
fun ObserveLoadMore(
    listState: LazyListState,
    itemCount: Int,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    val lastVisibleItemIndex = remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: 0
        }
    }

    LaunchedEffect(lastVisibleItemIndex.value, itemCount, canLoadMore) {
        val shouldLoadMore =
            lastVisibleItemIndex.value >= itemCount - 1 &&
                canLoadMore &&
                !isLoadingMore
        if (shouldLoadMore) {
            onLoadMore()
        }
    }
}
