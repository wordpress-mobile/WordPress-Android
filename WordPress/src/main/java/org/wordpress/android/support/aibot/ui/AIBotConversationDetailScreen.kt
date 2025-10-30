package org.wordpress.android.support.aibot.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.aibot.util.generateSampleBotConversations
import org.wordpress.android.ui.compose.theme.AppThemeM3

private const val PAGINATION_TRIGGER_THRESHOLD = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBotConversationDetailScreen(
    snackbarHostState: SnackbarHostState,
    conversation: BotConversation,
    isLoading: Boolean,
    isBotTyping: Boolean,
    isLoadingOlderMessages: Boolean,
    hasMorePages: Boolean,
    canSendMessage: Boolean,
    userName: String,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onLoadOlderMessages: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages are added at the end (not when loading older messages at the beginning)
    // Only scroll to bottom when:
    // 1. The last message changes (new message added at the end)
    // 2. Bot starts typing
    // 3. We're not loading older messages (which adds messages at the beginning)
    LaunchedEffect(conversation.id, conversation.messages.lastOrNull()?.id, isBotTyping) {
        if ((conversation.messages.isNotEmpty() || isBotTyping) && !isLoadingOlderMessages) {
            listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    // Detect when user scrolls near the top to load older messages
    LaunchedEffect(listState, isLoadingOlderMessages, isLoading, hasMorePages) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                val shouldLoadMore = !isLoadingOlderMessages && firstVisibleIndex <= PAGINATION_TRIGGER_THRESHOLD

                if (shouldLoadMore && !isLoading && hasMorePages) {
                    onLoadOlderMessages()
                }
            }
    }

    val resources = LocalResources.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show loading indicator at top when loading older messages
                if (isLoadingOlderMessages) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Only show welcome header when we're at the beginning (no more pages to load)
                if (!hasMorePages) {
                    item {
                        WelcomeHeader(userName)
                    }
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
    val greeting = stringResource(R.string.ai_bot_welcome_greeting, userName)
    val message = stringResource(R.string.ai_bot_welcome_message)
    val welcomeDescription = "$greeting. $message"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clearAndSetSemantics {
                contentDescription = welcomeDescription
            },
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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
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
    val messageInputLabel = stringResource(R.string.ai_bot_message_input_placeholder)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = messageInputLabel },
            placeholder = { Text(messageInputLabel) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
    val timestamp = formatRelativeTime(message.date, resources)
    val author = stringResource(if (message.isWrittenByUser) R.string.ai_bot_you else R.string.ai_bot_support_bot)
    val messageDescription = "$author, $timestamp. ${message.formattedText}"

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
                .clearAndSetSemantics {
                    contentDescription = messageDescription
                }
        ) {
            Column {
                Text(
                    text = message.formattedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (message.isWrittenByUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timestamp,
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
                .semantics { contentDescription = "AI Bot is typing" }
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
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false) {
        AIBotConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            isLoadingOlderMessages = false,
            hasMorePages = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { },
            onLoadOlderMessages = { }
        )
    }
}

@Preview(showBackground = true, name = "Conversation Detail - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationDetailScreenPreviewDark() {
    val sampleConversation = generateSampleBotConversations()[0]
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true) {
        AIBotConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            isLoadingOlderMessages = false,
            hasMorePages = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { },
            onLoadOlderMessages = { }
        )
    }
}

@Preview(showBackground = true, name = "Conversation Detail")
@Composable
private fun ConversationDetailScreenWordPressPreview() {
    val sampleConversation = generateSampleBotConversations()[0]
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        AIBotConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            isLoadingOlderMessages = false,
            hasMorePages = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { },
            onLoadOlderMessages = { }
        )
    }
}

@Preview(showBackground = true, name = "Conversation Detail - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationDetailScreenPreviewWordPressDark() {
    val sampleConversation = generateSampleBotConversations()[0]
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        AIBotConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            userName = "UserName",
            conversation = sampleConversation,
            isLoading = false,
            isBotTyping = false,
            isLoadingOlderMessages = false,
            hasMorePages = false,
            canSendMessage = true,
            onBackClick = { },
            onSendMessage = { },
            onLoadOlderMessages = { }
        )
    }
}
