package org.wordpress.android.support.aibot.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.aibot.util.generateSampleBotConversations
import org.wordpress.android.support.common.ui.EmptyConversationsView
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBotConversationsListScreen(
    snackbarHostState: SnackbarHostState,
    conversations: StateFlow<List<BotConversation>>,
    isLoading: Boolean,
    onConversationClick: (BotConversation) -> Unit,
    onBackClick: () -> Unit,
    onCreateNewConversationClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_bot_conversations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.ai_bot_back_button_content_description)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onCreateNewConversationClick() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.ai_bot_new_conversation_content_description)
                        )
                    }
                }
            )
        },
    ) { contentPadding ->
        val conversationsList by conversations.collectAsState()

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when {
                conversationsList.isEmpty() && !isLoading -> {
                    EmptyConversationsView(
                        modifier = Modifier.fillMaxSize(),
                        onCreateNewConversationClick = onCreateNewConversationClick
                    )
                }
                else -> {
                    ShowConversationsList(
                        modifier = Modifier.fillMaxSize(),
                        conversations = conversations,
                        onConversationClick = onConversationClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowConversationsList(
    modifier: Modifier,
    conversations: StateFlow<List<BotConversation>>,
    onConversationClick: (BotConversation) -> Unit
) {
    val conversations by conversations.collectAsState()
    val resources = LocalResources.current

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = conversations,
            key = { it.id }
        ) { conversation ->
            ConversationListItem(
                conversation = conversation,
                resources = resources,
                onClick = { onConversationClick(conversation) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ConversationListItem(
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
    val sampleConversations = MutableStateFlow(generateSampleBotConversations())
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = sampleConversations.asStateFlow(),
            isLoading = false,
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
    val sampleConversations = MutableStateFlow(generateSampleBotConversations())
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = sampleConversations.asStateFlow(),
            isLoading = false,
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
    val sampleConversations = MutableStateFlow(generateSampleBotConversations())
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = sampleConversations.asStateFlow(),
            isLoading = true,
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
    val sampleConversations = MutableStateFlow(generateSampleBotConversations())
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = sampleConversations.asStateFlow(),
            isLoading = true,
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
    val emptyConversations = MutableStateFlow(emptyList<BotConversation>())
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = emptyConversations.asStateFlow(),
            isLoading = false,
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
    val emptyConversations = MutableStateFlow(emptyList<BotConversation>())
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true) {
        AIBotConversationsListScreen(
            snackbarHostState = snackbarHostState,
            conversations = emptyConversations.asStateFlow(),
            isLoading = false,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
            onRefresh = { },
        )
    }
}
