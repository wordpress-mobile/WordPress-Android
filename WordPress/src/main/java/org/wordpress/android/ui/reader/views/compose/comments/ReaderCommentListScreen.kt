package org.wordpress.android.ui.reader.views.compose.comments

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.comments.unified.compose.CommentHtmlBody
import org.wordpress.android.ui.comments.unified.compose.CommentReplySheet
import org.wordpress.android.ui.reader.FollowConversationUiState
import org.wordpress.android.ui.reader.ReaderCommentItem
import org.wordpress.android.ui.reader.ReaderCommentListUiState
import org.wordpress.android.ui.reader.ReaderCommentModeration
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.views.compose.article.articleColors
import org.wordpress.android.ui.reader.views.compose.article.body
import org.wordpress.android.ui.reader.views.compose.article.ui
import org.wordpress.android.ui.reader.views.compose.readableWidth
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImagePlaceholderManager
import org.wordpress.android.util.image.ImageType

private val avatarPlaceholders = ImagePlaceholderManager()

private val COMMENT_AVATAR_SIZE = 32.dp

// the text sits under the name, past the avatar and its gap
private val COMMENT_TEXT_INSET = COMMENT_AVATAR_SIZE + 12.dp

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderCommentListScreen(
    uiState: ReaderCommentListUiState,
    followState: FollowConversationUiState?,
    replyText: TextFieldValue,
    actions: ReaderCommentListActions,
    snackbarHostState: SnackbarHostState,
) {
    // hoisted so the list keeps its position while a comment's detail replaces it
    val listState = rememberLazyListState()
    val detail = uiState.detail
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BackHandler(enabled = detail != null, onBack = actions.onDetailBack)
    Scaffold(
        // shown in a bottom sheet, which has no status bar above it and insets its own composer
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Text(
                        stringResource(if (detail != null) R.string.comment else R.string.reader_title_comments),
                        style = ui(20f, 600, tracking = -0.01f),
                    )
                },
                navigationIcon = {
                    // the sheet closes by dragging it down, so it only needs back from a single comment
                    if (detail != null) {
                        IconButton(onClick = actions.onDetailBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                actions = { followState?.let { FollowConversationActions(it) } }
            )
        },
        bottomBar = { Composer(uiState, replyText, actions.onComposerClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        ContentListPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.padding(contentPadding),
            // a refresh drops comments past the first page, which would close a comment opened from there
            enabled = detail == null
        ) {
            AnimatedContent(
                targetState = detail,
                contentKey = { it?.comment?.commentId },
                transitionSpec = {
                    // opening a comment slides it in from the end; backing out slides the other way
                    val isOpening = (targetState?.depth ?: 0) > (initialState?.depth ?: 0)
                    val direction = (if (isOpening) 1 else -1) * (if (isRtl) -1 else 1)
                    (slideInHorizontally { direction * it } + fadeIn())
                        .togetherWith(slideOutHorizontally { -direction * it / SLIDE_OUT_FRACTION } + fadeOut())
                },
                label = "commentDetail"
            ) { shownDetail ->
                if (shownDetail != null) {
                    ReaderCommentDetail(uiState, shownDetail, actions)
                } else {
                    CommentList(uiState, actions, listState)
                }
            }
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.emptyMessage != null -> Text(
                    text = stringResource(uiState.emptyMessage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            }
        }
    }

    if (uiState.isReplySheetShown) {
        CommentReplySheet(
            replyText = replyText,
            onReplyTextChange = actions.onReplyTextChange,
            suggestions = uiState.suggestions,
            hint = stringResource(uiState.replyHint),
            isReplyInProgress = uiState.isSubmitting,
            isSendEnabled = true,
            onSendClick = actions.onSendReply,
            onDeleteDraft = actions.onDeleteDraft,
            onDismiss = actions.onReplySheetDismissed
        )
    }
}

@Composable
private fun CommentList(
    uiState: ReaderCommentListUiState,
    actions: ReaderCommentListActions,
    listState: LazyListState
) {
    LoadMoreOnScrollToEnd(
        listState = listState,
        itemCount = uiState.comments.size,
        canLoadMore = uiState.canLoadMore,
        onLoadMore = actions.onLoadMore
    )
    LaunchedEffect(uiState.scrollToCommentId) {
        val commentId = uiState.scrollToCommentId ?: return@LaunchedEffect
        val index = uiState.comments.indexOfFirst { it.commentId == commentId }
        // the post header is the first item
        if (index >= 0) listState.animateScrollToItem(index + 1)
        actions.onScrollHandled()
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            Column(Modifier.readableWidth()) { PostHeader(uiState, actions.onHeaderClick) }
        }
        items(uiState.comments, key = { it.commentId }) { comment ->
            Column(Modifier.readableWidth()) {
                CommentRow(comment, actions, onClick = { actions.onCommentClick(comment.commentId) })
            }
        }
        if (uiState.isLoadingMore) contentListLoadingMoreItem()
    }
}

@Composable
internal fun PostHeader(uiState: ReaderCommentListUiState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = uiState.isHeaderClickable, onClick = onClick)
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.comment_reply_to_user, uiState.postAuthorName),
            style = ui(12.5f, 500),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = uiState.postTitle,
            style = ui(17f, 600, tracking = -0.01f),
            color = articleColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun CommentRow(
    comment: ReaderCommentItem,
    actions: ReaderCommentListActions,
    onClick: (() -> Unit)?
) {
    val indentPerLevel = dimensionResource(R.dimen.reader_comment_indent_per_level)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                // a comment that's still sending can't be opened, since its id changes once it's sent
                .then(
                    if (onClick != null && !comment.isPending) Modifier.clickable(onClick = onClick) else Modifier
                )
                .background(
                    if (comment.isHighlighted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = HIGHLIGHT_ALPHA)
                    } else {
                        Color.Transparent
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .width(dimensionResource(R.dimen.reader_comments_selected_indicator_width))
                    .fillMaxHeight()
                    .background(if (comment.isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(indentPerLevel * comment.indentLevel))
            // as WPFolio lays a comment out: avatar beside the name and time, then the text, then Reply and likes
            Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp)) {
                CommentAuthorRow(comment, actions)
                CommentHtmlBody(
                    html = comment.html.ifBlank { stringResource(R.string.comment_unable_to_show_error) },
                    onImageClick = actions.onImageClick,
                    textStyle = body(15f, lineHeight = 1.5f),
                    modifier = Modifier
                        .padding(start = COMMENT_TEXT_INSET, end = 12.dp)
                        .padding(vertical = 2.dp)
                )
                Box(Modifier.padding(start = COMMENT_TEXT_INSET - 4.dp)) { CommentActionRow(comment, actions) }
            }
        }
    }
}

