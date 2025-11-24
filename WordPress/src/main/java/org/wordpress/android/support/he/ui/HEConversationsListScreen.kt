package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.common.ui.ConversationsListScreen
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.util.generateSampleHESupportConversations
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HEConversationsListScreen(
    snackbarHostState: SnackbarHostState,
    conversations: List<SupportConversation>,
    conversationsState: ConversationsSupportViewModel.ConversationsState,
    onConversationClick: (SupportConversation) -> Unit,
    onBackClick: () -> Unit,
    onCreateNewConversationClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val resources = LocalResources.current
    ConversationsListScreen(
        title = stringResource(R.string.he_support_conversations_title),
        addConversationContentDescription = stringResource(R.string.he_support_new_conversation_content_description),
        snackbarHostState = snackbarHostState,
        conversations = conversations,
        conversationsState = conversationsState,
        onBackClick = onBackClick,
        onCreateNewConversationClick = onCreateNewConversationClick,
        onRefresh = onRefresh,
        conversationListItem = { conversation ->
            HEConversationListItem(
                conversation = conversation,
                resources = resources,
                onClick = { onConversationClick(conversation) }
            )
        }
    )
}

@Composable
private fun HEConversationListItem(
    conversation: SupportConversation,
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
            // Status badge
            ConversationStatusBadge(
                status = conversation.status,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = formatRelativeTime(
                        conversation.lastMessageSentAt,
                        resources
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = conversation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_chevron_right_white_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List")
@Composable
private fun ConversationsScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false) {
        HEConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleHESupportConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationsScreenPreviewDark() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true) {
        HEConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleHESupportConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List - WordPress")
@Composable
private fun ConversationsScreenWordPressPreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        HEConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleHESupportConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationsScreenPreviewWordPressDark() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        HEConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleHESupportConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { }
        )
    }
}
