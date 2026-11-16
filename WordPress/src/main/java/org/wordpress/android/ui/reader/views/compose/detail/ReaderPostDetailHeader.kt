package org.wordpress.android.ui.reader.views.compose.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.toComposeFontFamily
import org.wordpress.android.ui.reader.views.compose.postcard.CircleAvatar
import org.wordpress.android.ui.reader.views.compose.postlist.FollowButton
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderAction
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderUiState
import org.wordpress.android.util.image.ImageType

private const val TITLE_MAX_LINES = 5
private const val EXCERPT_MAX_LINES = 5
private const val TITLE_FONT_SIZE_SP = 24
private const val HIGH_EMPHASIS_ALPHA = 0.87f
private const val MEDIUM_EMPHASIS_ALPHA = 0.6f

/**
 * The top of the post detail, above the post's body: the blog, title, author, featured image and excerpt
 */
@Composable
fun ReaderPostDetailHeader(
    state: ReaderPostDetailsHeaderUiState,
    readingPreferences: ReaderReadingPreferences,
    onHeaderAction: (ReaderPostDetailsHeaderAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BlogName(state.blogSectionUiState)
        state.title?.let { title ->
            Text(
                text = uiStringText(title),
                modifier = Modifier.padding(top = Margin.ExtraLarge.value),
                color = readingThemeTextColor(readingPreferences),
                fontFamily = readingPreferences.fontFamily.toComposeFontFamily(),
                fontSize = (TITLE_FONT_SIZE_SP * readingPreferences.fontSize.multiplier).sp,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AuthorRow(state, onHeaderAction)
        state.featuredImageUiState?.let { featuredImage ->
            AsyncImage(
                model = featuredImage.url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(top = Margin.Medium.value)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.reader_featured_image_corner_radius_new)))
                    .clickable {
                        onHeaderAction(
                            ReaderPostDetailsHeaderAction.FeaturedImageClicked(featuredImage.blogId, featuredImage.url)
                        )
                    },
            )
        }
        state.excerpt?.let { Excerpt(uiStringText(it)) }
        HorizontalDivider(
            modifier = Modifier.padding(top = Margin.Medium.value),
            thickness = 1.dp,
            color = colorResource(R.color.reader_separator),
        )
    }
}

/**
 * The title takes the reading theme's text color, as the WebView body does. The system theme's text color is the
 * app's own.
 */
@Composable
private fun readingThemeTextColor(readingPreferences: ReaderReadingPreferences): Color {
    val context = LocalContext.current
    return remember(readingPreferences.theme) {
        Color(ReaderReadingPreferences.ThemeValues.from(context, readingPreferences.theme).intTextColor)
    }
}

@Composable
private fun BlogName(blogSection: ReaderBlogSectionUiState) {
    val blogName = uiStringText(blogSection.blogName)
    if (blogName.isEmpty()) return
    val onClick = blogSection.blogSectionClickData?.onBlogSectionClicked
    Text(
        text = blogName,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = HIGH_EMPHASIS_ALPHA),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AuthorRow(
    state: ReaderPostDetailsHeaderUiState,
    onHeaderAction: (ReaderPostDetailsHeaderAction) -> Unit,
) {
    val blogSection = state.blogSectionUiState
    val followState = state.followButtonUiState
    Row(
        modifier = Modifier.padding(top = Margin.Medium.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onHeaderAction(ReaderPostDetailsHeaderAction.AuthorClicked) }
                .padding(vertical = Margin.Small.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatars(blogSection)
            Column {
                state.authorName?.takeIf { it.isNotEmpty() }?.let { SecondaryText(it, isPrimary = true) }
                blogSection.dateLine.takeIf { it.isNotEmpty() }?.let { SecondaryText(it) }
                state.readingTime?.let { SecondaryText(uiStringText(it)) }
            }
        }
        if (followState.isVisible) {
            Box(modifier = Modifier.padding(start = Margin.Medium.value)) {
                FollowButton(
                    isFollowed = followState.isFollowed,
                    isInProgress = followState.isFollowActionRunning,
                    onClick = { followState.onFollowButtonClicked?.invoke() },
                )
            }
        }
    }
}

/**
 * The blog's avatar, with the author's overlapping its bottom corner when it's shown
 */
@Composable
private fun Avatars(blogSection: ReaderBlogSectionUiState) {
    val blogAvatarUrl = blogSection.avatarOrBlavatarUrl
    val authorAvatarUrl = blogSection.authorAvatarUrl.takeIf { blogSection.isAuthorAvatarVisible }
    if (blogAvatarUrl == null && authorAvatarUrl == null) return
    Box(modifier = Modifier.padding(end = Margin.Large.value)) {
        blogAvatarUrl?.let { CircleAvatar(it, blogSection.blavatarType, size = 40.dp) }
        authorAvatarUrl?.let {
            CircleAvatar(
                url = it,
                imageType = ImageType.BLAVATAR_CIRCULAR,
                size = 22.dp,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun SecondaryText(text: String, isPrimary: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isPrimary) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The excerpt is cut to five lines, with "View more" to show the rest when it doesn't fit
 */
@Composable
private fun Excerpt(text: String) {
    var isExpanded by rememberSaveable(text) { mutableStateOf(false) }
    var isTruncated by rememberSaveable(text) { mutableStateOf(false) }
    Text(
        text = text,
        modifier = Modifier.padding(top = Margin.Medium.value),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
        maxLines = if (isExpanded) Int.MAX_VALUE else EXCERPT_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!isExpanded) isTruncated = it.hasVisualOverflow },
    )
    if (isTruncated && !isExpanded) {
        Text(
            text = stringResource(R.string.reader_excerpt_view_more),
            modifier = Modifier
                .clickable { isExpanded = true }
                .padding(vertical = Margin.Small.value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
