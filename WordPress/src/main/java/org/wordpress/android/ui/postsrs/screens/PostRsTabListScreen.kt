package org.wordpress.android.ui.postsrs.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.PostRsMenuAction
import org.wordpress.android.ui.postsrs.PostRsUiModel
import org.wordpress.android.ui.rs.RsTabUiState
import org.wordpress.android.ui.rs.contentlist.ContentDateGroup
import org.wordpress.android.ui.rs.contentlist.ContentDisplayState
import org.wordpress.android.ui.rs.contentlist.ContentListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListErrorState
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentDateGrouper
import org.wordpress.android.ui.rs.contentlist.ContentListGroupHeader
import org.wordpress.android.ui.rs.contentlist.ContentListHeroRow
import org.wordpress.android.ui.rs.contentlist.ContentListPlaceholderRow
import org.wordpress.android.ui.rs.contentlist.ContentListShimmer
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.ContentListRow
import org.wordpress.android.ui.postsrs.toContentListRowUiState
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.ReportVisibleRows
import org.wordpress.android.ui.rs.contentlist.RevealRow
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem
import org.wordpress.android.ui.rs.contentlist.toContentListRowActions

@Composable
fun PostRsTabListScreen(
    state: RsTabUiState<PostRsUiModel>,
    emptyMessageResId: Int,
    revealPostId: Long?,
    onRevealHandled: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    onCreatePost: () -> Unit,
    onRowsVisible: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    isSearchIdle: Boolean = false,
    isSearching: Boolean = false,
    isRedesignEnabled: Boolean = false,
    showDateGroups: Boolean = true,
    density: ContentListDensity = ContentListDensity.COMFORTABLE
) {
    ContentListPullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        when {
            isSearchIdle -> Box(Modifier.fillMaxSize())
            state.isLoading -> ContentListShimmer(isRedesignEnabled)
            state.error != null && state.items.isEmpty() -> FadeInOnAppear {
                ContentListErrorState(
                    error = state.error,
                    onRetry = if (state.isAuthError) null else onRefresh
                )
            }
            state.items.isEmpty() && !state.isRefreshing -> FadeInOnAppear {
                ContentListEmptyState(
                    messageResId = if (isSearching) {
                        R.string.post_list_search_nothing_found
                    } else {
                        emptyMessageResId
                    },
                    // No "write a post" shortcut on an empty search result.
                    actionLabelResId = R.string.posts_empty_list_button.takeIf { !isSearching },
                    onAction = if (isSearching) null else onCreatePost
                )
            }
            else -> PostListContent(
                posts = state.items,
                revealPostId = revealPostId,
                onRevealHandled = onRevealHandled,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                onLoadMore = onLoadMore,
                onPostClick = onPostClick,
                onPostMenuAction = onPostMenuAction,
                onRowsVisible = onRowsVisible,
                isRedesignEnabled = isRedesignEnabled,
                // Date buckets are computed against "now", so a list of future-dated posts would
                // land under "This week" wholesale. The Scheduled tab opts out instead.
                showDateGroups = showDateGroups && !isSearching,
                density = density
            )
        }
    }
}

@Composable
private fun PostListContent(
    posts: List<PostRsUiModel>,
    revealPostId: Long?,
    onRevealHandled: () -> Unit,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    onRowsVisible: (List<Long>) -> Unit,
    isRedesignEnabled: Boolean,
    showDateGroups: Boolean,
    density: ContentListDensity
) {
    val listState = rememberLazyListState()

    val entries = remember(posts, isRedesignEnabled, showDateGroups, density) {
        if (isRedesignEnabled) {
            buildEntries(posts, showDateGroups, density)
        } else {
            posts.map { PostListEntry.NonContent(it) }
        }
    }

    // Indexes the rendered entries, since group headers are list items too.
    RevealRow(revealPostId, listState, onRevealHandled) { id ->
        entries.indexOfFirst { it.postId == id }
    }

    // Post entries key on their remote id; group headers key on a String and drop out.
    ReportVisibleRows(listState, enabled = isRedesignEnabled, onRowsVisible = onRowsVisible) {
        listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Long }
    }

    LoadMoreOnScrollToEnd(listState, posts.size, canLoadMore, onLoadMore)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = entries,
            key = { it.key },
            contentType = { it::class }
        ) { entry ->
            when (entry) {
                is PostListEntry.Header -> ContentListGroupHeader(
                    group = entry.group,
                    modifier = Modifier.animateItem()
                )
                is PostListEntry.NonContent ->
                    // The redesigned placeholder only belongs to the redesigned list; with the flag
                    // off every row, placeholder included, goes through the pre-redesign item.
                    if (isRedesignEnabled &&
                        entry.post.displayState == ContentDisplayState.PLACEHOLDER
                    ) {
                        ContentListPlaceholderRow(modifier = Modifier.animateItem())
                    } else {
                        // The pre-redesign row still owns the error presentation.
                        PostRsListItem(
                            post = entry.post,
                            onClick = { onPostClick(entry.post.remoteId) },
                            onMenuAction = { action ->
                                onPostMenuAction(entry.post.remoteId, action)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                is PostListEntry.Row -> RedesignedRow(
                    entry = entry,
                    onPostClick = onPostClick,
                    onPostMenuAction = onPostMenuAction,
                    density = density,
                    modifier = Modifier.animateItem()
                )
            }
        }

        if (isLoadingMore) contentListLoadingMoreItem()
    }
}

/**
 * Fades its content in the first time it appears.
 *
 * Driven by a [MutableTransitionState] rather than a plain `visible = true`: the states this wraps
 * only enter the composition once the list has turned out to be empty or failed, so a plain flag
 * would already sit at its target on first composition and the fade would be skipped entirely.
 */
@Composable
private fun FadeInOnAppear(content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = true

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(STATE_FADE_MS))
    ) {
        content()
    }
}

