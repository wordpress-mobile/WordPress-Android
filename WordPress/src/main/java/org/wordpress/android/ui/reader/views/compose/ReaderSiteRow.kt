package org.wordpress.android.ui.reader.views.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.util.image.ImagePlaceholderManager
import org.wordpress.android.util.image.ImageType

private val blavatarPlaceholders = ImagePlaceholderManager()

/**
 * A site with its icon, title and host, used by Reader search, Manage subscriptions and the filter sheet.
 * It ends with a Subscribe/Subscribed button when [onFollowClick] is set, or with [badge] otherwise.
 */
@Composable
@Suppress("LongParameterList")
fun ReaderSiteRow(
    title: String,
    subtitle: String,
    iconUrl: String?,
    onClick: () -> Unit,
    isFollowing: Boolean = false,
    isFollowEnabled: Boolean = true,
    onFollowClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    badge: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(iconUrl)
                .apply {
                    blavatarPlaceholders.getPlaceholderResource(ImageType.BLAVATAR)?.let { placeholder(it) }
                    blavatarPlaceholders.getErrorResource(ImageType.BLAVATAR)?.let { error(it) }
                }
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onFollowClick != null) {
            ReaderFollowToggleButton(isFollowing, isFollowEnabled, onFollowClick)
        } else if (badge != null) {
            ReaderCountBadge(badge)
        }
    }
}

@Composable
private fun ReaderCountBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clearAndSetSemantics { }
    )
}

@Composable
fun ReaderFollowToggleButton(isFollowing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (isFollowing) {
        OutlinedButton(onClick = onClick, enabled = enabled) {
            Text(stringResource(R.string.reader_btn_subscribed))
        }
    } else {
        Button(onClick = onClick, enabled = enabled) {
            Text(stringResource(R.string.reader_btn_subscribe))
        }
    }
}
