package org.wordpress.android.ui.reader.views.compose.postlist

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyButton
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState

private const val BOOKMARK_ICON_ID = "bookmark"
private const val BOOKMARK_ICON_ALPHA = 0.6f
private const val BOOKMARK_ICON_SIZE_EM = 1.2
private val TOP_PADDING = 48.dp

/**
 * What the post list shows instead of posts
 */
@Composable
internal fun ReaderPostListEmptyState(
    emptyState: ReaderPostListEmptyState,
    onButtonClick: (ReaderPostListEmptyButton) -> Unit,
    isTopAligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val content = emptyStateContent(emptyState)
    val padding = Margin.ExtraExtraMediumLarge.value
    Column(
        modifier = modifier
            .padding(start = padding, end = padding, bottom = padding, top = if (isTopAligned) TOP_PADDING else padding)
            // a live region, so TalkBack announces it when its content changes
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isTopAligned) Arrangement.Top else Arrangement.Center
    ) {
        if (content.title.isNotEmpty()) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        content.description?.let { description ->
            val descriptionModifier = content.descriptionContentDescription?.let { label ->
                Modifier.semantics { contentDescription = label }
            } ?: Modifier
            Text(
                text = description,
                modifier = descriptionModifier.padding(top = Margin.Medium.value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                inlineContent = bookmarkInlineContent()
            )
        }
        content.button?.let { button ->
            Button(
                onClick = { onButtonClick(button) },
                modifier = Modifier.padding(top = Margin.ExtraLarge.value),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(stringResource(buttonTextRes(button)))
            }
        }
    }
}

private class EmptyStateContent(
    val title: String,
    val description: AnnotatedString? = null,
    val descriptionContentDescription: String? = null,
    val button: ReaderPostListEmptyButton? = null
)

@Suppress("CyclomaticComplexMethod")
@Composable
private fun emptyStateContent(emptyState: ReaderPostListEmptyState): EmptyStateContent = when (emptyState) {
    ReaderPostListEmptyState.SelfHostedCta -> EmptyStateContent(
        title = stringResource(R.string.reader_empty_subscriptions),
        button = ReaderPostListEmptyButton.DISCOVER
    )

    ReaderPostListEmptyState.Bookmarks -> EmptyStateContent(
        title = stringResource(R.string.reader_empty_saved_posts_title),
        description = withBookmarkIcon(stringResource(R.string.reader_empty_saved_posts_description)),
        descriptionContentDescription = stringResource(R.string.reader_empty_saved_posts_content_description),
        button = ReaderPostListEmptyButton.DISCOVER
    )

    ReaderPostListEmptyState.NoConnection -> titleOnly(R.string.reader_empty_posts_no_connection)
    is ReaderPostListEmptyState.RequestFailed -> titleOnly(
        if (emptyState.isSearch) {
            R.string.reader_empty_search_request_failed
        } else {
            R.string.reader_empty_posts_request_failed
        }
    )

    ReaderPostListEmptyState.Updating -> titleOnly(R.string.reader_empty_posts_in_tag_updating)
    ReaderPostListEmptyState.FollowedSitesNoRecentPosts -> EmptyStateContent(
        title = stringResource(R.string.reader_empty_followed_blogs_no_recent_posts_title),
        description = AnnotatedString(
            stringResource(R.string.reader_empty_followed_blogs_subscribed_no_recent_posts_description)
        ),
        button = ReaderPostListEmptyButton.DISCOVER
    )

    ReaderPostListEmptyState.NoFollowedSites -> EmptyStateContent(
        title = stringResource(R.string.reader_no_followed_blogs_title),
        description = AnnotatedString(stringResource(R.string.reader_no_followed_blogs_description)),
        button = ReaderPostListEmptyButton.DISCOVER
    )

    ReaderPostListEmptyState.NoLikedPosts -> EmptyStateContent(
        title = stringResource(R.string.reader_empty_posts_liked_title),
        description = AnnotatedString(stringResource(R.string.reader_empty_posts_liked_description)),
        button = ReaderPostListEmptyButton.FOLLOWED
    )

    ReaderPostListEmptyState.NoPostsInList -> titleOnly(R.string.reader_empty_blogs_posts_in_custom_list)
    ReaderPostListEmptyState.NoPostsWithTag -> titleOnly(R.string.reader_no_posts_with_this_tag)
    ReaderPostListEmptyState.NoPostsInBlog -> titleOnly(R.string.reader_no_posts_in_blog)
    ReaderPostListEmptyState.SearchUpdating -> EmptyStateContent(title = "")
    is ReaderPostListEmptyState.SearchNoResults -> EmptyStateContent(
        title = stringResource(R.string.reader_empty_search_title),
        description = withItalicQuery(stringResource(R.string.reader_empty_search_description), emptyState.query)
    )
}

@Composable
private fun titleOnly(@StringRes titleRes: Int) = EmptyStateContent(title = stringResource(titleRes))

@StringRes
private fun buttonTextRes(button: ReaderPostListEmptyButton) = when (button) {
    ReaderPostListEmptyButton.DISCOVER -> R.string.reader_no_followed_blogs_button_discover
    ReaderPostListEmptyButton.FOLLOWED -> R.string.reader_empty_followed_blogs_button_subscriptions
}

// replaces the %s placeholder with the bookmark icon
private fun withBookmarkIcon(text: String) = replacePlaceholder(text) { appendInlineContent(BOOKMARK_ICON_ID) }

// replaces the %s placeholder with the query in italics
private fun withItalicQuery(text: String, query: String) = replacePlaceholder(text) {
    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(query) }
}

private fun replacePlaceholder(text: String, insert: AnnotatedString.Builder.() -> Unit) = buildAnnotatedString {
    val parts = text.split("%s", limit = 2)
    append(parts[0])
    if (parts.size > 1) {
        insert()
        append(parts[1])
    }
}

@Composable
private fun bookmarkInlineContent() = mapOf(
    BOOKMARK_ICON_ID to InlineTextContent(
        Placeholder(BOOKMARK_ICON_SIZE_EM.em, BOOKMARK_ICON_SIZE_EM.em, PlaceholderVerticalAlign.TextCenter)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bookmark_grey_dark_18dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = BOOKMARK_ICON_ALPHA)
        )
    }
)
