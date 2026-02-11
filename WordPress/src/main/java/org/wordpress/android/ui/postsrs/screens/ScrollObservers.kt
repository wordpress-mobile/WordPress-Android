package org.wordpress.android.ui.postsrs.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

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
    LaunchedEffect(listState) {
        val scrollFlow = snapshotFlow {
            listState.layoutInfo
                .visibleItemsInfo.lastOrNull()?.index ?: 0
        }
        val stateFlow = snapshotFlow {
            Triple(itemCount, canLoadMore, isLoadingMore)
        }
        combine(scrollFlow, stateFlow) { lastVisible, (count, canLoad, loading) ->
            lastVisible >= count - 1 && canLoad && !loading
        }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad) onLoadMore()
            }
    }
}
