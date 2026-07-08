package org.wordpress.android.ui.commentsrs.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.R
import org.wordpress.android.ui.commentsrs.CommentRsUiModel
import org.wordpress.android.ui.commentsrs.CommentsTabUiState
import org.wordpress.android.ui.compose.components.ShimmerBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsRsTabListScreen(
    /** Null when the tab isn't initialized: first composition, or cleared awaiting a search. */
    state: CommentsTabUiState?,
    emptyMessageResId: Int,
    selectedIds: Set<Long>,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCommentClick: (Long) -> Unit,
    onCommentLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    isQuerySearchable: Boolean = false
) {
    // While searching, a missing tab state means "cleared, waiting for the debounced fetch"
    // (initTab inserts an isLoading state the moment it actually fetches) — show the same blank
    // idle screen as a below-minimum query rather than flashing the animated shimmer on every
    // keystroke. Outside search, a missing state is the pre-init first composition: shimmer.
    val isSearchIdle = isSearchActive && (!isQuerySearchable || state == null)
    val isSearching = isSearchActive && isQuerySearchable
    val tabState = state ?: CommentsTabUiState(isLoading = true)
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = tabState.isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = tabState.isRefreshing,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        when {
            // Search is open but the query is still below the minimum length: show nothing
            // rather than a misleading "no comments" state.
            isSearchIdle -> Box(Modifier.fillMaxSize())
            tabState.isLoading -> ShimmerList()
            tabState.error != null && tabState.comments.isEmpty() -> ErrorContent(
                error = tabState.error,
                onRetry = onRefresh
            )
            tabState.comments.isEmpty() && !tabState.isRefreshing -> EmptyContent(
                emptyMessageResId = if (isSearching) {
                    R.string.comments_rs_search_nothing_found
                } else {
                    emptyMessageResId
                }
            )
            else -> CommentListContent(
                comments = tabState.comments,
                selectedIds = selectedIds,
                listState = listState,
                isLoadingMore = tabState.isLoadingMore,
                canLoadMore = tabState.canLoadMore,
                onLoadMore = onLoadMore,
                onCommentClick = onCommentClick,
                onCommentLongClick = onCommentLongClick
            )
        }
    }
}

@Composable
private fun CommentListContent(
    comments: List<CommentRsUiModel>,
    selectedIds: Set<Long>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onCommentClick: (Long) -> Unit,
    onCommentLongClick: (Long) -> Unit
) {
    // Also keyed on the list size: a refresh that truncates the list (or an appended page) restarts
    // the flow, so a `true` latched by distinctUntilChanged before the change can't suppress the
    // re-fire needed to resume paging. The ViewModel's busy/cursor guards make re-fires safe.
    LaunchedEffect(canLoadMore, comments.size) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            // total > 0 keeps the pre-layout pass (empty layoutInfo, 0 >= -threshold) from
            // triggering a load of the next page before the user has scrolled at all.
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - LOAD_MORE_THRESHOLD
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = comments,
            key = { it.remoteCommentId }
        ) { comment ->
            CommentsRsListItem(
                comment = comment,
                isSelected = comment.remoteCommentId in selectedIds,
                onClick = { onCommentClick(comment.remoteCommentId) },
                onLongClick = { onCommentLongClick(comment.remoteCommentId) },
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
private fun PlaceholderItem() {
    Row(modifier = Modifier.padding(16.dp)) {
        ShimmerBox(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .size(width = 180.dp, height = 16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .size(width = 260.dp, height = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
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
