package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ShimmerBox
import org.wordpress.android.ui.pagesrs.PageRsDisplayState
import org.wordpress.android.ui.pagesrs.PageRsListItem
import org.wordpress.android.ui.pagesrs.PageRsMenuAction
import org.wordpress.android.ui.pagesrs.PageRsUiModel
import org.wordpress.android.ui.postsrs.screens.PlaceholderItem

@Composable
internal fun PageRsRow(
    item: PageRsListItem,
    onClick: () -> Unit,
    onMenuAction: (PageRsMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val page = item.page
    val indentLevel = (item as? PageRsListItem.Real)?.indentLevel ?: 0
    val virtualKind = (item as? PageRsListItem.Virtual)?.kind
    when (page.displayState) {
        PageRsDisplayState.PLACEHOLDER -> PlaceholderItem(modifier)
        PageRsDisplayState.ERROR -> ErrorItem(modifier)
        PageRsDisplayState.NORMAL,
        PageRsDisplayState.FETCHING_WITH_DATA,
        PageRsDisplayState.FAILED_WITH_DATA -> PageContentItem(
            page = page,
            indentLevel = indentLevel,
            virtualKind = virtualKind,
            onClick = onClick,
            onMenuAction = onMenuAction,
            modifier = modifier
        )
    }
}

@Composable
private fun PageContentItem(
    page: PageRsUiModel,
    indentLevel: Int,
    virtualKind: PageRsListItem.Virtual.Kind?,
    onClick: () -> Unit,
    onMenuAction: (PageRsMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = (H_PADDING_DP + indentLevel * INDENT_STEP_DP).dp,
                end = H_PADDING_DP.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (virtualKind != null) {
                Icon(
                    imageVector = virtualKind.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                val virtualLabel = virtualKind?.let { stringResource(it.labelResId()) }
                val statusLabel = page.statusLabelResId.takeIf { it != 0 }?.let { stringResource(it) }
                val bullet = stringResource(R.string.bullet_with_spaces)
                val headerText = listOfNotNull(
                    virtualLabel,
                    statusLabel,
                    page.date.takeIf { it.isNotBlank() },
                    page.authorDisplayName?.takeIf { it.isNotBlank() }
                ).joinToString(bullet)
                if (headerText.isNotBlank()) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (page.badges.isNotEmpty()) {
                    BadgeRow(page.badges)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = page.title.ifBlank { stringResource(R.string.untitled_in_parentheses) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (page.excerpt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = page.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (page.actions.isNotEmpty() || page.featuredImageId != 0L) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (page.actions.isNotEmpty()) {
                        PageMenuButton(actions = page.actions, onAction = onMenuAction)
                    }
                    if (page.featuredImageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(page.featuredImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.featured_image_desc),
                            modifier = Modifier
                                .size(FEATURED_IMAGE_SIZE)
                                .clip(RoundedCornerShape(2.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (page.featuredImageId != 0L) {
                        ShimmerBox(
                            modifier = Modifier
                                .size(FEATURED_IMAGE_SIZE)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
        if (page.displayState == PageRsDisplayState.FETCHING_WITH_DATA) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun PageMenuButton(
    actions: List<PageRsMenuAction>,
    onAction: (PageRsMenuAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 48.dp)
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
private fun BadgeRow(badges: List<Int>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        badges.forEach { labelResId ->
            Text(
                text = stringResource(labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
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
            text = stringResource(R.string.page_rs_failed_to_load),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

private fun PageRsListItem.Virtual.Kind.icon(): ImageVector = when (this) {
    PageRsListItem.Virtual.Kind.HOMEPAGE -> Icons.Filled.Home
    PageRsListItem.Virtual.Kind.POSTS_PAGE -> Icons.AutoMirrored.Filled.Article
}

private fun PageRsListItem.Virtual.Kind.labelResId(): Int = when (this) {
    PageRsListItem.Virtual.Kind.HOMEPAGE -> R.string.site_settings_homepage
    PageRsListItem.Virtual.Kind.POSTS_PAGE -> R.string.site_settings_posts_page
}

private const val INDENT_STEP_DP = 16
private const val H_PADDING_DP = 16
private val FEATURED_IMAGE_SIZE = 64.dp

@Preview(showBackground = true)
@Composable
private fun PreviewPageItem() {
    MaterialTheme {
        PageRsRow(
            item = PageRsListItem.Real(
                PageRsUiModel(
                    remotePageId = 1L,
                    title = "About",
                    excerpt = "Learn more about our journey and what we do.",
                    date = "Dec 15, 2025",
                    featuredImageId = 42L
                )
            ),
            onClick = {},
            onMenuAction = {}
        )
    }
}
