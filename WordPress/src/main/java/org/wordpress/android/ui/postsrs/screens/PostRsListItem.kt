package org.wordpress.android.ui.postsrs.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.PostRsMenuAction
import org.wordpress.android.ui.postsrs.PostRsUiModel
import org.wordpress.android.ui.postsrs.data.PostRsRestClient

@Composable
fun PostRsListItem(
    post: PostRsUiModel,
    onClick: () -> Unit,
    onMenuAction: (PostRsMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        post.isPlaceholder -> PlaceholderItem(modifier)
        post.isError -> ErrorItem(modifier)
        else -> PostContentItem(post, onClick, onMenuAction, modifier)
    }
}

@Composable
private fun PostContentItem(
    post: PostRsUiModel,
    onClick: () -> Unit,
    onMenuAction: (PostRsMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val statusLabel = if (post.statusLabelResId != 0) {
                    stringResource(post.statusLabelResId)
                } else {
                    null
                }
                val bullet = stringResource(R.string.bullet_with_spaces)
                val dateText = buildString {
                    if (!statusLabel.isNullOrBlank()) {
                        append(statusLabel)
                        if (post.date.isNotBlank()) append(bullet)
                    }
                    append(post.date)
                    if (!post.authorDisplayName.isNullOrBlank()) {
                        if (isNotBlank()) append(bullet)
                        append(post.authorDisplayName)
                    }
                }
                if (dateText.isNotBlank()) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = post.title.ifBlank { stringResource(R.string.untitled_in_parentheses) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.excerpt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 16.dp)
                    .width(FEATURED_IMAGE_SIZE),
                horizontalAlignment = Alignment.End
            ) {
                if (post.actions.isNotEmpty()) {
                    PostMenuButton(
                        actions = post.actions,
                        onAction = onMenuAction
                    )
                }
                if (post.featuredImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.featuredImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(
                            R.string.featured_image_desc
                        ),
                        modifier = Modifier
                            .size(FEATURED_IMAGE_SIZE)
                            .clip(RoundedCornerShape(2.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (post.featuredImageId != 0L) {
                    ShimmerBox(
                        modifier = Modifier
                            .size(FEATURED_IMAGE_SIZE)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun PostMenuButton(
    actions: List<PostRsMenuAction>,
    onAction: (PostRsMenuAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = FEATURED_IMAGE_SIZE, height = 48.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.more),
                onClick = { expanded = true }
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.more),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                val color = if (action.isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(action.labelResId), color = color)
                    },
                    onClick = {
                        expanded = false
                        onAction(action)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(action.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = color
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
    )
}

@Composable
internal fun PlaceholderItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            repeat(2) { index ->
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                if (index == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ErrorItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = stringResource(R.string.post_rs_failed_to_load),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

private val FEATURED_IMAGE_SIZE = PostRsRestClient.FEATURED_IMAGE_SIZE_DP.dp
