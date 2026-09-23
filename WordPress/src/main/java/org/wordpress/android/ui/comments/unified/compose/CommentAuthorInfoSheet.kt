package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage

/**
 * The author details iOS's `CommentAuthorInfoSheet` shows when the author header is tapped - the
 * full date, website, email and IP - plus whether the author is registered, their approved comment
 * count and their bio. Each row is omitted when blank. Email, IP and the count need moderation
 * rights; the count and bio arrive after the sheet opens, so they sit last where appearing late
 * doesn't push the other rows around.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun CommentAuthorInfoSheet(
    authorName: String,
    authorAvatarUrl: String,
    accountRes: Int?,
    date: String,
    website: String,
    email: String,
    ipAddress: String,
    commentCount: Int?,
    bio: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = SHEET_BOTTOM_PADDING)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SHEET_H_PADDING)
            ) {
                RemoteImage(
                    imageUrl = authorAvatarUrl,
                    fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
                    modifier = Modifier
                        .size(AVATAR_SIZE)
                        .clip(CircleShape)
                )
                Column(modifier = Modifier.padding(start = AVATAR_GAP)) {
                    Text(text = authorName, style = MaterialTheme.typography.titleMedium)
                    accountRes?.let {
                        Text(
                            text = stringResource(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            InfoRow(R.string.comment_author_info_date, date)
            InfoRow(
                R.string.comment_author_info_website,
                website,
                onClick = { ActivityLauncher.openUrlExternal(context, website) }
            )
            // Selectable so the address can be copied, which is what a moderator usually wants.
            SelectionContainer {
                Column {
                    InfoRow(R.string.comment_author_info_email, email) {
                        // ListItem centres its trailing slot on the label and value together;
                        // dropping it by half the (labelSmall) overline puts it level with the value.
                        val overlineOffset = with(LocalDensity.current) {
                            (MaterialTheme.typography.labelSmall.lineHeight / 2).toDp()
                        }
                        IconButton(
                            onClick = { ActivityLauncher.openUrlExternal(context, "mailto:$email") },
                            modifier = Modifier.offset(y = overlineOffset)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = stringResource(R.string.comment_author_info_send_email)
                            )
                        }
                    }
                    InfoRow(R.string.comment_author_info_ip_address, ipAddress)
                }
            }
            InfoRow(R.string.comment_author_info_comment_count, commentCount?.toString().orEmpty())
            InfoRow(R.string.comment_author_info_bio, bio)
        }
    }
}

@Composable
private fun InfoRow(
    labelRes: Int,
    value: String,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    if (value.isBlank()) return
    ListItem(
        overlineContent = { Text(stringResource(labelRes)) },
        headlineContent = {
            Text(
                text = value,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}

private val SHEET_H_PADDING = 16.dp
private val SHEET_BOTTOM_PADDING = 16.dp
private val AVATAR_SIZE = 40.dp
private val AVATAR_GAP = 12.dp

@Preview(showBackground = true)
@Composable
private fun CommentAuthorInfoSheetPreview() {
    AppThemeM3 {
        CommentAuthorInfoSheet(
            authorName = "Priya Nair",
            authorAvatarUrl = "",
            accountRes = R.string.comment_author_info_registered,
            date = "Nov 14, 2023, 10:13 PM",
            website = "https://example.com",
            email = "priya@example.com",
            ipAddress = "203.0.113.4",
            commentCount = 12,
            bio = "Photographer and occasional writer.",
            onDismiss = {}
        )
    }
}
