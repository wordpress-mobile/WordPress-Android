package org.wordpress.android.ui.posts.rslist.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

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
            listState.layoutInfo
                .visibleItemsInfo.lastOrNull()?.index ?: 0
        }
    }

    LaunchedEffect(
        lastVisibleItemIndex.value,
        itemCount,
        canLoadMore
    ) {
        val shouldLoadMore =
            lastVisibleItemIndex.value >= itemCount - 1 &&
                canLoadMore &&
                !isLoadingMore
        if (shouldLoadMore) {
            onLoadMore()
        }
    }
}
