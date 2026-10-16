package org.wordpress.android.ui.reader.views.compose.postcard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.theme.AppThemeM3WithoutBackground
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.CompactBlogSectionData
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.InteractionSectionData
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.image.ImagePlaceholderManager
import org.wordpress.android.util.image.ImageType

private const val MEDIUM_EMPHASIS_ALPHA = 0.6f
private const val HIGH_EMPHASIS_ALPHA = 0.87f
private const val TEXT_MAX_LINES = 3
private const val ACTION_MAX_WIDTH_DP = 140

// Stateless, so one instance serves every avatar
private val avatarPlaceholders = ImagePlaceholderManager()

/**
 * The Reader post card shown in the post list. Renders a [ReaderPostUiState] exactly as the
 * `reader_cardview_post_new` layout did.
 */
@Composable
fun ReaderPostCard(
    uiState: ReaderPostUiState,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onCardClick)
            .padding(
                start = Margin.ExtraMediumLarge.value,
                end = Margin.ExtraMediumLarge.value,
                top = Margin.ExtraLarge.value,
                bottom = Margin.Medium.value,
            ),
    ) {
        BlogSection(uiState.blogSection)
        ReaderPostCardMedia(uiState)
        uiState.title?.let { title ->
            Text(
                text = uiStringText(title),
                modifier = Modifier.padding(top = Margin.Medium.value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        uiState.excerpt?.let { excerpt ->
            Text(
                text = excerpt,
                // Kept out of the card's TalkBack label, as the View-based card did
                modifier = Modifier
                    .padding(top = Margin.Medium.value)
                    .clearAndSetSemantics {},
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        InteractionCounts(uiState.interactionSection)
        ActionRow(uiState)
    }
}

@Composable
private fun BlogSection(blogSection: CompactBlogSectionData) {
    val onClicked = blogSection.onClicked
    val clickModifier = if (onClicked != null) {
        Modifier.clickable { onClicked(blogSection.postId, blogSection.blogId) }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BlogSectionAvatars(blogSection)
        Text(
            text = uiStringText(blogSection.blogName),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = HIGH_EMPHASIS_ALPHA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        DotSeparator()
        SecondaryText(blogSection.dateLine)
    }
}

/**
 * The author avatar (P2 only) sits first and the blog avatar overlaps it, offset by 18dp.
 */
@Composable
private fun BlogSectionAvatars(blogSection: CompactBlogSectionData) {
    val authorAvatarUrl = blogSection.authorAvatarUrl.takeIf { blogSection.isAuthorAvatarVisible }
    val blogAvatarUrl = blogSection.avatarOrBlavatarUrl
    if (authorAvatarUrl == null && blogAvatarUrl == null) return

    Box(modifier = Modifier.padding(end = Margin.Medium.value)) {
        authorAvatarUrl?.let { CircleAvatar(it, ImageType.BLAVATAR_CIRCULAR) }
        blogAvatarUrl?.let {
            CircleAvatar(
                url = it,
                imageType = blogSection.blavatarType,
                modifier = Modifier.padding(start = if (authorAvatarUrl != null) 18.dp else 0.dp),
            )
        }
    }
}

@Composable
private fun CircleAvatar(url: String, imageType: ImageType, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(1.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .apply {
                    avatarPlaceholders.getPlaceholderResource(imageType)?.let { placeholder(it) }
                    avatarPlaceholders.getErrorResource(imageType)?.let { error(it) }
                }
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.5.dp, colorResource(R.color.reader_blog_section_avatar_internal_border), CircleShape),
        )
    }
}

@Composable
private fun InteractionCounts(interactionSection: InteractionSectionData) {
    val context = LocalContext.current
    val likeLabel = interactionSection.likeCount.takeIf { it > 0 }
        ?.let { ReaderUtils.getShortLikeLabelText(context, it) }
    val commentLabel = interactionSection.commentCount.takeIf { it > 0 }
        ?.let { ReaderUtils.getShortCommentLabelText(context, it) }
    if (likeLabel == null && commentLabel == null) return

    Row(modifier = Modifier.padding(top = Margin.Medium.value)) {
        likeLabel?.let { SecondaryText(it) }
        if (likeLabel != null && commentLabel != null) {
            DotSeparator()
        }
        commentLabel?.let { SecondaryText(it) }
    }
}

@Composable
private fun DotSeparator() {
    SecondaryText(
        text = stringResource(R.string.reader_dot_separator),
        modifier = Modifier.clearAndSetSemantics {},
    )
}

@Composable
private fun SecondaryText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ActionRow(uiState: ReaderPostUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The buttons share the weighted space, so the more button keeps its width and long labels
        // ellipsize instead of squeezing it out, as the View-based card's end constraint did
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Margin.ExtraLarge.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                uiState = uiState,
                action = uiState.reblogAction,
                labelRes = R.string.reader_view_reblog,
                iconRes = R.drawable.ic_reblog_new_24dp,
                modifier = Modifier.weight(1f, fill = false),
            )
            ActionButton(
                uiState = uiState,
                action = uiState.commentsAction,
                labelRes = R.string.comment,
                iconRes = R.drawable.ic_comment_outline_new_24dp,
                modifier = Modifier.weight(1f, fill = false),
            )
            ActionButton(
                uiState = uiState,
                action = uiState.likeAction,
                labelRes = R.string.like,
                iconRes = if (uiState.likeAction.isSelected) {
                    R.drawable.ic_like_fill_new_24dp
                } else {
                    R.drawable.ic_like_outline_new_24dp
                },
                // Only Like is capped, as in the View-based card
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = ACTION_MAX_WIDTH_DP.dp),
            )
        }
        if (uiState.moreMenuVisibility) {
            MoreMenu(uiState)
        }
    }
}

