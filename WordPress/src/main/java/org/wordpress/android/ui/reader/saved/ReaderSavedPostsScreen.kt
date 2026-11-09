package org.wordpress.android.ui.reader.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.ui.reader.views.compose.postlist.ReaderPostListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_mobile.ReaderSavedListItem

private val THUMBNAIL_SIZE = 72.dp
private const val TITLE_MAX_LINES = 2
private const val EXCERPT_MAX_LINES = 3

class ReaderSavedPostsActions(
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onItemClicked: (ReaderSavedListItem) -> Unit,
    val onUnsaveClicked: (ReaderSavedListItem) -> Unit,
    val onEmptyStateButtonClicked: (ReaderPostListEmptyButton) -> Unit,
)

@Composable
fun ReaderSavedPostsScreen(
    uiState: ReaderSavedPostsViewModel.UiState,
    listState: LazyListState,
    actions: ReaderSavedPostsActions,
) {
    ContentListPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = actions.onRefresh,
        // outermost, so the host's app bar collapses and expands before the list scrolls
        modifier = Modifier
            .nestedScroll(rememberNestedScrollInteropConnection())
            .background(colorResource(R.color.reader_post_list_background))
    ) {
        val items = uiState.items
        // the card margin is 0 on phones, where the post cards pad themselves
        val margin = dimensionResource(R.dimen.reader_card_margin) + Margin.ExtraLarge.value
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items = items, key = { _, item -> item.key.stableKey() }) { index, item ->
                Column(modifier = Modifier.padding(horizontal = margin)) {
                    SavedPostRow(item, actions)
                    if (index < items.lastIndex) {
                        HorizontalDivider(color = colorResource(R.color.on_surface_divider))
                    }
                }
            }
            emptyState(uiState)?.let { emptyState ->
                item(key = "empty") {
                    ReaderPostListEmptyState(
                        emptyState = emptyState,
                        onButtonClick = actions.onEmptyStateButtonClicked,
                        isTopAligned = false,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            }
            if (uiState.isLoadingMore) {
                contentListLoadingMoreItem()
            }
        }
        LoadMoreOnScrollToEnd(
            listState = listState,
            itemCount = items.size,
            canLoadMore = uiState.hasMore,
            onLoadMore = actions.onLoadMore
        )
    }
}

private fun emptyState(uiState: ReaderSavedPostsViewModel.UiState): ReaderPostListEmptyState? = when {
    uiState.items.isNotEmpty() || !uiState.isLoaded -> null
    uiState.isRefreshing -> ReaderPostListEmptyState.Updating
    uiState.isOffline -> ReaderPostListEmptyState.NoConnection
    uiState.loadFailed -> ReaderPostListEmptyState.RequestFailed(isSearch = false)
    else -> ReaderPostListEmptyState.Bookmarks
}

@Composable
private fun SavedPostRow(item: ReaderSavedListItem, actions: ReaderSavedPostsActions) {
    val post = item.post
    val title = post?.title?.takeIf { it.isNotBlank() } ?: post?.identity?.url ?: item.key.displayUrl()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onItemClicked(item) }
            .padding(top = Margin.Large.value)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Margin.Large.value)) {
            SavedPostText(item, title, modifier = Modifier.weight(1f))
            post?.featuredImage?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .placeholder(R.color.placeholder)
                        .error(R.color.placeholder)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(THUMBNAIL_SIZE)
                        .clip(RoundedCornerShape(Margin.Small.value))
                )
            }
        }
        // below the image as well as the text, so the unsave button lines up on every row
        SavedPostFooter(item, actions)
    }
}

@Composable
private fun SavedPostText(item: ReaderSavedListItem, title: String?, modifier: Modifier) {
    val post = item.post
    Column(modifier = modifier) {
        post?.siteName?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = HtmlUtils.fastStripHtml(it),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = HtmlUtils.fastStripHtml(title.orEmpty()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis
        )
        post?.excerpt?.let { HtmlUtils.fastStripHtml(it).trim() }?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = Margin.Small.value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = EXCERPT_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavedPostFooter(item: ReaderSavedListItem, actions: ReaderSavedPostsActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val details = listOfNotNull(
            (item.post?.date ?: item.dateSaved)?.let { DateTimeUtils.javaDateToTimeSpan(it, LocalContext.current) },
            if (item.isLocalOnly) stringResource(R.string.reader_saved_post_local_only) else null
        ).joinToString(" · ")
        Text(
            text = details,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = { actions.onUnsaveClicked(item) }) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_fill_new_24dp),
                contentDescription = stringResource(R.string.reader_remove_bookmark),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun ReaderSavedKey.stableKey(): String = when (this) {
    is ReaderSavedKey.Post -> "post:$blogId:$postId"
    is ReaderSavedKey.FeedItem -> "feed-item:$feedItemId"
    is ReaderSavedKey.Url -> "url:$url"
}

private fun ReaderSavedKey.displayUrl(): String? = (this as? ReaderSavedKey.Url)?.url