@Composable
private fun CommentAuthorRow(comment: ReaderCommentItem, actions: ReaderCommentListActions) {
    val context = LocalContext.current
    val avatarSizePx = with(LocalDensity.current) { COMMENT_AVATAR_SIZE.roundToPx() }
    val date = remember(comment.published, comment.isPending) {
        DateTimeUtils.javaDateToTimeSpan(
            if (comment.isPending) java.util.Date() else DateTimeUtils.dateFromIso8601(comment.published),
            context
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { actions.onAuthorClick(comment.commentId) }
                .padding(vertical = 4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(WPAvatarUtils.rewriteAvatarUrl(comment.avatarUrl, avatarSizePx))
                    .apply {
                        val type = ImageType.AVATAR_WITH_BACKGROUND
                        avatarPlaceholders.getPlaceholderResource(type)?.let { placeholder(it) }
                        avatarPlaceholders.getErrorResource(type)?.let { error(it) }
                    }
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(COMMENT_AVATAR_SIZE)
                    .clip(CircleShape)
            )
            // "Name · 2h" on one line
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = comment.authorName,
                    style = ui(13.5f, 600),
                    color = articleColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (comment.isPostAuthor) AuthorBadge()
                Text(
                    text = " · $date",
                    style = ui(13.5f, 500),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (comment.isPending) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(12.dp)
                    )
                }
            }
        }
        CommentMenu(comment, actions)
    }
}

