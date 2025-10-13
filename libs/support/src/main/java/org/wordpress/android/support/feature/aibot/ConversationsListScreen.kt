package org.wordpress.android.support.feature.aibot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.support.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.support.feature.aibot.AIBotSupportActivity.ConversationScreen
import org.wordpress.android.support.feature.aibot.BotConversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.collections.List

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    conversations: StateFlow<List<BotConversation>>,
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
        ShowConversationsList(
            modifier = Modifier.padding(contentPadding),
            conversations = conversations,
            onConversationClick = onConversationClick
        )
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.getTitle(LocalContext.current),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatRelativeTime(conversation.mostRecentMessageDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            conversation.messages.lastOrNull()?.let { lastMessage ->
                Text(
                    text = lastMessage.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { },
        )
    }
}
