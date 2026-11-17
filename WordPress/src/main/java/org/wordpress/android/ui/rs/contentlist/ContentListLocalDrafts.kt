package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.rs.RsLocalDraftUiModel

/**
 * Says how many drafts exist only on this device, which the server list can't show. Tapping it opens
 * [ContentListLocalDraftsSheet].
 */
@Composable
fun ContentListLocalDraftsBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Margin and corners match the tab chips and the list's cards, so the banner lines up with both.
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(BANNER_RADIUS),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BANNER_HORIZONTAL_MARGIN, vertical = BANNER_VERTICAL_MARGIN)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = pluralStringResource(R.plurals.post_rs_local_drafts_banner, count, count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Lists the drafts that exist only on this device. Tapping a row opens it; a failed one also offers
 * Retry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentListLocalDraftsSheet(
    drafts: List<RsLocalDraftUiModel>,
    onOpen: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.post_rs_local_drafts_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
                    .semantics { heading() }
            )
            Text(
                text = stringResource(R.string.post_rs_local_drafts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items = drafts, key = { it.localId }) { draft ->
                    LocalDraftRow(
                        draft = draft,
                        onOpen = { onOpen(draft.localId) },
                        onRetry = { onRetry(draft.localId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalDraftRow(
    draft: RsLocalDraftUiModel,
    onOpen: () -> Unit,
    onRetry: () -> Unit
) {
    val title = draft.title.ifBlank { stringResource(R.string.untitled_in_parentheses) }
    // Retry is its own focus stop, so it names its draft; otherwise every failed row's button reads the same.
    val retryDescription = stringResource(R.string.post_rs_local_drafts_retry_description, title)
    ListItem(
        headlineContent = {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (draft.dateLabel.isNotEmpty() || draft.isUploadFailed) {
            { LocalDraftDetails(draft) }
        } else {
            null
        },
        trailingContent = if (draft.isUploadFailed) {
            {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.semantics { contentDescription = retryDescription }
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            // TalkBack says "Double-tap to edit" rather than the generic "to activate".
            .clickable(onClickLabel = stringResource(R.string.edit), onClick = onOpen)
    )
}

@Composable
private fun LocalDraftDetails(draft: RsLocalDraftUiModel) {
    Column {
        if (draft.dateLabel.isNotEmpty()) Text(draft.dateLabel)
        if (draft.isUploadFailed) {
            Text(
                text = stringResource(R.string.post_rs_local_drafts_upload_failed),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val BANNER_HORIZONTAL_MARGIN = 8.dp
private val BANNER_VERTICAL_MARGIN = 4.dp
private val BANNER_RADIUS = 14.dp
