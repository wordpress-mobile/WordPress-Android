package org.wordpress.android.support.feature.aibot

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AIBotConversationsActivity : AppCompatActivity() {
    private val viewModel by viewModels<AIBotConversationsViewModel>()

    private lateinit var composeView: ComposeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    ConversationsContent()
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConversationsContent() {
        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Conversations") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    )
                },
            ) { contentPadding ->
                ShowConversationsList(
                    modifier = Modifier.padding(contentPadding)
                )
            }
        }
    }

    @Composable
    private fun ShowConversationsList(modifier: Modifier) {
        val conversations by viewModel.conversations.collectAsState()

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Add top spacing
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
            }

            items(conversations) { conversation ->
                ConversationCard(
                    conversation = conversation,
                    onClick = { viewModel.onConversationClick(conversation) }
                )
            }

            item {
                // Add bottom spacing
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 4.dp))
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

    private fun formatRelativeTime(date: Date): String {
        val now = Date()
        val diffMillis = now.time - date.time
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
            diffHours < 24 -> "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
            diffDays < 7 -> "$diffDays day${if (diffDays == 1L) "" else "s"} ago"
            diffDays < 30 -> "${diffDays / 7} week${if (diffDays / 7 == 1L) "" else "s"} ago"
            else -> {
                val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                formatter.format(date)
            }
        }
    }
}

// Preview functions
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
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

@Composable
private fun formatRelativeTimePreview(date: Date): String {
    val now = Date()
    val diffMillis = now.time - date.time
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
        diffHours < 24 -> "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
        diffDays < 7 -> "$diffDays day${if (diffDays == 1L) "" else "s"} ago"
        diffDays < 30 -> "${diffDays / 7} week${if (diffDays / 7 == 1L) "" else "s"} ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}
