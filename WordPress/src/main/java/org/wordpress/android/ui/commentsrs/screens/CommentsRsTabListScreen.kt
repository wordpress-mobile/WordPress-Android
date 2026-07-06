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
import androidx.compose.foundation.lazy.rememberLazyListState
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
    state: CommentsTabUiState,
    emptyMessageResId: Int,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCommentClick: (Long) -> Unit,
    modifier: Modifier = Modifier
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
            state.isLoading -> ShimmerList()
            state.error != null && state.comments.isEmpty() -> ErrorContent(
                error = state.error,
                onRetry = onRefresh
            )
            state.comments.isEmpty() && !state.isRefreshing -> EmptyContent(
                emptyMessageResId = emptyMessageResId
            )
            else -> CommentListContent(
                comments = state.comments,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                onLoadMore = onLoadMore,
                onCommentClick = onCommentClick
            )
        }
    }
}

@Composable
private fun CommentListContent(
    comments: List<CommentRsUiModel>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onCommentClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()

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
                onClick = { onCommentClick(comment.remoteCommentId) },
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
