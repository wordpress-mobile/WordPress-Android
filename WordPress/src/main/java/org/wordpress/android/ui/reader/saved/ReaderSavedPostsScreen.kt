package org.wordpress.android.ui.reader.saved

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.ui.reader.views.compose.postlist.ReaderPostListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.WPAvatarUtils
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_mobile.ReaderSavedListItem
import java.util.Calendar
import java.util.Date

private val THUMBNAIL_SIZE = 64.dp
private const val TITLE_MAX_LINES = 3
private const val EXCERPT_MAX_LINES = 2
private const val FOOTNOTE_MAX_LINES = 2
private const val MAX_TAGS = 3
private val SITE_ICON_SIZE = 20.dp

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

/** A saved post. Swipe it away, or long-press it, to remove it, as on iOS. */
@Composable
private fun SavedPostRow(item: ReaderSavedListItem, actions: ReaderSavedPostsActions) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { RemoveBackground() },
        onDismiss = {
            actions.onUnsaveClicked(item)
            // the row leaves the list once the unsave lands; if it fails, it's back
            scope.launch { dismissState.reset() }
        }
    ) {
        SavedPostContent(item, actions)
    }
}

@Composable
private fun SavedPostContent(item: ReaderSavedListItem, actions: ReaderSavedPostsActions) {
    val post = item.post
    var isMenuShown by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.reader_post_list_background))
                .combinedClickable(
                    onClick = { actions.onItemClicked(item) },
                    onLongClick = { isMenuShown = true },
                    onLongClickLabel = stringResource(R.string.remove)
                )
                .padding(vertical = Margin.ExtraLarge.value),
            horizontalArrangement = Arrangement.spacedBy(Margin.Large.value)
        ) {
            SavedPostText(item, modifier = Modifier.weight(1f))
            // the image the post's Reader card shows, else its featured image
            (post?.displayImageUrl ?: post?.featuredImage)?.takeIf { it.isNotBlank() }?.let { imageUrl ->
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
                        .clip(RoundedCornerShape(Margin.Medium.value))
                )
            }
        }
        DropdownMenu(expanded = isMenuShown, onDismissRequest = { isMenuShown = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove)) },
                onClick = {
                    isMenuShown = false
                    actions.onUnsaveClicked(item)
                }
            )
        }
    }
}

@Composable
private fun RemoveBackground() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = Margin.ExtraLarge.value),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.remove),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onError
        )
    }
}

@Composable
private fun SavedPostText(item: ReaderSavedListItem, modifier: Modifier) {
    val post = item.post
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Margin.Small.value)) {
        post?.siteName?.takeIf { it.isNotBlank() }?.let { siteName ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                post.siteIconUrl?.takeIf { it.isNotBlank() }?.let { SiteIcon(it) }
                Text(
                    text = HtmlUtils.fastStripHtml(siteName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = HtmlUtils.fastStripHtml(item.title()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis
        )
        post?.excerpt?.let { HtmlUtils.fastStripHtml(it).trim() }?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = EXCERPT_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
        post?.tags.orEmpty().take(MAX_TAGS).takeIf { it.isNotEmpty() }?.let { tags ->
            Text(
                text = tags.joinToString(" ") { "#" + HtmlUtils.fastStripHtml(it.displayName ?: it.name) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        footnote(item)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = FOOTNOTE_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The site's icon, or an RSS feed's image, sized like the site avatar on Reader post cards. */
@Composable
private fun SiteIcon(url: String) {
    val sizePx = with(LocalDensity.current) { SITE_ICON_SIZE.roundToPx() }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(WPAvatarUtils.rewriteAvatarUrl(url, sizePx))
            .placeholder(R.color.placeholder)
            .error(R.color.placeholder)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .padding(end = Margin.Medium.value)
            .size(SITE_ICON_SIZE)
            .clip(CircleShape)
    )
}

/** The author, the number of comments and when the post was saved. */
@Composable
private fun footnote(item: ReaderSavedListItem): String? {
    val context = LocalContext.current
    // the server sends 0 for RSS posts and pages saved by URL, which can't have comments
    val commentCount = item.post?.commentCount?.toLong()?.takeIf { it > 0 }
    val parts = listOfNotNull(
        item.post?.authorName?.takeIf { it.isNotBlank() },
        commentCount?.let {
            val format = if (it == 1L) {
                R.string.content_list_comment_count_singular
            } else {
                R.string.content_list_comment_count_plural
            }
            stringResource(format, FormatUtils.formatInt(it.toInt()))
        },
        item.dateSaved?.let { stringResource(R.string.reader_saved_post_saved_date, formatSavedDate(context, it)) },
        if (item.isLocalOnly) stringResource(R.string.reader_saved_post_local_only) else null
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** Leaves out the year for dates in this year, so the footnote fits on one line more often. */
private fun formatSavedDate(context: Context, date: Date): String {
    val sameYear = Calendar.getInstance().get(Calendar.YEAR) ==
        Calendar.getInstance().apply { time = date }.get(Calendar.YEAR)
    val yearFlag = if (sameYear) DateUtils.FORMAT_NO_YEAR else DateUtils.FORMAT_SHOW_YEAR
    return DateUtils.formatDateTime(
        context,
        date.time,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or yearFlag
    )
}

/** The post's title, or for a page saved by URL, its host. */
private fun ReaderSavedListItem.title(): String {
    post?.title?.takeIf { it.isNotBlank() }?.let { return it }
    val url = post?.identity?.url ?: (key as? ReaderSavedKey.Url)?.url ?: return ""
    return Uri.parse(url).host ?: url
}

private fun ReaderSavedKey.stableKey(): String = when (this) {
    is ReaderSavedKey.Post -> "post:$blogId:$postId"
    is ReaderSavedKey.FeedItem -> "feed-item:$feedItemId"
    is ReaderSavedKey.Url -> "url:$url"
}

