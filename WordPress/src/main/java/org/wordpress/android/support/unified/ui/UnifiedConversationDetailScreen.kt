package org.wordpress.android.support.unified.ui

import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.he.model.AttachmentState
import org.wordpress.android.support.he.ui.TicketMainContentView
import org.wordpress.android.support.he.util.AttachmentActionsListener
import org.wordpress.android.support.unified.model.UnifiedAttachment
import org.wordpress.android.support.unified.model.UnifiedConversation
import org.wordpress.android.support.unified.model.UnifiedMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedConversationDetailScreen(
    snackbarHostState: SnackbarHostState,
    conversation: UnifiedConversation,
    isLoading: Boolean,
    isSendingReply: Boolean,
    onBackClick: () -> Unit,
    onSendReply: (String) -> Unit,
) {
    var messageText by remember { mutableStateOf("") }
    var replyText by rememberSaveable { mutableStateOf("") }
    var showReplySheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val resources = LocalResources.current
    val isBot = conversation.isBot
    val isBotTyping = isBot && isSendingReply

    // Scroll to bottom when a new message is added or when the bot starts "typing".
    LaunchedEffect(conversation.id, conversation.messages.lastOrNull()?.id, isBotTyping) {
        if (conversation.messages.isNotEmpty() || isBotTyping) {
            listState.scrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = conversation.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.unified_support_back_button_content_description)
                        )
                    }
                }
            )
        },
        bottomBar = {
            when {
                isBot -> {
                    if (conversation.canAcceptReply) {
                        ChatInputBar(
                            messageText = messageText,
                            canSendMessage = !isSendingReply && !isLoading,
                            onMessageTextChange = { messageText = it },
                            onSendClick = {
                                if (messageText.isNotBlank()) {
                                    onSendReply(messageText)
                                    messageText = ""
                                }
                            }
                        )
                    }
                }
                conversation.canAcceptReply -> {
                    Box(modifier = Modifier.navigationBarsPadding()) {
                        ReplyButton(
                            enabled = !isLoading,
                            onClick = { showReplySheet = true }
                        )
                    }
                }
                else -> {
                    Box(modifier = Modifier.navigationBarsPadding()) {
                        ClosedConversationBanner()
                    }
                }
            }
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
                items(
                    items = conversation.messages,
                    key = { message -> message.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        timestamp = formatRelativeTime(message.createdAt, resources)
                    )
                }

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

    if (showReplySheet) {
        UnifiedReplyBottomSheet(
            sheetState = sheetState,
            isSending = isSendingReply,
            messageText = replyText,
            onMessageChange = { replyText = it },
            onDismiss = {
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { showReplySheet = false }
            },
            onSend = { message ->
                onSendReply(message)
                replyText = ""
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { showReplySheet = false }
            },
        )
    }
}

@Composable
private fun MessageBubble(message: UnifiedMessage, timestamp: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (!message.isUser && message.authorName.isNotBlank()) {
            Text(
                text = message.authorName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.formattedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )

                if (message.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.attachments.forEach { attachment ->
                        AttachmentRow(attachment)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isUser) {
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
private fun AttachmentRow(attachment: UnifiedAttachment) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        if (attachment.isImage) {
            AsyncImage(
                model = attachment.url,
                contentDescription = attachment.filename,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = attachment.filename,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            attachment.botCitationScore?.let { score ->
                Text(
                    text = stringResource(
                        R.string.unified_support_attachment_match_score,
                        (score * PERCENT_MULTIPLIER).toInt()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.unified_support_message_input_placeholder)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        IconButton(
            onClick = onSendClick,
            enabled = canSend
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.unified_support_send_button_content_description),
                tint = if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedReplyBottomSheet(
    sheetState: SheetState,
    isSending: Boolean,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSending
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = stringResource(R.string.he_support_reply_button),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                TextButton(
                    onClick = { onSend(messageText) },
                    enabled = messageText.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.he_support_send_button),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            TicketMainContentView(
                messageText = messageText,
                includeAppLogs = false,
                onMessageChanged = onMessageChange,
                onIncludeAppLogsChanged = {},
                enabled = !isSending,
                attachmentsEnabled = false,
                appLogsEnabled = false,
                attachmentState = AttachmentState(),
                attachmentActionsListener = NoOpAttachmentActionsListener
            )
        }
    }
}

@Suppress("EmptyFunctionBlock")
private object NoOpAttachmentActionsListener : AttachmentActionsListener {
    override fun onAddImageClick() {}
    override fun onRemoveImage(uri: Uri) {}
}

@Composable
private fun ReplyButton(
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val replyButtonLabel = stringResource(R.string.he_support_reply_button)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = replyButtonLabel },
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = replyButtonLabel,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ClosedConversationBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_outline_white_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.he_support_conversation_closed_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun TypingIndicatorBubble() {
    val typingDescription = stringResource(R.string.unified_support_bot_typing_content_description)
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
                .semantics {
                    contentDescription = typingDescription
                }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDot(delay = 0)
                TypingDot(delay = TYPING_DOT_DELAY_STEP)
                TypingDot(delay = TYPING_DOT_DELAY_STEP * 2)
            }
        }
    }
}

@Composable
private fun TypingDot(delay: Int) {
    var alpha by remember { mutableStateOf(TYPING_DOT_MIN_ALPHA) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        while (true) {
            alpha = 1f
            kotlinx.coroutines.delay(TYPING_DOT_PULSE_MS)
            alpha = TYPING_DOT_MIN_ALPHA
            kotlinx.coroutines.delay(TYPING_DOT_PULSE_MS)
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

private const val PERCENT_MULTIPLIER = 100
private const val TYPING_DOT_DELAY_STEP = 150
private const val TYPING_DOT_PULSE_MS = 600L
private const val TYPING_DOT_MIN_ALPHA = 0.3f
