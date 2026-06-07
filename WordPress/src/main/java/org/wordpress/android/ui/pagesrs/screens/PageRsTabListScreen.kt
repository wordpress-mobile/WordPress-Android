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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsUiModel
import org.wordpress.android.ui.pagesrs.PageTabUiState
import org.wordpress.android.ui.postsrs.screens.PlaceholderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageRsTabListScreen(
    state: PageTabUiState,
    emptyMessageResId: Int,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSearchIdle: Boolean = false,
    isSearching: Boolean = false
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
            state.isLoading -> ShimmerList()
            state.error != null -> {
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
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                onLoadMore = onLoadMore,
                onPageClick = onPageClick
            )
        }
    }
}

@Composable
private fun PageListContent(
    pages: List<PageRsUiModel>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onPageClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()

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
            key = { it.remotePageId }
        ) { page ->
            PageRsListItem(
                page = page,
                onClick = { onPageClick(page.remotePageId) },
                modifier = Modifier.animateItem()
            )
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
private fun ShimmerList() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(SHIMMER_ITEM_COUNT) {
            PlaceholderItem()
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

private const val LOAD_MORE_THRESHOLD = 5
private const val SHIMMER_ITEM_COUNT = 8
