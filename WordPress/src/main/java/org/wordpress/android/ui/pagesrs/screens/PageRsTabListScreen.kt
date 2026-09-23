package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.FlowPreview
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsListItem
import org.wordpress.android.ui.pagesrs.PageRsMenuAction
import org.wordpress.android.ui.pagesrs.SITE_EDITOR_PAGE_ID
import org.wordpress.android.ui.pagesrs.hasRealPages
import org.wordpress.android.ui.rs.RsTabUiState
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListErrorState
import org.wordpress.android.ui.rs.contentlist.ContentListPlaceholderRow
import org.wordpress.android.ui.rs.contentlist.ContentListShimmer
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LegacyContentListPlaceholderRow
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.ReportVisibleRows
import org.wordpress.android.ui.rs.contentlist.RevealRow
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageRsTabListScreen(
    state: RsTabUiState<PageRsListItem>,
    emptyMessageResId: Int,
    revealPageId: Long?,
    onRevealHandled: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPageClick: (Long) -> Unit,
    onPageMenuAction: (Long, PageRsMenuAction) -> Unit,
    onRowsVisible: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    isSearchIdle: Boolean = false,
    isSearching: Boolean = false,
    isRedesignEnabled: Boolean = false,
    density: ContentListDensity = ContentListDensity.COMFORTABLE
) {
    ContentListPullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        when {
            isSearchIdle -> Box(Modifier.fillMaxSize())
            state.isLoading -> ShimmerList(isRedesignEnabled)
            state.error != null && !state.items.hasRealPages -> {
                ContentListErrorState(
                    error = state.error,
                    onRetry = if (state.isAuthError) null else onRefresh
                )
            }
            state.items.isEmpty() && !state.isRefreshing -> {
                ContentListEmptyState(
                    messageResId = if (isSearching) {
                        R.string.pages_empty_search_result
                    } else {
                        emptyMessageResId
                    }
                )
            }
            else -> PageListContent(
                pages = state.items,
                revealPageId = revealPageId,
                onRevealHandled = onRevealHandled,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                onLoadMore = onLoadMore,
                onPageClick = onPageClick,
                onPageMenuAction = onPageMenuAction,
                onRowsVisible = onRowsVisible,
                isRedesignEnabled = isRedesignEnabled,
                density = density
            )
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun PageListContent(
    pages: List<PageRsListItem>,
    revealPageId: Long?,
    onRevealHandled: () -> Unit,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onPageClick: (Long) -> Unit,
    onPageMenuAction: (Long, PageRsMenuAction) -> Unit,
    onRowsVisible: (List<Long>) -> Unit,
    isRedesignEnabled: Boolean,
    density: ContentListDensity
) {
    val listState = rememberLazyListState()

    val currentPages by rememberUpdatedState(pages)

    // Scrolls to a page the user just saved, once the refresh carrying it lands. The published and
    // draft tabs sort by title, so it can be anywhere in the list.
    RevealRow(revealPageId, listState, onRevealHandled) { id ->
        currentPages.indexOfFirst { it.remotePageId == id }
    }

    // Rows are keyed by a String, so the ids come from the entries the visible indexes land on.
    // The Site Editor row has no page behind it and so no view count to ask for.
    ReportVisibleRows(listState, enabled = isRedesignEnabled, onRowsVisible = onRowsVisible) {
        val entries = currentPages
        listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
            entries.getOrNull(info.index)
                ?.remotePageId
                ?.takeIf { it != SITE_EDITOR_PAGE_ID }
        }
    }

    LoadMoreOnScrollToEnd(listState, pages.size, canLoadMore, onLoadMore)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = pages,
            key = { it.stableKey }
        ) { item ->
            val onClick = { onPageClick(item.remotePageId) }
            val onMenuAction = { action: PageRsMenuAction ->
                onPageMenuAction(item.remotePageId, action)
            }
            if (isRedesignEnabled) {
                PageRsRedesignedRow(
                    item = item,
                    density = density,
                    onClick = onClick,
                    onMenuAction = onMenuAction,
                    modifier = Modifier.animateItem()
                )
            } else {
                PageRsRow(
                    item = item,
                    onClick = onClick,
                    onMenuAction = onMenuAction,
                    modifier = Modifier.animateItem()
                )
            }
        }

        if (isLoadingMore) contentListLoadingMoreItem()
    }
}

@Composable
private fun ShimmerList(isRedesignEnabled: Boolean) {
    ContentListShimmer {
        if (isRedesignEnabled) ContentListPlaceholderRow() else LegacyContentListPlaceholderRow()
    }
}



