package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage
import java.text.NumberFormat

/**
 * The author info sheet iOS opens from the author header; blank rows are omitted. The count and bio
 * load after it opens, so the count sits in the band (no layout shift) and the bio's arrival animates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun CommentAuthorInfoSheet(
    authorName: String,
    authorAvatarUrl: String,
    /** Null for a pingback, whose author is a site rather than a person. */
    isRegistered: Boolean?,
    date: String,
    website: String,
    email: String,
    ipAddress: String,
    commentCount: Int?,
    bio: String,
    onDismiss: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = containerColor,
        // The band starts behind the handle, keeping the stock handle and its accessibility actions.
        dragHandle = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    ) {
        Column(modifier = Modifier.padding(bottom = SHEET_BOTTOM_PADDING)) {
            // Outside the animated column: animateContentSize clips, and the avatar draws above
            // this column's top edge.
            HeaderBand(authorAvatarUrl, commentCount, containerColor)
            AnimatedDetails(authorName, isRegistered, bio, date, website, email, ipAddress)
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun AnimatedDetails(
    authorName: String,
    isRegistered: Boolean?,
    bio: String,
    date: String,
    website: String,
    email: String,
    ipAddress: String
) {
    val context = LocalContext.current
    Column(modifier = Modifier.animateContentSize()) {
        NameBlock(authorName, isRegistered)
        if (bio.isNotBlank()) {
            BioCard(bio)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(ROW_GAP),
            modifier = Modifier.padding(horizontal = ROWS_H_PADDING)
        ) {
            InfoRow(Icons.Outlined.Schedule, R.string.comment_author_info_date, date)
            InfoRow(
                icon = Icons.Outlined.Language,
                labelRes = R.string.comment_author_info_website,
                value = website,
                valueColor = MaterialTheme.colorScheme.primary,
                onClick = { ActivityLauncher.openUrlExternal(context, website) }
            )
            // Selectable so a moderator can copy the address.
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
                    InfoRow(Icons.Outlined.AlternateEmail, R.string.comment_author_info_email, email) {
                        IconButton(onClick = { ActivityLauncher.openUrlExternal(context, "mailto:$email") }) {
                            Icon(
                                imageVector = Icons.Outlined.Mail,
                                contentDescription = stringResource(R.string.comment_author_info_send_email),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    InfoRow(Icons.Outlined.Lan, R.string.comment_author_info_ip_address, ipAddress)
                }
            }
        }
    }
}

@Composable
private fun HeaderBand(avatarUrl: String, commentCount: Int?, containerColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAND_EXTENSION + AVATAR_RING_SIZE / 2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAND_EXTENSION)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        // Taller than this box (it reaches up behind the handle), so it opts out of the height constraint,
        // pinned to the top so the offset lines it up.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = SHEET_H_PADDING)
                .offset(y = BAND_EXTENSION - AVATAR_RING_SIZE / 2)
                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                .size(AVATAR_RING_SIZE)
                .background(containerColor, CircleShape)
        ) {
            RemoteImage(
                imageUrl = avatarUrl,
                fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
            )
        }
        commentCount?.let {
            CommentCountChip(
                count = it,
                containerColor = containerColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = CHIP_END_PADDING)
                    .offset(y = BAND_EXTENSION - CHIP_HEIGHT / 2)
            )
        }
    }
}

@Composable
private fun CommentCountChip(count: Int, containerColor: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CHIP_ICON_GAP),
        modifier = modifier
            .height(CHIP_HEIGHT)
            .background(containerColor, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(start = CHIP_START_PADDING, end = CHIP_END_INNER_PADDING)
    ) {
        Icon(
            imageVector = Icons.Outlined.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(CHIP_ICON_SIZE)
        )
        Text(
            text = pluralStringResource(
                R.plurals.content_list_comment_count,
                count,
                NumberFormat.getIntegerInstance().format(count)
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NameBlock(authorName: String, isRegistered: Boolean?) {
    Column(
        modifier = Modifier.padding(
            start = SHEET_H_PADDING,
            end = SHEET_H_PADDING,
            top = NAME_TOP_GAP,
            bottom = NAME_BOTTOM_GAP
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VERIFIED_GAP)
        ) {
            Text(
                text = authorName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isRegistered == true) {
                // Decorative: the "Registered user" line below says the same thing.
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VERIFIED_SIZE)
                )
            }
        }
        isRegistered?.let {
            Text(
                text = stringResource(
                    if (it) R.string.comment_author_info_registered else R.string.comment_author_info_guest
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun BioCard(bio: String) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(start = SHEET_H_PADDING, end = SHEET_H_PADDING, bottom = BIO_BOTTOM_GAP)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(BIO_CORNER))
            .padding(horizontal = BIO_H_PADDING, vertical = BIO_V_PADDING)
    ) {
        Text(
            text = stringResource(R.string.comment_author_info_bio),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = bio,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (isExpanded) Int.MAX_VALUE else BIO_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!isExpanded) hasOverflow = it.hasVisualOverflow }
        )
        if (hasOverflow && !isExpanded) {
            Text(
                text = stringResource(R.string.more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { isExpanded = true }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun InfoRow(
    icon: ImageVector,
    labelRes: Int,
    value: String,
    valueColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    if (value.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_ICON_GAP),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ROW_CORNER))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ROW_H_PADDING, vertical = ROW_V_PADDING)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(ROW_ICON_CONTAINER)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ROW_ICON_SIZE)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingContent?.invoke()
    }
}

private val SHEET_H_PADDING = 24.dp
private val SHEET_BOTTOM_PADDING = 24.dp

// The drag handle slot is 48dp, so this brings the band to the design's 64dp.
private val BAND_EXTENSION = 16.dp
private val AVATAR_SIZE = 72.dp
private val AVATAR_RING_SIZE = 80.dp

private val CHIP_HEIGHT = 32.dp
private val CHIP_END_PADDING = 20.dp
private val CHIP_START_PADDING = 10.dp
private val CHIP_END_INNER_PADDING = 12.dp
private val CHIP_ICON_SIZE = 18.dp
private val CHIP_ICON_GAP = 4.dp

private val NAME_TOP_GAP = 4.dp
private val NAME_BOTTOM_GAP = 16.dp
private val VERIFIED_SIZE = 20.dp
private val VERIFIED_GAP = 8.dp

private const val BIO_COLLAPSED_LINES = 3
private val BIO_CORNER = 16.dp
private val BIO_H_PADDING = 16.dp
private val BIO_V_PADDING = 14.dp
private val BIO_BOTTOM_GAP = 12.dp

private val ROWS_H_PADDING = 12.dp
private val ROW_GAP = 4.dp
private val ROW_H_PADDING = 12.dp
private val ROW_V_PADDING = 8.dp
private val ROW_CORNER = 12.dp
private val ROW_ICON_GAP = 14.dp
private val ROW_ICON_CONTAINER = 40.dp
private val ROW_ICON_SIZE = 20.dp

@Preview(showBackground = true)
@Composable
private fun CommentAuthorInfoSheetPreview() {
    AppThemeM3 {
        CommentAuthorInfoSheet(
            authorName = "nick_tester",
            authorAvatarUrl = "",
            isRegistered = true,
            date = "Sep 22, 2026, 6:44 AM",
            website = "https://nbradburytest.wordpress.com",
            email = "nbradbury.test@gmail.com",
            ipAddress = "192.0.81.205",
            commentCount = 12,
            bio = "Mobile developer testing things so you don't have to. Writes about Android, coffee, " +
                "and the occasional rubber duck. Based in Colorado, usually replying from a trailhead.",
            onDismiss = {}
        )
    }
}
