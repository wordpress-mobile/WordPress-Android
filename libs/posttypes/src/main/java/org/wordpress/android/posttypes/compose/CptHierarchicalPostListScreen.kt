package org.wordpress.android.posttypes.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.posttypes.CptHierarchicalPostItem
import org.wordpress.android.posttypes.CptHierarchicalPostListUiState
import org.wordpress.android.posttypes.R
import uniffi.wp_mobile.PostItemState

private const val INDENT_WIDTH_DP = 16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CptHierarchicalPostListScreen(
    uiState: CptHierarchicalPostListUiState,
    onBackClick: () -> Unit,
    onPostClick: (CptHierarchicalPostItem) -> Unit,
    onRefreshClick: () -> Unit,
    onLoadMoreClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.postTypeLabel) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cpt_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !uiState.isSyncing
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cpt_refresh)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(uiState.posts) { post ->
                CptHierarchicalPostItemRow(
                    post = post,
                    onClick = { onPostClick(post) }
                )
                HorizontalDivider()
            }

            item {
                LoadMoreSection(
                    uiState = uiState,
                    onLoadMoreClick = onLoadMoreClick
                )
            }
        }
    }
}

@Composable
private fun CptHierarchicalPostItemRow(
    post: CptHierarchicalPostItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indent based on hierarchy level
        if (post.indent > 0) {
            Spacer(modifier = Modifier.width((INDENT_WIDTH_DP * post.indent).dp))
        }

        // State indicator
        PostItemStateIndicator(state = post.itemState)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (post.isLoading) {
                Text(
                    text = "Loading page ${post.id}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (post.errorMessage != null) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Error: ${post.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.status.isNotEmpty()) {
                    Text(
                        text = post.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ID badge
        Text(
            text = "#${post.id}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PostItemStateIndicator(state: PostItemState) {
    val color = when (state) {
        is PostItemState.Missing -> Color.Gray
        is PostItemState.Fetching -> Color.Blue
        is PostItemState.FetchingWithData -> Color.Blue
        is PostItemState.Fresh -> Color.Green
        is PostItemState.Stale -> Color.Yellow
        is PostItemState.Failed -> Color.Red
        is PostItemState.FailedWithData -> Color.Red
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LoadMoreSection(
    uiState: CptHierarchicalPostListUiState,
    onLoadMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            uiState.isSyncing -> {
                CircularProgressIndicator()
                Text(
                    text = "Syncing...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            uiState.hasMorePages && uiState.currentPage > 0L -> {
                Button(onClick = onLoadMoreClick) {
                    Text("Load More")
                }
            }
            uiState.currentPage > 0L -> {
                Text(
                    text = "All pages loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            uiState.posts.isEmpty() && !uiState.isSyncing -> {
                Text(
                    text = "Tap refresh to load pages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Show error if any
        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
