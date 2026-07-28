package org.wordpress.android.ui.comments.unified.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * The row of comment actions pinned above the reply box: moderate (approve/unapprove/untrash),
 * spam, like and a "more" overflow menu (edit, trash, copy/share link, delete permanently).
 * Mirrors the legacy comment_action_footer layout: equal-width icon+label buttons in on-surface
 * at medium emphasis. An enabled on toggle (approved or liked) is highlighted with the accent
 * colour at full opacity; once disabled it dims to on-surface at the reduced disabled opacity, so
 * a moderation action the user can't perform no longer shows a tappable-looking tint.
 *
 * When [canModerate] is false (the current user lacks the moderate_comments capability) the
 * moderation controls — moderate, spam, edit, trash and delete permanently — stay visible but are
 * disabled and dimmed, mirroring the legacy detail screen's per-capability gating. Like and the
 * copy/share-link actions are unaffected; the reply box (rendered separately) also stays active.
 */
@Composable
@Suppress("LongParameterList")
fun CommentActionFooter(
    status: CommentStatus,
    isLiked: Boolean,
    showLikeButton: Boolean,
    showCommentUrlActions: Boolean,
    canModerate: Boolean,
    onModerateClick: () -> Unit,
    onSpamClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEditClick: () -> Unit,
    onTrashClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onShareLinkClick: () -> Unit,
    onDeletePermanentlyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        val (moderateIconRes, moderateLabelRes) = when (status) {
            APPROVED -> Pair(R.drawable.ic_checkmark_white_24dp, R.string.comment_status_approved)
            TRASH -> Pair(R.drawable.ic_undo_white_24dp, R.string.mnu_comment_untrash)
            else -> Pair(R.drawable.ic_checkmark_white_24dp, R.string.mnu_comment_approve)
        }
        ActionButton(
            iconRes = moderateIconRes,
            labelRes = moderateLabelRes,
            isOn = status == APPROVED,
            enabled = canModerate,
            onClick = onModerateClick,
            modifier = Modifier.weight(1f)
        )

        ActionButton(
            iconRes = R.drawable.ic_spam_white_24dp,
            labelRes = if (status == SPAM) R.string.mnu_comment_unspam else R.string.mnu_comment_spam,
            enabled = canModerate,
            onClick = onSpamClick,
            modifier = Modifier.weight(1f)
        )

        if (showLikeButton) {
            ActionButton(
                iconRes = if (isLiked) R.drawable.ic_star_white_24dp else R.drawable.ic_star_outline_white_24dp,
                labelRes = if (isLiked) R.string.mnu_comment_liked else R.string.like,
                isOn = isLiked,
                onClick = onLikeClick,
                modifier = Modifier.weight(1f)
            )
        }

        MoreActionButton(
            status = status,
            showCommentUrlActions = showCommentUrlActions,
            canModerate = canModerate,
            onEditClick = onEditClick,
            onTrashClick = onTrashClick,
            onCopyLinkClick = onCopyLinkClick,
            onShareLinkClick = onShareLinkClick,
            onDeletePermanentlyClick = onDeletePermanentlyClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun MoreActionButton(
    status: CommentStatus,
    showCommentUrlActions: Boolean,
    canModerate: Boolean,
    onEditClick: () -> Unit,
    onTrashClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onShareLinkClick: () -> Unit,
    onDeletePermanentlyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        // The button itself stays enabled even without moderation rights so the copy/share-link
        // items remain reachable; the moderation items inside are individually disabled instead.
        ActionButton(
            iconRes = R.drawable.ic_more_horiz_white_24dp,
            labelRes = R.string.more,
            onClick = { isMenuExpanded = true },
            // Fill the Box (which carries this button's share of the row) so the icon centres in
            // its slot like the sibling buttons, instead of hugging the slot's start edge
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            val errorColor = MaterialTheme.colorScheme.error
            MoreMenuItem(R.string.edit, enabled = canModerate) {
                isMenuExpanded = false
                onEditClick()
            }
            if (status == TRASH) {
                MoreMenuItem(R.string.mnu_comment_untrash, enabled = canModerate) {
                    isMenuExpanded = false
                    onTrashClick()
                }
            } else {
                MoreMenuItem(R.string.mnu_comment_trash, color = errorColor, enabled = canModerate) {
                    isMenuExpanded = false
                    onTrashClick()
                }
            }
            if (showCommentUrlActions) {
                MoreMenuItem(R.string.copy_link_address) {
                    isMenuExpanded = false
                    onCopyLinkClick()
                }
                MoreMenuItem(R.string.share_link) {
                    isMenuExpanded = false
                    onShareLinkClick()
                }
            }
            if (status == TRASH || status == SPAM) {
                MoreMenuItem(R.string.mnu_comment_delete_permanently, color = errorColor, enabled = canModerate) {
                    isMenuExpanded = false
                    onDeletePermanentlyClick()
                }
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    @StringRes labelRes: Int,
    color: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        // Only apply the explicit colour (e.g. the destructive red) while enabled; when disabled,
        // defer to the menu item's disabled content colour so it greys out instead of staying red.
        text = { Text(text = stringResource(labelRes), color = if (enabled) color else Color.Unspecified) },
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun ActionButton(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOn: Boolean = false
) {
    // An on toggle (approved or liked) is highlighted with the accent colour at full opacity, but
    // only while it's actionable; once disabled it drops to on-surface at the reduced disabled
    // opacity so a moderation action the user can't perform no longer shows a tappable-looking tint.
    val color = if (isOn && enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
    val alpha = when {
        !enabled -> DISABLED_EMPHASIS_ALPHA
        isOn -> 1f
        else -> MEDIUM_EMPHASIS_ALPHA
    }
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = color.copy(alpha = alpha),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(labelRes),
            color = color.copy(alpha = alpha),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Matches material_emphasis_medium, used by the footer for enabled "off" action buttons. */
internal const val MEDIUM_EMPHASIS_ALPHA = 0.6f

/** Material's disabled-content opacity, used to dim moderation buttons the user can't use. */
private const val DISABLED_EMPHASIS_ALPHA = 0.38f

@Preview(showBackground = true)
@Composable
private fun CommentActionFooterPreview() {
    AppThemeM3 {
        CommentActionFooter(
            status = APPROVED,
            isLiked = true,
            showLikeButton = true,
            showCommentUrlActions = true,
            canModerate = true,
            onModerateClick = {},
            onSpamClick = {},
            onLikeClick = {},
            onEditClick = {},
            onTrashClick = {},
            onCopyLinkClick = {},
            onShareLinkClick = {},
            onDeletePermanentlyClick = {}
        )
    }
}
