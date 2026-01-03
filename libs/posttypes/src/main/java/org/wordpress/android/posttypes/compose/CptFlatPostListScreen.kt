package org.wordpress.android.posttypes.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.posttypes.AuthorFilter
import org.wordpress.android.posttypes.CptFlatPostListUiState
import org.wordpress.android.posttypes.CptPostListItem
import org.wordpress.android.posttypes.PostStatusFilter
import org.wordpress.android.posttypes.R
import uniffi.wp_mobile.PostItemState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CptFlatPostListScreen(
    uiState: CptFlatPostListUiState,
    onBackClick: () -> Unit,
    onPostClick: (CptPostListItem) -> Unit,
    onFilterChange: (PostStatusFilter) -> Unit,
    onAuthorFilterChange: (AuthorFilter) -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status filter chips
            FilterChipsRow(
                currentFilter = uiState.currentFilter,
                onFilterChange = onFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Author filter dropdown
            AuthorFilterDropdown(
                currentAuthorFilter = uiState.currentAuthorFilter,
                onAuthorFilterChange = onAuthorFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
            )

            // Post list
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.posts) { post ->
                    CptPostListItemRow(
                        post = post,
                        onClick = { onPostClick(post) }
                    )
                    HorizontalDivider()
                }

                // Load more / end of list indicator
                item {
                    LoadMoreSection(
                        uiState = uiState,
                        onLoadMoreClick = onLoadMoreClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    currentFilter: PostStatusFilter,
    onFilterChange: (PostStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PostStatusFilter.entries.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

@Composable
private fun AuthorFilterDropdown(
    currentAuthorFilter: AuthorFilter,
    onAuthorFilterChange: (AuthorFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Author:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 8.dp)
        )

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(currentAuthorFilter.displayName)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AuthorFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.displayName) },
                        onClick = {
                            onAuthorFilterChange(filter)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CptPostListItemRow(
    post: CptPostListItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // State indicator
        PostItemStateIndicator(state = post.itemState)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (post.isLoading) {
                Text(
                    text = "Loading post ${post.id}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (post.errorMessage != null) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.excerpt.isNotEmpty()) {
                    Text(
                        text = post.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
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
    uiState: CptFlatPostListUiState,
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
                    text = "All posts loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            uiState.posts.isEmpty() && !uiState.isSyncing -> {
                Text(
                    text = "Tap refresh to load posts",
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
