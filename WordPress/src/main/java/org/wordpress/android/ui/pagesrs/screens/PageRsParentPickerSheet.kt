package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsParentPickerState

/**
 * Bottom sheet for choosing a page's parent: a "Top level" entry followed by the eligible
 * published pages. The list is paged and the search field queries the server, so parents that
 * aren't loaded into the main list yet can still be found. The current parent shows a check mark.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageRsParentPickerSheet(
    state: PageRsParentPickerState,
    onParentSelected: (Long) -> Unit,
    onParentSearchChanged: (String) -> Unit,
    onLoadMoreParents: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.canLoadMore) {
        if (!state.canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - LOAD_MORE_THRESHOLD
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadMoreParents()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Pin the sheet to a fixed fraction of the screen so it doesn't resize as the content
        // region swaps between the spinner, the "no results" message and the (variable-length)
        // candidate list while searching.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION)
        ) {
            Text(
                text = stringResource(R.string.set_parent),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            TextField(
                value = state.query,
                onValueChange = onParentSearchChanged,
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isLoading && state.candidates.isEmpty() ->
                        CenteredMessage { CircularProgressIndicator() }
                    state.error != null && state.candidates.isEmpty() ->
                        CenteredMessage { MessageText(state.error) }
                    state.candidates.isEmpty() && state.query.isNotBlank() ->
                        CenteredMessage {
                            MessageText(stringResource(R.string.pages_empty_search_result))
                        }
                    else -> CandidateList(state, listState, onParentSelected)
                }
            }
        }
    }
}

@Composable
private fun CandidateList(
    state: PageRsParentPickerState,
    listState: LazyListState,
    onParentSelected: (Long) -> Unit
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (state.query.isBlank()) {
            item(key = "top_level") {
                ParentCandidateRow(
                    title = stringResource(R.string.top_level),
                    isSelected = state.currentParentId == 0L,
                    onClick = { onParentSelected(0L) }
                )
            }
        }
        items(items = state.candidates, key = { it.id }) { candidate ->
            ParentCandidateRow(
                title = candidate.title.ifBlank {
                    stringResource(R.string.untitled_in_parentheses)
                },
                isSelected = state.currentParentId == candidate.id,
                onClick = { onParentSelected(candidate.id) }
            )
        }
        if (state.isLoadingMore) {
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
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ParentCandidateRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

private const val LOAD_MORE_THRESHOLD = 5

// Fraction of the screen height the sheet occupies, kept fixed so it doesn't resize while searching.
private const val SHEET_HEIGHT_FRACTION = 0.75f
