package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import org.wordpress.android.R
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.he.model.AttachmentState
import org.wordpress.android.support.he.model.MessageSendResult
import org.wordpress.android.support.he.model.AttachmentType
import org.wordpress.android.support.he.model.SupportAttachment
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import org.wordpress.android.support.he.util.AttachmentActionsListener
import org.wordpress.android.support.he.util.generateSampleHESupportConversations
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HEConversationDetailScreen(
    snackbarHostState: SnackbarHostState,
    conversation: SupportConversation,
    isLoading: Boolean = false,
    isSendingMessage: Boolean = false,
    messageSendResult: MessageSendResult? = null,
    onBackClick: () -> Unit,
    onSendMessage: (message: String, includeAppLogs: Boolean) -> Unit,
    onClearMessageSendResult: () -> Unit = {},
    attachmentState: AttachmentState = AttachmentState(),
    attachmentActionsListener: AttachmentActionsListener,
    onDownloadAttachment: (SupportAttachment) -> Unit = {},
    videoUrlResolver: org.wordpress.android.support.he.util.VideoUrlResolver? = null
) {
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val resources = LocalResources.current

    // Save draft message state to restore when reopening the bottom sheet
    var draftMessageText by remember { mutableStateOf("") }
    var draftIncludeAppLogs by remember { mutableStateOf(false) }

    // State for fullscreen attachment preview (image or video)
    var previewAttachment by remember { mutableStateOf<SupportAttachment?>(null) }

    // Scroll to bottom when conversation changes or new messages arrive
    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainTopAppBar(
                title = "",
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackClick
            )
        },
        bottomBar = {
            ReplyButton(
                enabled = !isLoading,
                onClick = {
                    showBottomSheet = true
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
            item {
                ConversationHeader(
                    messageCount = conversation.messages.size,
                    lastUpdated = formatRelativeTime(conversation.lastMessageSentAt, resources),
                    isLoading = isLoading
                )
            }

            item {
                ConversationTitleCard(title = conversation.title)
            }

            items(
                items = conversation.messages,
                key = { it.id }
            ) { message ->
                MessageItem(
                    message = message,
                    timestamp = formatRelativeTime(message.createdAt, resources),
                    onPreviewAttachment = { attachment -> previewAttachment = attachment },
                    onDownloadAttachment = onDownloadAttachment
                )
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

    if (showBottomSheet) {
        // Close the sheet when sending completes
        LaunchedEffect(messageSendResult) {
            if (messageSendResult != null) {
                // Clear draft only on success
                if (messageSendResult is MessageSendResult.Success) {
                    draftMessageText = ""
                    draftIncludeAppLogs = false
                }

                // Dismiss sheet and clear result for both success and failure
                onClearMessageSendResult()
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                }
            }
        }

        HEConversationReplyBottomSheet(
            sheetState = sheetState,
            isSending = isSendingMessage,
            initialMessageText = draftMessageText,
            initialIncludeAppLogs = draftIncludeAppLogs,
            onDismiss = { currentMessage, currentIncludeAppLogs ->
                draftMessageText = currentMessage
                draftIncludeAppLogs = currentIncludeAppLogs
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                }
            },
            onSend = { message, includeAppLogs ->
                draftMessageText = message
                onSendMessage(message, includeAppLogs)
            },
            onMessageSentSuccessfully = {
                // Clear draft after successful send
                draftMessageText = ""
                draftIncludeAppLogs = false
                onClearMessageSendResult()
            },
            attachmentState = attachmentState,
            attachmentActionsListener = attachmentActionsListener
        )
    }

    // Show fullscreen attachment preview based on type
    previewAttachment?.let { attachment ->
        when (attachment.type) {
            AttachmentType.Image -> {
                AttachmentFullscreenImagePreview(
                    imageUrl = attachment.url,
                    onDismiss = { previewAttachment = null },
                    onDownload = {
                        onDownloadAttachment(attachment)
                    }
                )
            }
            AttachmentType.Video -> {
                AttachmentFullscreenVideoPlayer(
                    videoUrl = attachment.url,
                    onDismiss = { previewAttachment = null },
                    onDownload = {
                        onDownloadAttachment(attachment)
                    },
                    videoUrlResolver = videoUrlResolver
                )
            }
            else -> {
                // For other types (documents, etc.), do nothing
                // They should only be downloadable, not previewable
            }
        }
    }
}

@Composable
private fun ConversationHeader(
    messageCount: Int,
    lastUpdated: String,
    isLoading: Boolean = false
) {
    val headerDescription = if (!isLoading) {
        "${stringResource(R.string.he_support_message_count, messageCount)}. " +
                stringResource(R.string.he_support_last_updated, lastUpdated)
    } else {
        stringResource(R.string.he_support_last_updated, lastUpdated)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clearAndSetSemantics {
                contentDescription = headerDescription
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_comment_white_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.he_support_message_count, messageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(modifier = Modifier.size(0.dp))
        }

        Text(
            text = stringResource(R.string.he_support_last_updated, lastUpdated),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConversationTitleCard(title: String) {
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
private fun MessageItem(
    message: SupportMessage,
    timestamp: String,
    onPreviewAttachment: (SupportAttachment) -> Unit,
    onDownloadAttachment: (SupportAttachment) -> Unit
) {
    val messageDescription = "${message.authorName}, $timestamp. ${message.formattedText}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (message.authorIsUser) {
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
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (message.authorIsUser) FontWeight.Bold else FontWeight.Normal,
                    color = if (message.authorIsUser) {
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

            // Display attachments if present
            if (message.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AttachmentsList(
                    attachments = message.attachments,
                    onPreviewAttachment = onPreviewAttachment,
                    onDownloadAttachment = onDownloadAttachment
                )
            }
        }
    }
}

@Composable
private fun AttachmentsList(
    attachments: List<SupportAttachment>,
    onPreviewAttachment: (SupportAttachment) -> Unit,
    onDownloadAttachment: (SupportAttachment) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            AttachmentItem(
                attachment = attachment,
                onClick = {
                    when (attachment.type) {
                        AttachmentType.Image, AttachmentType.Video -> onPreviewAttachment(attachment)
                        else -> onDownloadAttachment(attachment)
                    }
                }
            )
        }
    }
}

@Composable
private fun AttachmentItem(
    attachment: SupportAttachment,
    onClick: () -> Unit
) {
    val iconRes = when (attachment.type) {
        AttachmentType.Image -> R.drawable.ic_image_white_24dp
        AttachmentType.Video -> R.drawable.ic_video_camera_white_24dp
        AttachmentType.Other -> R.drawable.ic_pages_white_24dp
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
        if (attachment.type == AttachmentType.Image ||
            attachment.type == AttachmentType.Video) {
            // Show image/video preview for image and video attachments
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(attachment.url)
                    .crossfade(true)
                    .apply {
                        if (attachment.type == AttachmentType.Video) {
                            decoderFactory(VideoFrameDecoder.Factory())
                            videoFrameMillis(0) // Get first frame
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
                    // Show icon if image/video fails to load
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            )

            // Add play icon overlay for videos
            if (attachment.type == AttachmentType.Video) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = stringResource(R.string.photo_picker_thumbnail_desc),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                    tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            }
        } else {
            // Show icon for non-image/video attachments
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

@Preview(showBackground = true, name = "HE Conversation Detail")
@Composable
private fun HEConversationDetailScreenPreview() {
    val sampleConversation = generateSampleHESupportConversations()[0]
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false) {
        HEConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            conversation = sampleConversation,
            onBackClick = { },
            onSendMessage = { _, _ -> },
            attachmentActionsListener = object : AttachmentActionsListener {
                override fun onAddImageClick() {
                    // stub
                }
                override fun onRemoveImage(uri: Uri) {
                    // stub
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "HE Conversation Detail - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HEConversationDetailScreenPreviewDark() {
    val sampleConversation = generateSampleHESupportConversations()[0]
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true) {
        HEConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            conversation = sampleConversation,
            onBackClick = { },
            onSendMessage = { _, _ -> },
            attachmentActionsListener = object : AttachmentActionsListener {
                override fun onAddImageClick() {
                    // stub
                }
                override fun onRemoveImage(uri: Uri) {
                    // stub
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "HE Conversation Detail - WordPress")
@Composable
private fun HEConversationDetailScreenWordPressPreview() {
    val sampleConversation = generateSampleHESupportConversations()[0]
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        HEConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            conversation = sampleConversation,
            onBackClick = {
                // stub
            },
            onSendMessage = { _, _ -> },
            attachmentActionsListener = object : AttachmentActionsListener {
                override fun onAddImageClick() {
                    // stub
                }
                override fun onRemoveImage(uri: Uri) {
                    // stub
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "HE Conversation Detail - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HEConversationDetailScreenPreviewWordPressDark() {
    val sampleConversation = generateSampleHESupportConversations()[0]
    val snackbarHostState = remember { SnackbarHostState() }

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        HEConversationDetailScreen(
            snackbarHostState = snackbarHostState,
            isLoading = true,
            conversation = sampleConversation,
            onBackClick = { },
            onSendMessage = { _, _ -> },
            attachmentActionsListener = object : AttachmentActionsListener {
                override fun onAddImageClick() {
                    // stub
                }
                override fun onRemoveImage(uri: Uri) {
                    // stub
                }
            }
        )
    }
}
