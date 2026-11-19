package org.wordpress.android.ui.reader.views.compose.detail

import android.text.Spanned
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.comments.unified.compose.CommentHtmlBody
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.toAnnotatedString
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.FollowConversationUiState
import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.toComposeFontFamily
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.TrainOfFacesUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.compose.comments.AuthorBadge
import org.wordpress.android.ui.reader.views.compose.comments.FollowConversationActions
import org.wordpress.android.ui.reader.views.compose.postcard.CircleAvatar
import org.wordpress.android.ui.reader.views.compose.readingpreferences.ReadingPreferencesPreviewTag
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImageType

private const val RELATED_POST_MAX_LINES = 3
private const val RELATED_POST_IMAGE_RATIO = 16f / 9f

class ReaderPostDetailFooterActions(
    val onExcerptFooterClick: (String) -> Unit,
    val onLikesClick: () -> Unit,
    val onTagsExpandToggled: () -> Unit,
    val onCommentImageClick: (String) -> Unit,
    val onCommentClick: (CommentSnippetItemState.CommentState) -> Unit,
    /** Reports where a related posts section is, so the fragment can tell when it's scrolled into view */
    val onRelatedPostsPositioned: (isGlobal: Boolean, coordinates: LayoutCoordinates) -> Unit,
)

/**
 * Everything below the post's body. Apart from the excerpt footer and related posts, which only arrive once the
 * body has loaded, it waits for [hasWebViewContent] so it doesn't flash up above the body and then jump down.
 */