@Composable
internal fun AuthorBadge() {
    Text(
        text = stringResource(R.string.comment_author_badge),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .padding(start = 8.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
private fun CommentMenu(comment: ReaderCommentItem, actions: ReaderCommentListActions) {
    if (!comment.canModerate) {
        IconButton(onClick = { actions.onShareClick(comment.commentId) }) {
            Icon(
                painter = painterResource(R.drawable.ic_share_white_24dp),
                contentDescription = stringResource(R.string.reader_comment_menu_share),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                contentDescription = stringResource(R.string.show_more_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            ReaderCommentModeration.entries.forEach { moderation ->
                MenuItem(moderation.labelRes, moderation.iconRes()) {
                    isExpanded = false
                    actions.onModerateClick(comment.commentId, moderation)
                }
            }
            HorizontalDivider()
            MenuItem(R.string.reader_comment_menu_edit, R.drawable.ic_pencil_white_24dp) {
                isExpanded = false
                actions.onEditClick(comment.commentId)
            }
            MenuItem(R.string.reader_comment_menu_share, R.drawable.ic_share_white_24dp) {
                isExpanded = false
                actions.onShareClick(comment.commentId)
            }
        }
    }
}

@Composable
private fun MenuItem(labelRes: Int, iconRes: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        leadingIcon = { Icon(painter = painterResource(iconRes), contentDescription = null) },
        onClick = onClick
    )
}

private fun ReaderCommentModeration.iconRes() = when (this) {
    ReaderCommentModeration.UNAPPROVE -> R.drawable.ic_cross_in_circle_white_24dp
    ReaderCommentModeration.SPAM -> R.drawable.ic_spam_white_24dp
    ReaderCommentModeration.TRASH -> R.drawable.ic_trash_white_24dp
}

@Composable
private fun CommentActionRow(comment: ReaderCommentItem, actions: ReaderCommentListActions) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        // text rather than an icon, as in WPFolio
        if (comment.canReply) {
            val color = if (comment.isReplyTarget) articleColors.text else mutedColor
            ActionButton(
                iconRes = null,
                label = stringResource(R.string.reply),
                color = color,
                onClick = { actions.onReplyClick(comment.commentId) }
            )
        }
        if (comment.showLike) {
            val context = LocalContext.current
            ActionButton(
                iconRes = if (comment.isLiked) {
                    R.drawable.ic_like_fill_new_24dp
                } else {
                    R.drawable.ic_like_outline_new_24dp
                },
                label = if (comment.numLikes > 0) comment.numLikes.toString() else stringResource(R.string.like),
                color = if (comment.isLiked) articleColors.text else mutedColor,
                isEnabled = comment.isLikeEnabled,
                isSelected = comment.isLiked,
                description = ReaderUtils.getLongLikeLabelText(context, comment.numLikes, comment.isLiked),
                onClick = { actions.onLikeClick(comment.commentId) }
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun ActionButton(
    iconRes: Int?,
    label: String,
    color: Color,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    isSelected: Boolean = false,
    description: String? = null
) {
    val tint = if (isEnabled) color else color.copy(alpha = DISABLED_ALPHA)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 4.dp)
            .semantics {
                description?.let { contentDescription = it }
                selected = isSelected
            }
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
            )
        }
        Text(text = label, style = ui(13f, 600), color = tint)
    }
}

/**
 * The bar under the list that opens the reply sheet, or says comments are closed. Hidden when logged out.
 */
@Composable
private fun Composer(uiState: ReaderCommentListUiState, replyText: TextFieldValue, onClick: () -> Unit) {
    if (!uiState.isLoggedIn) return
    Surface(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider()
            if (!uiState.isCommentsOpen) {
                Text(
                    text = stringResource(R.string.reader_label_comments_closed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                return@Box
            }
            // WPFolio's rounded field and send button; tapping opens the reply sheet, which has mentions and drafts
            val shape = RoundedCornerShape(22.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 12.dp)
                    .clip(shape)
                    .border(1.dp, articleColors.outline, shape)
                    .clickable(enabled = !uiState.isSubmitting, onClick = onClick)
                    .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            ) {
                // a saved draft shows in place of the hint
                val hasDraft = replyText.text.isNotBlank()
                Text(
                    text = if (hasDraft) replyText.text else stringResource(uiState.replyHint),
                    style = body(15f),
                    color = if (hasDraft) articleColors.text else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = if (hasDraft) articleColors.text else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun FollowConversationActions(
    followState: FollowConversationUiState,
    @StringRes followLabelRes: Int = R.string.reader_btn_follow,
) {
    val flags = followState.flags
    if (flags.isFollowMenuVisible) {
        TextButton(
            onClick = { followState.onFollowTapped?.invoke() },
            enabled = flags.isMenuEnabled && !flags.showMenuShimmer && followState.onFollowTapped != null
        ) {
            Text(stringResource(followLabelRes))
        }
    }
    if (flags.isBellMenuVisible) {
        IconButton(onClick = followState.onManageNotificationsTapped, enabled = flags.isMenuEnabled) {
            Icon(
                painter = painterResource(R.drawable.ic_notifications_white_24dp),
                contentDescription = stringResource(R.string.reader_btn_follow_conversation_settings)
            )
        }
    }
}

private const val HIGHLIGHT_ALPHA = 0.12f
private const val SLIDE_OUT_FRACTION = 4
private const val DISABLED_ALPHA = 0.38f
