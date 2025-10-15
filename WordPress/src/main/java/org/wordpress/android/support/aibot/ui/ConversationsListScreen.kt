package org.wordpress.android.support.aibot.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    conversations: StateFlow<List<BotConversation>>,
    isLoading: Boolean,
    onConversationClick: (BotConversation) -> Unit,
    onBackClick: () -> Unit,
    onCreateNewConversationClick: () -> Unit,
) {
    Scaffold(
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
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.ai_bot_new_conversation_content_description)
                        )
                    }
                }
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            ShowConversationsList(
                modifier = Modifier.fillMaxSize(),
                conversations = conversations,
                onConversationClick = onConversationClick
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Add top spacing
            Spacer(modifier = Modifier.padding(top = 4.dp))
        }

        items(conversations) { conversation ->
            ConversationCard(
                conversation = conversation,
                onClick = { onConversationClick(conversation) }
            )
        }

        item {
            // Add bottom spacing
            Spacer(modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: BotConversation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = formatRelativeTime(conversation.mostRecentMessageDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Conversations List")
@Composable
private fun ConversationsScreenPreview() {
    val sampleConversations = MutableStateFlow(generateSampleBotConversations())

    MaterialTheme(colorScheme = lightColorScheme()) {
        ConversationsListScreen(
            conversations = sampleConversations.asStateFlow(),
            isLoading = false,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
        )
    }
}

@Preview(showBackground = true, name = "Conversations List - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationsScreenPreviewDark() {
    val sampleConversations = MutableStateFlow(generateSampleBotConversations())

    MaterialTheme(colorScheme = darkColorScheme()) {
        ConversationsListScreen(
            conversations = sampleConversations.asStateFlow(),
            isLoading = false,
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
        )
    }
}
