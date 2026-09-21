package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsListItem
import org.wordpress.android.ui.pagesrs.PageRsMenuAction
import org.wordpress.android.ui.pagesrs.PageTabUiState
import org.wordpress.android.ui.pagesrs.SITE_EDITOR_PAGE_ID
import org.wordpress.android.ui.pagesrs.hasRealPages
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListPlaceholderRow
import org.wordpress.android.ui.rs.contentlist.LegacyContentListPlaceholderRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageRsTabListScreen(
    state: PageTabUiState,
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
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = state.isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        when {
            isSearchIdle -> Box(Modifier.fillMaxSize())
            state.isLoading -> ShimmerList(isRedesignEnabled)
            state.error != null && !state.pages.hasRealPages -> {
                ErrorContent(
                    error = state.error,
                    onRetry = if (state.isAuthError) null else onRefresh
                )
            }
            state.pages.isEmpty() && !state.isRefreshing -> {
                EmptyContent(
                    emptyMessageResId = if (isSearching) {
                        R.string.pages_empty_search_result
                    } else {
                        emptyMessageResId
                    }
                )
            }
            else -> PageListContent(
                pages = state.pages,
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
    // draft tabs sort by title, so it can be anywhere in the list. requestScrollToItem applies at
    // the next measurement rather than to the content currently laid out, which a plain
    // scrollToItem would, leaving a just-added row short of the viewport.
    LaunchedEffect(revealPageId) {
        if (revealPageId == null) return@LaunchedEffect
        val index = withTimeoutOrNull(REVEAL_TIMEOUT_MS) {
            snapshotFlow { currentPages.indexOfFirst { it.remotePageId == revealPageId } }
                .first { it >= 0 }
        }
        if (index != null) listState.requestScrollToItem(index)
        // Disarm either way. A refresh replaces the list with page 1 only, so a page that sorts
        // beyond it never arrives here; leaving the request armed would fire it much later, when
        // load-more finally paged the page in and the user was reading something else.
        onRevealHandled()
    }

    // Per-page view counts are one request each, so the ViewModel is told which rows are actually
    // on screen rather than fetching for the whole loaded page. Rows are keyed by a String, so the
    // ids come from the entries the visible indexes land on.
    val currentOnRowsVisible by rememberUpdatedState(onRowsVisible)
    LaunchedEffect(listState, isRedesignEnabled) {
        if (!isRedesignEnabled) return@LaunchedEffect
        snapshotFlow {
            val entries = currentPages
            listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                entries.getOrNull(info.index)
                    ?.remotePageId
                    ?.takeIf { it != SITE_EDITOR_PAGE_ID }
            }
        }
            // A fling changes the visible set on nearly every frame. Without settling first, each
            // of those emissions would start fetching for rows already gone from the screen.
            .debounce(VISIBLE_ROWS_DEBOUNCE_MS)
            .distinctUntilChanged()
            .collect { currentOnRowsVisible(it) }
    }

    LaunchedEffect(canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo
                .visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - LOAD_MORE_THRESHOLD
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadMore()
        }
    }

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

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShimmerList(isRedesignEnabled: Boolean) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(SHIMMER_ITEM_COUNT) {
            if (isRedesignEnabled) ContentListPlaceholderRow() else LegacyContentListPlaceholderRow()
        }
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_generic),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyContent(emptyMessageResId: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(emptyMessageResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** How long the visible-row set must settle before view counts are fetched for it. */
private const val VISIBLE_ROWS_DEBOUNCE_MS = 300L

private const val LOAD_MORE_THRESHOLD = 5
private const val SHIMMER_ITEM_COUNT = 8

/**
 * How long a reveal waits for the refresh carrying the page to land before giving up.
 */
private const val REVEAL_TIMEOUT_MS = 15_000L
