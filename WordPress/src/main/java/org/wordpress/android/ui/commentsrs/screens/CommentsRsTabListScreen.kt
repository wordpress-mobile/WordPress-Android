package org.wordpress.android.ui.commentsrs.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.wordpress.android.R
import org.wordpress.android.ui.commentsrs.CommentRsUiModel
import org.wordpress.android.ui.commentsrs.CommentsRsListRow
import org.wordpress.android.ui.commentsrs.withDateGroups
import org.wordpress.android.ui.rs.RsTabUiState
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListErrorState
import org.wordpress.android.ui.rs.contentlist.ContentListGroupHeader
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.ContentListShimmer
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem

@Composable
fun CommentsRsTabListScreen(
    /** Null when the tab isn't initialized: first composition, or cleared awaiting a search. */
    state: RsTabUiState<CommentRsUiModel>?,
    emptyMessageResId: Int,
    selectedIds: Set<Long>,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCommentClick: (Long) -> Unit,
    onCommentLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    isQuerySearchable: Boolean = false,
    density: ContentListDensity = ContentListDensity.COMFORTABLE
) {
    // While searching, a missing tab state means "cleared, waiting for the debounced fetch"
    // (initTab inserts an isLoading state the moment it actually fetches) — show the same blank
    // idle screen as a below-minimum query rather than flashing the animated shimmer on every
    // keystroke. Outside search, a missing state is the pre-init first composition: shimmer.
    val isSearchIdle = isSearchActive && (!isQuerySearchable || state == null)
    val isSearching = isSearchActive && isQuerySearchable
    val tabState = state ?: RsTabUiState(isLoading = true)

    ContentListPullToRefreshBox(
        isRefreshing = tabState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        when {
            // Search is open but the query is still below the minimum length: show nothing
            // rather than a misleading "no comments" state.
            isSearchIdle -> Box(Modifier.fillMaxSize())
            tabState.isLoading -> ContentListShimmer { CommentsRsPlaceholderRow() }
            tabState.error != null && tabState.items.isEmpty() -> ContentListErrorState(
                error = tabState.error,
                onRetry = if (tabState.isAuthError) null else onRefresh
            )
            // isLoadingMore matters here because the Unreplied tab auto-advances: a page can
            // thread away to nothing and the next one is already on its way, so an empty list
            // mid-walk isn't an empty tab.
            tabState.items.isEmpty() && !tabState.isRefreshing && !tabState.isLoadingMore ->
                ContentListEmptyState(
                    messageResId = if (isSearching) {
                        R.string.comments_rs_search_nothing_found
                    } else {
                        emptyMessageResId
                    }
                )
            else -> CommentListContent(
                comments = tabState.items,
                selectedIds = selectedIds,
                listState = listState,
                isLoadingMore = tabState.isLoadingMore,
                canLoadMore = tabState.canLoadMore,
                onLoadMore = onLoadMore,
                onCommentClick = onCommentClick,
                onCommentLongClick = onCommentLongClick,
                density = density
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
    onCommentLongClick: (Long) -> Unit,
    density: ContentListDensity
) {
    LoadMoreOnScrollToEnd(listState, comments.size, canLoadMore, onLoadMore)

    val rows = remember(comments) { withDateGroups(comments) }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is CommentsRsListRow.GroupHeader -> row.key
                    is CommentsRsListRow.Item -> row.comment.remoteCommentId
                }
            },
            contentType = { it::class }
        ) { row ->
            when (row) {
                is CommentsRsListRow.GroupHeader -> ContentListGroupHeader(
                    group = row.group,
                    modifier = Modifier.animateItem()
                )
                is CommentsRsListRow.Item -> CommentsRsRedesignedRow(
                    comment = row.comment,
                    isSelected = row.comment.remoteCommentId in selectedIds,
                    onClick = { onCommentClick(row.comment.remoteCommentId) },
                    onLongClick = { onCommentLongClick(row.comment.remoteCommentId) },
                    modifier = Modifier.animateItem(),
                    density = density
                )
            }
        }

        if (isLoadingMore) contentListLoadingMoreItem()
    }
}