@Composable
private fun ActionButton(
    uiState: ReaderPostUiState,
    action: PrimaryAction,
    labelRes: Int,
    iconRes: Int,
    modifier: Modifier = Modifier,
) {
    if (!action.isEnabled) return
    val color = if (action.isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
    }
    val description = action.contentDescription?.let { uiStringText(it) }
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clickable { action.onClicked?.invoke(uiState.postId, uiState.blogId, action.type) }
            .semantics {
                description?.let { contentDescription = it }
                // TalkBack announces "selected" for a liked post, as it did for the View-based card
                selected = action.isSelected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = color)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The adapter re-binds the card with [ReaderPostUiState.moreMenuItems] filled in when the button is tapped, and
 * without them when the menu is dismissed, so the menu is open exactly when the items are present.
 */
@Composable
private fun MoreMenu(uiState: ReaderPostUiState) {
    Box {
        // A plain 24dp icon rather than an IconButton, whose 48dp layout size would make the row taller and
        // inset the icon from the card's edge; Compose still extends its touch target to the 48dp minimum
        Icon(
            painter = painterResource(R.drawable.ic_more_ellipsis_horizontal_squares),
            contentDescription = stringResource(R.string.show_more_desc),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = null,
                    indication = ripple(bounded = false),
                    role = Role.Button,
                    onClick = { uiState.onMoreButtonClicked(uiState) },
                ),
        )
        val items = uiState.moreMenuItems
        DropdownMenu(
            expanded = items != null,
            onDismissRequest = { uiState.onMoreDismissed(uiState) },
        ) {
            items?.forEach { item -> MoreMenuItem(uiState, item) }
        }
    }
}

@Composable
private fun MoreMenuItem(uiState: ReaderPostUiState, item: ReaderPostCardAction) {
    when (item) {
        is SecondaryAction -> {
            val context = LocalContext.current
            DropdownMenuItem(
                text = {
                    Text(text = uiStringText(item.label), color = Color(context.getColorFromAttribute(item.labelColor)))
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        tint = Color(context.getColorFromAttribute(item.iconColor)),
                    )
                },
                // The item's handler re-binds the card without the menu items, which closes the menu
                onClick = { item.onClicked(uiState.postId, uiState.blogId, item.type) },
            )
        }
        is ReaderPostCardAction.SpacerNoAction -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = Margin.Small.value))
        }
        is PrimaryAction -> Unit // never part of the more menu
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReaderPostCardPreview() {
    AppThemeM3WithoutBackground {
        ReaderPostCard(uiState = previewUiState(), onCardClick = {})
    }
}

@Suppress("MagicNumber")
private fun previewUiState(): ReaderPostUiState {
    fun action(type: ReaderPostCardActionType, isSelected: Boolean = false) =
        PrimaryAction(isEnabled = true, isSelected = isSelected, type = type)
    return ReaderPostUiState(
        source = "preview",
        postId = 1L,
        blogId = 2L,
        feedId = 3L,
        isFollowed = true,
        blogSection = CompactBlogSectionData(
            postId = 1L,
            blogId = 2L,
            dateLine = "2h",
            blogName = UiStringText("A Very Good Blog"),
            avatarOrBlavatarUrl = null,
            authorAvatarUrl = null,
            isAuthorAvatarVisible = false,
            blavatarType = ImageType.BLAVATAR_CIRCULAR,
            onClicked = { _, _ -> },
        ),
        interactionSection = InteractionSectionData(likeCount = 15, commentCount = 4),
        title = UiStringText("This is the title of this post, which runs long enough to wrap"),
        excerpt = "This is the post excerpt. Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        featuredImageUrl = null,
        fullVideoUrl = null,
        thumbnailStripSection = null,
        videoOverlayVisibility = false,
        featuredImageVisibility = false,
        moreMenuVisibility = true,
        likeAction = action(ReaderPostCardActionType.LIKE, isSelected = true),
        reblogAction = action(ReaderPostCardActionType.REBLOG),
        commentsAction = action(ReaderPostCardActionType.COMMENTS),
        onItemClicked = { _, _ -> },
        onItemRendered = {},
        onMoreButtonClicked = {},
        onMoreDismissed = {},
        onVideoOverlayClicked = { _, _ -> },
    )
}
