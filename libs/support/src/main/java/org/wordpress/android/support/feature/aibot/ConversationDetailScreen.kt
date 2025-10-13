package org.wordpress.android.support.feature.aibot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversation: BotConversation,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversation.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                WelcomeHeader()
            }

            items(conversation.messages) { message ->
                MessageBubble(message = message)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.displaySmall
            )

            Text(
                text = "Howdy demo-user! 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "I'm your personal AI assistant. I can help with any questions about your site or account.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            maxLines = 4
        )

        IconButton(
            onClick = onSendClick,
            enabled = messageText.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (messageText.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: BotMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isWrittenByUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.isWrittenByUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isWrittenByUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isWrittenByUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isWrittenByUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatRelativeTime(message.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isWrittenByUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "Conversation Detail")
@Composable
private fun ConversationDetailScreenPreview() {
    val now = Date()
    val sampleConversation = BotConversation(
        id = 1234,
        title = "App Crashing on Launch",
        mostRecentMessageDate = Date(now.time - 120_000),
        messages = listOf(
            BotMessage(
                id = 1001,
                text = "Hi, I'm having trouble with the app. It keeps crashing when I try to open it after the latest update. Can you help?",
                date = Date(now.time - 3_600_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = true
            ),
            BotMessage(
                id = 1002,
                text = "I'm sorry to hear you're experiencing crashes! I'd be happy to help you troubleshoot this issue. Let me ask a few questions to better understand what's happening. What device are you using and what Android version are you running?",
                date = Date(now.time - 3_540_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = false
            ),
            BotMessage(
                id = 1003,
                text = "I'm using a Pixel 8 Pro with Android 14. The app worked fine before the update yesterday.",
                date = Date(now.time - 3_480_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = true
            ),
            BotMessage(
                id = 1004,
                text = "Thank you for that information! Android 14 on Pixel 8 Pro should work well with our latest update. Let's try a few troubleshooting steps:\n\n1. First, try force-closing the app and reopening it\n2. If that doesn't work, try restarting your phone\n3. As a last resort, you might need to clear app data or reinstall\n\nCan you try step 1 first and let me know if that helps?",
                date = Date(now.time - 3_420_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = false
            ),
            BotMessage(
                id = 1005,
                text = "I tried force-closing and restarting my phone, but it's still crashing immediately when I tap the app icon. Should I try reinstalling?",
                date = Date(now.time - 3_300_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = true
            ),
            BotMessage(
                id = 1006,
                text = "Yes, let's try reinstalling the app. This will often resolve issues caused by corrupted app data during updates. Here's what to do:\n\n1. Long press the app icon and tap 'Uninstall'\n2. Go to the Play Store and reinstall the app\n3. Sign back into your account\n\nYour data should be preserved if you're signed into your account. Give this a try and let me know how it goes!",
                date = Date(now.time - 3_240_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = false
            ),
            BotMessage(
                id = 1007,
                text = "That worked! The app is opening normally now. Thank you so much for your help!",
                date = Date(now.time - 180_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = true
            ),
            BotMessage(
                id = 1008,
                text = "Wonderful! I'm so glad that resolved the issue for you. The reinstall process often fixes problems that occur during app updates. If you run into any other issues, please don't hesitate to reach out. Is there anything else I can help you with today?",
                date = Date(now.time - 120_000),
                userWantsToTalkToHuman = false,
                isWrittenByUser = false
            )
        )
    )

    MaterialTheme {
        ConversationDetailScreenPreviewContent(conversation = sampleConversation)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationDetailScreenPreviewContent(conversation: BotConversation) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversation.title) },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 4
                )

                IconButton(
                    onClick = { },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                WelcomeHeaderPreview()
            }

            items(conversation.messages) { message ->
                MessageBubblePreview(message = message)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WelcomeHeaderPreview() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.displaySmall
            )

            Text(
                text = "Howdy demo-user! 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "I'm your personal AI assistant. I can help with any questions about your site or account.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MessageBubblePreview(message: BotMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isWrittenByUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.isWrittenByUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isWrittenByUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isWrittenByUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isWrittenByUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatRelativeTimePreview(message.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isWrittenByUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}
