package org.wordpress.android.support.aibot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import org.wordpress.android.R
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.aibot.util.generateSampleBotConversations
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversation: BotConversation,
    isLoading: Boolean,
    isBotTyping: Boolean,
    canSendMessage: Boolean,
    userName: String,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when conversation changes or messages are added or typing state changes
    LaunchedEffect(conversation.id, conversation.messages.size, isBotTyping) {
        if (conversation.messages.isNotEmpty() || isBotTyping) {
            coroutineScope.launch {
                // +2 for welcome header and spacer, +1 if typing indicator is showing
                val itemCount = conversation.messages.size + 2 + if (isBotTyping) 1 else 0
                listState.animateScrollToItem(itemCount)
            }
        }
    }

    val resources = LocalResources.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.ai_bot_back_button_content_description)
                        )
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                canSendMessage = canSendMessage,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    WelcomeHeader(userName)
                }

                // Key ensures the items recompose when messages change
                items(
                    items = conversation.messages,
                    key = { message -> message.id }
                ) { message ->
                    MessageBubble(message = message, resources = resources)
                }

                // Show typing indicator when bot is typing
                if (isBotTyping) {
                    item {
                        TypingIndicatorBubble()
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeader(userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                text = stringResource(R.string.ai_bot_welcome_greeting, userName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.ai_bot_welcome_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    messageText: String,
    canSendMessage: Boolean,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val canSend = messageText.isNotBlank() && canSendMessage

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
            placeholder = { Text(stringResource(R.string.ai_bot_message_input_placeholder)) },
            maxLines = 4,
        )

        IconButton(
            onClick = onSendClick,
            enabled = canSend
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.ai_bot_send_button_content_description),
                tint = if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: BotMessage, resources: android.content.res.Resources) {
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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
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
                    text = formatRelativeTime(message.date, resources),
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

@Composable
private fun TypingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDot(delay = 0)
                TypingDot(delay = 150)
                TypingDot(delay = 300)
            }
        }
    }
}

@Composable
private fun TypingDot(delay: Int) {
    var alpha by remember { mutableStateOf(0.3f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        while (true) {
            alpha = 1f
            kotlinx.coroutines.delay(600)
            alpha = 0.3f
            kotlinx.coroutines.delay(600)
        }
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                shape = RoundedCornerShape(50)
            )
            .padding(4.dp)
    )
}

@Preview(showBackground = true, name = "Conversation Detail")
@Composable
private fun ConversationDetailScreenPreview() {
    val sampleConversation = generateSampleBotConversations()[0]

    AppThemeM3(isDarkTheme = false) {
        ConversationDetailScreen(
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { }
        )
    }
}

@Preview(showBackground = true, name = "Conversation Detail - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationDetailScreenPreviewDark() {
    val sampleConversation = generateSampleBotConversations()[0]

    AppThemeM3(isDarkTheme = true) {
        ConversationDetailScreen(
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { }
        )
    }
}

@Preview(showBackground = true, name = "Conversation Detail")
@Composable
private fun ConversationDetailScreenWordPressPreview() {
    val sampleConversation = generateSampleBotConversations()[0]

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        ConversationDetailScreen(
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { }
        )
    }
}

@Preview(showBackground = true, name = "Conversation Detail - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationDetailScreenPreviewWordPressDark() {
    val sampleConversation = generateSampleBotConversations()[0]

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        ConversationDetailScreen(
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { }
        )
    }
}