@Composable
fun ReaderPostDetailFooter(
    detailsState: ReaderPostDetailsUiState?,
    hasWebViewContent: Boolean,
    likesState: TrainOfFacesUiState?,
    commentSnippetState: CommentSnippetUiState?,
    followConversationState: FollowConversationUiState?,
    readingPreferences: ReaderReadingPreferences,
    actions: ReaderPostDetailFooterActions,
) {
    val margin = dimensionResource(R.dimen.reader_detail_margin)
    Column(modifier = Modifier.fillMaxWidth()) {
        detailsState?.excerptFooterUiState?.let {
            ExcerptFooter(it, actions.onExcerptFooterClick, Modifier.padding(horizontal = margin))
        }
        if (hasWebViewContent) {
            val tags = detailsState?.headerUiState?.tagItems.orEmpty()
            if (tags.isNotEmpty()) {
                Tags(
                    tags = tags,
                    readingPreferences = readingPreferences,
                    onExpandToggled = actions.onTagsExpandToggled,
                    modifier = Modifier.padding(start = margin, end = margin, top = Margin.ExtraLarge.value),
                )
            }
            if (likesState != null && likesState.showLikeFacesTrainContainer) {
                // pads itself, so its ripple spans the full width
                Likers(likesState, actions.onLikesClick)
            }
            commentSnippetState?.let {
                CommentSnippet(
                    state = it,
                    followState = followConversationState,
                    onImageClick = actions.onCommentImageClick,
                    onCommentClick = actions.onCommentClick,
                    modifier = Modifier.padding(horizontal = margin),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = margin)) {
            RelatedPosts(detailsState?.localRelatedPosts, isGlobal = false, actions.onRelatedPostsPositioned)
            RelatedPosts(detailsState?.globalRelatedPosts, isGlobal = true, actions.onRelatedPostsPositioned)
        }
    }
}

@Composable
private fun ExcerptFooter(
    state: ReaderPostDetailsUiState.ExcerptFooterUiState,
    onClick: (String) -> Unit,
    modifier: Modifier,
) {
    val text = state.visitPostExcerptFooterLinkText ?: return
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
    Row(
        modifier = modifier
            .padding(vertical = Margin.ExtraLarge.value)
            .fillMaxWidth()
            .clickable { state.postLink?.let(onClick) }
            .padding(Margin.ExtraLarge.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = annotatedText(text),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
        Icon(
            painter = painterResource(R.drawable.ic_external_grey_min_24dp),
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(start = Margin.Medium.value),
        )
    }
}

/**
 * Keeps the link color the view model's HTML gives the blog's name
 */
@Composable
private fun annotatedText(text: UiString): AnnotatedString {
    val spanned = (text as? UiString.UiStringText)?.text as? Spanned
    return spanned?.toAnnotatedString() ?: AnnotatedString(uiStringText(text))
}

/**
 * The post's tags, on one line with a "+N" chip that shows the rest, as the View-based chip group did
 */
@Composable
private fun Tags(
    tags: List<TagUiState>,
    readingPreferences: ReaderReadingPreferences,
    onExpandToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slugs = remember(tags) { tags.map { it.slug } }
    var isExpanded by rememberSaveable(slugs) { mutableStateOf(false) }
    val textColor = LocalReadingTextColor.current
    val fontFamily = readingPreferences.fontFamily.toComposeFontFamily()
    val fontSizeMultiplier = readingPreferences.fontSize.multiplier
    val tagChip: @Composable (TagUiState) -> Unit = { tag ->
        TagChip(tag, textColor, fontSizeMultiplier, fontFamily)
    }
    val overflowChip: @Composable (String, String?) -> Unit = { text, description ->
        ReadingPreferencesPreviewTag(
            text = text,
            baseTextColor = textColor,
            fontSizeMultiplier = fontSizeMultiplier,
            fontFamily = fontFamily,
            modifier = Modifier.semantics { description?.let { contentDescription = it } },
            onClick = {
                isExpanded = !isExpanded
                onExpandToggled()
            },
        )
    }
    if (isExpanded) {
        ExpandedTags(tags, tagChip, { overflowChip(collapseTitle(), null) }, modifier)
    } else {
        CollapsedTags(
            tags = tags,
            tagChip = tagChip,
            overflowChip = { hiddenCount -> overflowChip(expandTitle(hiddenCount), expandDescription(hiddenCount)) },
            modifier = modifier,
        )
    }
}

@Composable
private fun expandTitle(hiddenCount: Int) = stringResource(
    R.string.reader_expandable_tags_view_overflow_indicator_expand_title_new,
    hiddenCount.toString()
)

@Composable
private fun expandDescription(hiddenCount: Int) =
    stringResource(R.string.show_n_hidden_items_desc, hiddenCount.toString())

@Composable
private fun collapseTitle() = stringResource(R.string.reader_expandable_tags_view_overflow_indicator_collapse_title)

@Composable
private fun TagChip(tag: TagUiState, textColor: Color, fontSizeMultiplier: Float, fontFamily: FontFamily) {
    val density = LocalDensity.current
    val widthModifier = if (tag.maxWidth == Int.MAX_VALUE) {
        Modifier
    } else {
        Modifier.widthIn(max = with(density) { tag.maxWidth.toDp() })
    }
    val onClick = tag.onClick
    ReadingPreferencesPreviewTag(
        text = tag.title,
        baseTextColor = textColor,
        fontSizeMultiplier = fontSizeMultiplier,
        fontFamily = fontFamily,
        modifier = widthModifier,
        onClick = onClick?.let { { it(tag.slug) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandedTags(
    tags: List<TagUiState>,
    tagChip: @Composable (TagUiState) -> Unit,
    collapseChip: @Composable () -> Unit,
    modifier: Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
        verticalArrangement = Arrangement.spacedBy(Margin.Medium.value),
    ) {
        tags.forEach { tagChip(it) }
        collapseChip()
    }
}

/**
 * Lays out as many tags as fit on one line, leaving room for the chip that counts the rest. The first tag always
 * shows, since its width is already capped.
 */
@Composable
private fun CollapsedTags(
    tags: List<TagUiState>,
    tagChip: @Composable (TagUiState) -> Unit,
    overflowChip: @Composable (Int) -> Unit,
    modifier: Modifier,
) {
    val spacing = with(LocalDensity.current) { Margin.Medium.value.roundToPx() }
    // built here rather than in the measure block, so a remeasure reuses them rather than recomposing every chip;
    // the overflow chip's content is kept per count for the same reason
    val tagChips: @Composable () -> Unit = { tags.forEach { tagChip(it) } }
    val overflowChips = remember(overflowChip) { mutableMapOf<Int, @Composable () -> Unit>() }
    fun overflowContent(hiddenCount: Int) = overflowChips.getOrPut(hiddenCount) { { overflowChip(hiddenCount) } }
    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val chips = subcompose("tags", tagChips).map { it.measure(looseConstraints) }
        fun countFitting(availableWidth: Int): Int {
            var usedWidth = 0
            var count = 0
            for (chip in chips) {
                val width = if (count == 0) chip.width else chip.width + spacing
                if (usedWidth + width > availableWidth) break
                usedWidth += width
                count++
            }
            return count
        }
        var visibleCount = countFitting(constraints.maxWidth)
        val overflow = if (visibleCount < chips.size) {
            // make room for the widest the overflow chip can be, then count what's left
            val widestOverflow = subcompose("overflowMeasure", overflowContent(chips.size))
                .first().measure(looseConstraints)
            visibleCount = countFitting(constraints.maxWidth - widestOverflow.width - spacing).coerceAtLeast(1)
            subcompose("overflow", overflowContent(chips.size - visibleCount)).first().measure(looseConstraints)
        } else {
            null
        }
        val placeables = chips.take(visibleCount) + listOfNotNull(overflow)
        val height = placeables.maxOf { it.height }
        layout(constraints.maxWidth, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + spacing
            }
        }
    }
}

@Composable
private fun Likers(state: TrainOfFacesUiState, onClick: () -> Unit) {
    val avatarSize = dimensionResource(R.dimen.avatar_sz_small)
    val avatarSizePx = with(LocalDensity.current) { avatarSize.roundToPx() }
    val avatarModels = remember(state.engageItemsList, avatarSizePx) {
        state.engageItemsList.filterIsInstance<AvatarItem>().map {
            TrainOfIconsModel(WPAvatarUtils.rewriteAvatarUrl(it.userAvatarUrl, avatarSizePx))
        }
    }
    val label = remember(state.engageItemsList) {
        state.engageItemsList.filterIsInstance<TrailingLabelTextItem>().firstOrNull()
    }
    val description = uiStringText(state.contentDescription)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.liker_faces_section_min_height))
            .clickable(onClick = onClick)
            .padding(horizontal = dimensionResource(R.dimen.reader_detail_margin))
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.showEmptyState) {
            Text(
                text = stringResource(
                    R.string.like_faces_error_loading_message,
                    state.emptyStateTitle?.let { uiStringText(it) }.orEmpty()
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            return@Row
        }
        if (avatarModels.isNotEmpty()) {
            TrainOfIcons(
                iconModels = avatarModels,
                iconSize = avatarSize,
                modifier = Modifier.padding(end = Margin.Medium.value),
            )
        }
        label?.let {
            Text(
                text = uiStringText(it.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommentSnippet(
    state: CommentSnippetUiState,
    followState: FollowConversationUiState?,
    onImageClick: (String) -> Unit,
    onCommentClick: (CommentSnippetItemState.CommentState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 45.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ReaderUtils.getTextForCommentSnippet(context, state.commentsNumber),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.showFollowConversation && followState != null) {
                FollowConversationActions(followState, R.string.reader_comments_follow_conversation)
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(bottom = Margin.Large.value),
            color = colorResource(R.color.reader_separator),
        )
        state.snippetItems.forEach { item -> CommentSnippetItem(item, onImageClick, onCommentClick) }
    }
}

@Composable
private fun CommentSnippetItem(
    item: CommentSnippetItemState,
    onImageClick: (String) -> Unit,
    onCommentClick: (CommentSnippetItemState.CommentState) -> Unit,
) {
    val margin = dimensionResource(R.dimen.reader_detail_margin)
    when (item) {
        is CommentSnippetItemState.LoadingState -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = margin),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        is CommentSnippetItemState.CommentState -> SnippetComment(item, onImageClick) { onCommentClick(item) }
        is CommentSnippetItemState.ButtonState -> OutlinedButton(
            onClick = { item.onCommentSnippetClicked(item.postId, item.blogId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(uiStringText(item.buttonText))
        }
        is CommentSnippetItemState.TextMessage -> Text(
            text = uiStringText(item.message),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = margin),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SnippetComment(
    comment: CommentSnippetItemState.CommentState,
    onImageClick: (String) -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(bottom = Margin.Large.value)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleAvatar(
                url = comment.avatarUrl,
                imageType = ImageType.AVATAR_WITH_BACKGROUND,
                size = dimensionResource(R.dimen.avatar_sz_medium),
            )
            Column(modifier = Modifier.padding(start = Margin.Large.value)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.authorName,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (comment.showAuthorBadge) AuthorBadge()
                }
                Text(
                    text = comment.datePublished,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CommentHtmlBody(
            html = comment.commentText.ifBlank { stringResource(R.string.comment_unable_to_show_error) },
            modifier = Modifier.padding(top = Margin.Small.value),
            onImageClick = onImageClick,
        )
    }
}

@Composable
private fun RelatedPosts(
    state: ReaderPostDetailsUiState.RelatedPostsUiState?,
    isGlobal: Boolean,
    onPositioned: (isGlobal: Boolean, coordinates: LayoutCoordinates) -> Unit,
) {
    val cards = state?.cards.orEmpty()
    AnimatedVisibility(visible = cards.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // not onGloballyPositioned, which runs on every frame the post scrolls
                .onPlaced { onPositioned(isGlobal, it) }
        ) {
            state?.headerLabel?.let {
                Text(
                    text = uiStringText(it).uppercase(),
                    modifier = Modifier.padding(vertical = Margin.ExtraLarge.value),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            cards.forEach { RelatedPostCard(it) }
        }
    }
}

@Composable
private fun RelatedPostCard(card: ReaderPostDetailsUiState.RelatedPostsUiState.ReaderRelatedPostUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { card.onItemClicked(card.postId, card.blogId, card.isGlobal) }
            .padding(bottom = Margin.ExtraLarge.value),
    ) {
        if (card.featuredImageVisibility) {
            AsyncImage(
                model = card.featuredImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(end = Margin.ExtraLarge.value)
                    .width(dimensionResource(R.dimen.reader_related_post_image_width))
                    .aspectRatio(RELATED_POST_IMAGE_RATIO)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.reader_featured_image_corner_radius)))
                    .background(colorResource(R.color.placeholder)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            card.title?.let {
                Text(
                    text = uiStringText(it),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = RELATED_POST_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            card.excerpt?.let {
                Text(
                    text = uiStringText(it),
                    modifier = Modifier.padding(top = Margin.Medium.value),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = RELATED_POST_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
