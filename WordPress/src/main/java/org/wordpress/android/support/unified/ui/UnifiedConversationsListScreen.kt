package org.wordpress.android.support.unified.ui

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.support.unified.util.formatRelativeTime
import org.wordpress.android.support.common.ui.ConversationsListScreen
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.unified.model.UnifiedConversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedConversationsListScreen(
    snackbarHostState: SnackbarHostState,
    conversations: List<UnifiedConversation>,
    conversationsState: ConversationsSupportViewModel.ConversationsState,
    onConversationClick: (UnifiedConversation) -> Unit,
    onBackClick: () -> Unit,
    onCreateNewConversationClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    val resources = LocalResources.current
    ConversationsListScreen(
        title = stringResource(R.string.unified_support_conversations_title),
        addConversationContentDescription =
            stringResource(R.string.unified_support_new_conversation_content_description),
        snackbarHostState = snackbarHostState,
        conversations = conversations,
        conversationsState = conversationsState,
        onBackClick = onBackClick,
        onCreateNewConversationClick = onCreateNewConversationClick,
        onRefresh = onRefresh,
        conversationListItem = { conversation ->
            UnifiedConversationListItem(
                conversation = conversation,
                resources = resources,
                onClick = { onConversationClick(conversation) }
            )
        }
    )
}

@Composable
private fun UnifiedConversationListItem(
    conversation: UnifiedConversation,
    resources: Resources,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            UnifiedStatusBadge(
                conversation = conversation,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.title.ifBlank { conversation.description },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = formatRelativeTime(conversation.updatedAt, resources),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (conversation.description.isNotBlank() && conversation.title.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = conversation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            painter = painterResource(R.drawable.ic_chevron_right_white_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun UnifiedStatusBadge(
    conversation: UnifiedConversation,
    modifier: Modifier = Modifier
) {
    if (conversation.isBot) {
        Text(
            text = stringResource(R.string.unified_support_status_bot),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    } else {
        ConversationStatusBadge(
            status = conversation.status,
            modifier = modifier
        )
    }
}
