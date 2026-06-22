package org.wordpress.android.support.unified.ui

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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextAlign
import org.wordpress.android.R
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.support.unified.util.formatRelativeTime
import org.wordpress.android.support.unified.model.AttachmentState
import org.wordpress.android.support.unified.model.AttachmentType
import org.wordpress.android.support.unified.model.ConversationReplyFormState
import org.wordpress.android.support.unified.model.VideoDownloadState
import org.wordpress.android.support.unified.util.AttachmentActionsListener
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
    userName: String,
    onBackClick: () -> Unit,
    onSendReply: (String, Boolean) -> Unit,
    onDownloadAttachment: (UnifiedAttachment) -> Unit,
    onLinkClick: (String) -> Unit,
    authorizationHeader: String,
    videoDownloadState: VideoDownloadState,
    onStartVideoDownload: (String) -> Unit,
    onResetVideoDownloadState: () -> Unit,
    replyFormState: ConversationReplyFormState,
    onReplyMessageChange: (String) -> Unit,
    onReplyIncludeAppLogsChange: (Boolean) -> Unit,
    onReplyBottomSheetVisibilityChange: (Boolean) -> Unit,
    attachmentActionsListener: AttachmentActionsListener,
) {
    var previewAttachment by remember { mutableStateOf<UnifiedAttachment?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val resources = LocalResources.current
    val isBot = conversation.isBot
    val isBotTyping = isBot && isSendingReply

    // Keep the conversation pinned to its last item whenever a message is added or the bot's typing
    // indicator appears/disappears. We scroll in reaction to layoutInfo.totalItemsCount rather than
    // deriving the index from the data: the count is reported by the lazy layout once the new items
    // are laid out (avoiding the composition lag that made an eager scroll miss them), and it stays
    // correct regardless of how many headers/footers the list composition adds.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .collect { itemCount ->
                if (itemCount > 0) listState.scrollToItem(itemCount - 1)
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    // Bots intentionally have no title (it's always empty); the welcome header in the
                    // body stands in for it. HE conversations show their title in the body (title card),
                    // like the old screen, so the top bar is left empty in both cases.
                    Text(
                        text = if (isBot) conversation.title else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            ConversationBottomBar(
                isBot = isBot,
                canAcceptReply = conversation.canAcceptReply,
                // The chat input is backed by the shared reply form state so the typed text is
                // retained on send failure (the VM clears it only on success) and survives
                // configuration changes via the ViewModel.
                messageText = replyFormState.message,
                canSendMessage = !isSendingReply && !isLoading,
                onMessageTextChange = onReplyMessageChange,
                onSendMessage = { message -> onSendReply(message, false) },
                replyEnabled = !isLoading,
                onReplyClick = { onReplyBottomSheetVisibilityChange(true) }
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
                if (isBot) {
                    // The whole conversation is loaded at once, so the beginning is always
                    // visible — show the welcome header like the Ask the Bots screen does.
                    item {
                        WelcomeHeader(userName)
                    }

                    items(
                        items = conversation.messages,
                        key = { message -> message.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            timestamp = formatRelativeTime(message.createdAt, resources),
                            onLinkClick = onLinkClick
                        )
                    }

                    if (isBotTyping) {
                        item {
                            TypingIndicatorBubble()
                        }
                    }
                } else {
                    item {
                        UnifiedConversationHeader(
                            status = conversation.status,
                            lastUpdated = formatRelativeTime(conversation.updatedAt, resources)
                        )
                    }

                    item {
                        UnifiedConversationTitleCard(title = conversation.title)
                    }

                    items(
                        items = conversation.messages,
                        key = { message -> message.id }
                    ) { message ->
                        UnifiedMessageItem(
                            message = message,
                            timestamp = formatRelativeTime(message.createdAt, resources),
                            onPreviewAttachment = { previewAttachment = it },
                            onDownloadAttachment = onDownloadAttachment,
                            onLinkClick = onLinkClick,
                            authorizationHeader = authorizationHeader,
                        )
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

    if (replyFormState.isBottomSheetVisible) {
        val hideSheet = {
            scope.launch { sheetState.hide() }
                .invokeOnCompletion { onReplyBottomSheetVisibilityChange(false) }
            Unit
        }
        UnifiedReplyBottomSheet(
            sheetState = sheetState,
            isSending = isSendingReply,
            messageText = replyFormState.message,
            includeAppLogs = replyFormState.includeAppLogs,
            onMessageChange = onReplyMessageChange,
            onIncludeAppLogsChange = onReplyIncludeAppLogsChange,
            onDismiss = hideSheet,
            onSend = { message, includeAppLogs ->
                onSendReply(message, includeAppLogs)
                hideSheet()
            },
            attachmentState = replyFormState.attachmentState,
            attachmentActionsListener = attachmentActionsListener,
        )
    }

    previewAttachment?.let { attachment ->
        AttachmentPreviewOverlay(
            attachment = attachment,
            authorizationHeader = authorizationHeader,
            videoDownloadState = videoDownloadState,
            onStartVideoDownload = onStartVideoDownload,
            onResetVideoDownloadState = onResetVideoDownloadState,
            onDismiss = { previewAttachment = null },
            onDownload = { onDownloadAttachment(attachment) },
        )
    }
}

@Composable
private fun ConversationBottomBar(
    isBot: Boolean,
    canAcceptReply: Boolean,
    messageText: String,
    canSendMessage: Boolean,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    replyEnabled: Boolean,
    onReplyClick: () -> Unit,
) {
    when {
        isBot -> {
            if (canAcceptReply) {
                ChatInputBar(
                    messageText = messageText,
                    canSendMessage = canSendMessage,
                    onMessageTextChange = onMessageTextChange,
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                        }
                    }
                )
            }
        }
        canAcceptReply -> {
            Box(modifier = Modifier.navigationBarsPadding()) {
                ReplyButton(
                    enabled = replyEnabled,
                    onClick = onReplyClick
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

@Composable
private fun AttachmentPreviewOverlay(
    attachment: UnifiedAttachment,
    authorizationHeader: String,
    videoDownloadState: VideoDownloadState,
    onStartVideoDownload: (String) -> Unit,
    onResetVideoDownloadState: () -> Unit,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    when (attachment.type) {
        AttachmentType.Image -> {
            AttachmentFullscreenImagePreview(
                imageUrl = attachment.url,
                authorizationHeader = authorizationHeader,
                onDismiss = onDismiss,
                onDownload = onDownload
            )
        }
        AttachmentType.Video -> {
            AttachmentFullscreenVideoPlayer(
                videoUrl = attachment.url,
                downloadState = videoDownloadState,
                onStartVideoDownload = onStartVideoDownload,
                onResetVideoDownloadState = onResetVideoDownloadState,
                onDismiss = onDismiss,
                onDownload = onDownload,
            )
        }
        else -> Unit
    }
}

@Composable
private fun MessageBubble(
    message: UnifiedMessage,
    timestamp: String,
    onLinkClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (!message.isUser) {
            Text(
                text = stringResource(R.string.unified_support_status_bot),
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
                        AttachmentRow(attachment, onLinkClick)
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
private fun AttachmentRow(attachment: UnifiedAttachment, onLinkClick: (String) -> Unit) {
    val isLink = attachment.type == AttachmentType.Link
    val linkModifier = if (isLink) {
        val linkDescription = attachmentLinkDescription(attachment)
        Modifier
            .clickable(role = Role.Button) { onLinkClick(attachment.url) }
            .semantics { contentDescription = linkDescription }
    } else {
        Modifier
    }
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
                color = if (isLink) MaterialTheme.colorScheme.primary else Color.Unspecified,
                textDecoration = if (isLink) TextDecoration.Underline else null,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .then(linkModifier),
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
                contentDescription = stringResource(R.string.send),
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
    includeAppLogs: Boolean,
    onMessageChange: (String) -> Unit,
    onIncludeAppLogsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSend: (String, Boolean) -> Unit,
    attachmentState: AttachmentState,
    attachmentActionsListener: AttachmentActionsListener,
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
                    onClick = { onSend(messageText, includeAppLogs) },
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
                includeAppLogs = includeAppLogs,
                onMessageChanged = onMessageChange,
                onIncludeAppLogsChanged = onIncludeAppLogsChange,
                enabled = !isSending,
                attachmentState = attachmentState,
                attachmentActionsListener = attachmentActionsListener
            )
        }
    }
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
    var alpha by remember { mutableFloatStateOf(TYPING_DOT_MIN_ALPHA) }

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

@Composable
private fun UnifiedConversationHeader(status: String, lastUpdated: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConversationStatusBadge(status = status)

        Text(
            text = stringResource(R.string.he_support_last_updated, lastUpdated),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UnifiedConversationTitleCard(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun UnifiedMessageItem(
    message: UnifiedMessage,
    timestamp: String,
    onPreviewAttachment: (UnifiedAttachment) -> Unit,
    onDownloadAttachment: (UnifiedAttachment) -> Unit,
    onLinkClick: (String) -> Unit,
    authorizationHeader: String,
) {
    val messageDescription = "${message.authorName}, $timestamp. ${message.formattedText}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
            .clearAndSetSemantics {
                contentDescription = messageDescription
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (message.isUser) FontWeight.Bold else FontWeight.Normal,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.formattedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            if (message.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UnifiedAttachmentsList(
                    attachments = message.attachments,
                    onPreviewAttachment = onPreviewAttachment,
                    onDownloadAttachment = onDownloadAttachment,
                    onLinkClick = onLinkClick,
                    authorizationHeader = authorizationHeader,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnifiedAttachmentsList(
    attachments: List<UnifiedAttachment>,
    onPreviewAttachment: (UnifiedAttachment) -> Unit,
    onDownloadAttachment: (UnifiedAttachment) -> Unit,
    onLinkClick: (String) -> Unit,
    authorizationHeader: String,
) {
    // Link attachments (text/html web pages) are rendered as tappable links rather than
    // file cards, since they point to web articles rather than downloadable files.
    val (links, files) = attachments.partition { it.type == AttachmentType.Link }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        links.forEach { attachment ->
            UnifiedAttachmentLink(attachment, onLinkClick)
        }

        if (files.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                files.forEach { attachment ->
                    UnifiedAttachmentItem(
                        attachment = attachment,
                        onClick = {
                            when (attachment.type) {
                                AttachmentType.Image, AttachmentType.Video -> onPreviewAttachment(attachment)
                                else -> onDownloadAttachment(attachment)
                            }
                        },
                        authorizationHeader = authorizationHeader,
                    )
                }
            }
        }
    }
}

@Composable
private fun attachmentLinkDescription(attachment: UnifiedAttachment): String =
    stringResource(
        R.string.unified_support_attachment_link_content_description,
        attachment.filename
    )

@Composable
private fun UnifiedAttachmentLink(attachment: UnifiedAttachment, onLinkClick: (String) -> Unit) {
    val linkDescription = attachmentLinkDescription(attachment)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                onLinkClick(attachment.url)
            }
            .padding(vertical = 4.dp)
            .semantics { contentDescription = linkDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = attachment.filename,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UnifiedAttachmentItem(
    attachment: UnifiedAttachment,
    onClick: () -> Unit,
    authorizationHeader: String,
) {
    // Link attachments are rendered as links by UnifiedAttachmentLink, not as cards, so they
    // fall back to the generic file icon here.
    val iconRes = when (attachment.type) {
        AttachmentType.Image -> R.drawable.ic_image_white_24dp
        AttachmentType.Video -> R.drawable.ic_video_camera_white_24dp
        AttachmentType.Link, AttachmentType.Other -> R.drawable.ic_pages_white_24dp
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (attachment.type == AttachmentType.Image || attachment.type == AttachmentType.Video) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(attachment.url)
                    .crossfade(true)
                    .apply {
                        if (attachment.type == AttachmentType.Video) {
                            decoderFactory(VideoFrameDecoder.Factory())
                            videoFrameMillis(0) // First frame as thumbnail
                        }
                    }
                    .apply {
                        // Only attach the WP.com auth token to trusted WP.com hosts, so the bearer
                        // token is never leaked to an unexpected attachment host.
                        if (WPUrlUtils.safeToAddWordPressComAuthToken(attachment.url)) {
                            addHeader(UnifiedSupportActivity.AUTHORIZATION_TAG, authorizationHeader)
                        }
                    }
                    .build(),
                contentDescription = attachment.filename,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            )

            if (attachment.type == AttachmentType.Video) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                    tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            }
        } else {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
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
                text = greeting,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private const val PERCENT_MULTIPLIER = 100
private const val TYPING_DOT_DELAY_STEP = 150
private const val TYPING_DOT_PULSE_MS = 600L
private const val TYPING_DOT_MIN_ALPHA = 0.3f