/**
 * One rendered item. Group headers occupy list positions of their own, so the list is built from
 * entries rather than straight from the posts.
 */
private sealed interface PostListEntry {
    val key: Any

    /** Remote id of the post this entry shows, or null for a header. */
    val postId: Long?

    /**
     * [ordinal] disambiguates the key. A sticky post floats to the top regardless of its date, so
     * the same bucket can legitimately open twice in one list - and a repeated LazyColumn key
     * throws rather than merely looking odd.
     */
    data class Header(val group: ContentDateGroup, val ordinal: Int) : PostListEntry {
        override val key get() = "header_${ordinal}_${group.key}"
        override val postId: Long? get() = null
    }

    data class Row(val post: PostRsUiModel, val isHero: Boolean) : PostListEntry {
        override val key get() = post.remoteId
        override val postId get() = post.remoteId
    }

    /**
     * A row that is not a loaded post: still loading, or failed. Which of the two it is comes from
     * the post's own [PostRsUiModel.displayState] at render time.
     */
    data class NonContent(val post: PostRsUiModel) : PostListEntry {
        override val key get() = post.remoteId
        override val postId get() = post.remoteId
    }
}

private fun buildEntries(
    posts: List<PostRsUiModel>,
    showDateGroups: Boolean,
    density: ContentListDensity
): List<PostListEntry> {
    val entries = mutableListOf<PostListEntry>()
    var currentGroupKey: String? = null
    var headerCount = 0

    posts.forEach { post ->
        if (post.displayState == ContentDisplayState.PLACEHOLDER ||
            post.displayState == ContentDisplayState.ERROR
        ) {
            entries += PostListEntry.NonContent(post)
            return@forEach
        }

        if (showDateGroups && post.dateGmtMillis > 0L) {
            val group = ContentDateGrouper.groupOf(post.dateGmtMillis)
            if (group.key != currentGroupKey) {
                entries += PostListEntry.Header(group, headerCount++)
                currentGroupKey = group.key
            }
        }

        // Every post with a featured image gets the hero treatment, not just the newest. Keyed off
        // the featured image *id*, which is present as soon as the post loads, rather than the
        // resolved URL, which arrives a network call later - keying off the URL would pop rows from
        // compact to hero as their images resolved.
        // An image-led card with no image is just a compact card with the wrong padding, so a
        // post whose media could not be resolved falls back to the compact shape.
        val isHero = !density.isCondensed &&
            showDateGroups &&
            post.featuredImageId != 0L &&
            !post.isFeaturedImageUnresolvable
        entries += PostListEntry.Row(post, isHero)
    }
    return entries
}

@Composable
private fun RedesignedRow(
    entry: PostListEntry.Row,
    onPostClick: (Long) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    density: ContentListDensity,
    modifier: Modifier = Modifier
) {
    val post = entry.post
    val state = post.toContentListRowUiState()
    val onClick = { onPostClick(post.remoteId) }
    // A trashed post's row tap offers to restore it rather than opening the editor.
    val actions = post.actions.toContentListRowActions(onEdit = if (post.isTrashed) null else onClick) { action ->
        onPostMenuAction(post.remoteId, action)
    }

    // The two shapes are different layouts, not one layout resized, so the swap is crossfaded
    // rather than left to snap. Only the lead row ever takes this branch; the rest change size
    // only, which the card animates itself.
    AnimatedContent(
        targetState = entry.isHero,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
        label = "content list row density"
    ) { isHero ->
        if (isHero) {
            ContentListHeroRow(
                state = state,
                onClick = onClick,
                actions = actions
            )
        } else {
            ContentListRow(
                state = state,
                onClick = onClick,
                density = density,
                actions = actions
            )
        }
    }
}

/** Long enough to read as a fade rather than a flicker, short enough not to feel sluggish. */
private const val STATE_FADE_MS = 300
