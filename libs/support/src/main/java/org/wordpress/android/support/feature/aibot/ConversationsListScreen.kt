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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.support.feature.aibot.AIBotSupportActivity.ConversationScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
                title = { Text("Conversations") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onCreateNewConversationClick() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New conversation"
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
                    text = conversation.title,
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "Conversations List")
@Composable
private fun ConversationsScreenPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Conversations") },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            },
        ) { contentPadding ->
            ConversationsListPreview(modifier = Modifier.padding(contentPadding))
        }
    }
}

@Composable
private fun ConversationsListPreview(modifier: Modifier) {
    val now = Date()
    val sampleConversations = listOf(
        BotConversation(
            id = 1234,
            title = "App Crashing on Launch",
            mostRecentMessageDate = Date(now.time - 120_000), // 2 minutes ago
            messages = listOf(
                BotMessage(
                    id = 1001,
                    text = "Hi, I'm having trouble with the app.",
                    date = Date(now.time - 3_600_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1002,
                    text = "Wonderful! I'm so glad that resolved the issue for you.",
                    date = Date(now.time - 120_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1235,
            title = "Site Setup Assistance",
            mostRecentMessageDate = Date(now.time - 7_200_000), // 2 hours ago
            messages = listOf(
                BotMessage(
                    id = 2001,
                    text = "I just created my WordPress site and need help getting started.",
                    date = Date(now.time - 7_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 2002,
                    text = "Congratulations on your new site! I'd be happy to help you get started.",
                    date = Date(now.time - 7_200_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1236,
            title = "Theme Customization",
            mostRecentMessageDate = Date(now.time - 86_400_000), // 1 day ago
            messages = listOf(
                BotMessage(
                    id = 3001,
                    text = "How can I change the colors on my site?",
                    date = Date(now.time - 87_000_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 3002,
                    text = "You can change the colors by going to Appearance → Customize → Colors.",
                    date = Date(now.time - 86_400_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1237,
            title = "SEO Optimization Tips",
            mostRecentMessageDate = Date(now.time - 259_200_000), // 3 days ago
            messages = listOf(
                BotMessage(
                    id = 4001,
                    text = "My site isn't showing up in Google search results.",
                    date = Date(now.time - 259_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 4002,
                    text = "To improve your SEO, consider installing an SEO plugin like Yoast.",
                    date = Date(now.time - 259_200_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1238,
            title = "Site Performance Questions",
            mostRecentMessageDate = Date(now.time - 604_800_000), // 1 week ago
            messages = listOf(
                BotMessage(
                    id = 5001,
                    text = "My website seems to be loading slowly.",
                    date = Date(now.time - 605_400_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 5002,
                    text = "Your site is loading well, but here are some tips to optimize further.",
                    date = Date(now.time - 604_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
        }

        items(sampleConversations) { conversation ->
            ConversationCardPreview(conversation = conversation)
        }

        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun ConversationCardPreview(conversation: BotConversation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { }),
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
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatRelativeTimePreview(conversation.mostRecentMessageDate),
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
