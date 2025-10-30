package org.wordpress.android.support.aibot.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import androidx.compose.foundation.clickable
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
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.aibot.util.generateSampleBotConversations
import org.wordpress.android.support.common.ui.ConversationsListScreen
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBotConversationsListScreen(
    snackbarHostState: SnackbarHostState,
    conversations: List<BotConversation>,
    conversationsState: ConversationsSupportViewModel.ConversationsState,
    onConversationClick: (BotConversation) -> Unit,
    onBackClick: () -> Unit,
    onCreateNewConversationClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    val resources = LocalResources.current
    ConversationsListScreen(
        title = stringResource(R.string.ai_bot_conversations_title),
        addConversationContentDescription = stringResource(R.string.ai_bot_new_conversation_content_description),
        snackbarHostState = snackbarHostState,
        conversations = conversations,
        conversationsState = conversationsState,
        onBackClick = onBackClick,
        onCreateNewConversationClick = onCreateNewConversationClick,
        onRefresh = onRefresh,
        conversationListItem = { conversation ->
            BotConversationListItem(
                conversation = conversation,
                resources = resources,
                onClick = { onConversationClick(conversation) }
            )
        }
    )
}

@Composable
private fun BotConversationListItem(
    conversation: BotConversation,
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
            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = formatRelativeTime(conversation.mostRecentMessageDate, resources),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Preview(showBackground = true, name = "Conversations List")
@Composable
private fun ConversationsScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleBotConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { },
        )
    }
}

@Preview(showBackground = true, name = "Conversations List - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationsScreenPreviewDark() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleBotConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { },
        )
    }
}

@Preview(showBackground = true, name = "Conversations List")
@Composable
private fun ConversationsScreenWordPressPreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleBotConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { },
        )
    }
}

@Preview(showBackground = true, name = "Conversations List - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationsScreenPreviewWordPressDark() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = generateSampleBotConversations(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { },
        )
    }
}

@Preview(showBackground = true, name = "Empty Conversations List")
@Composable
private fun EmptyConversationsScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = emptyList(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { },
        )
    }
}

@Preview(showBackground = true, name = "Empty Conversations List - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EmptyConversationsScreenPreviewDark() {
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = emptyList(),
            conversationsState = ConversationsSupportViewModel.ConversationsState.Loaded,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { },
        )
    }
}
