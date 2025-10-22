package org.wordpress.android.support.he.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.util.generateSampleHESupportConversations
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HEConversationDetailScreen(
    conversation: SupportConversation,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val resources = LocalResources.current

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "",
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackClick
            )
        },
        bottomBar = {
            ReplyButton(
                onClick = {
                    showBottomSheet = true
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
                ConversationHeader(
                    messageCount = conversation.messages.size,
                    lastUpdated = formatRelativeTime(conversation.lastMessageSentAt, resources)
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
                    authorName = message.authorName,
                    messageText = message.text,
                    timestamp = formatRelativeTime(message.createdAt, resources),
                    isUserMessage = message.authorIsUser
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showBottomSheet) {
        ReplyBottomSheet(
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                }
            },
            onSend = { message, includeAppLogs ->
                /* Placeholder for send functionality */
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                }
            }
        )
    }
}

@Composable
private fun ConversationHeader(
    messageCount: Int,
    lastUpdated: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun MessageItem(
    authorName: String,
    messageText: String,
    timestamp: String,
    isUserMessage: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isUserMessage) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
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
                    text = authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isUserMessage) FontWeight.Bold else FontWeight.Normal,
                    color = if (isUserMessage) {
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
                text = messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReplyButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.he_support_reply_button),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplyBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSend: (String, Boolean) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var includeAppLogs by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = stringResource(R.string.he_support_reply_button),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = { onSend(messageText, includeAppLogs) },
                    enabled = messageText.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.he_support_send_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            TicketMainContentView(
                messageText = messageText,
                includeAppLogs = includeAppLogs,
                onMessageChanged = { message -> messageText = message },
                onIncludeAppLogsChanged = { checked -> includeAppLogs = checked },
            )
        }
    }
}

@Preview(showBackground = true, name = "HE Conversation Detail")
@Composable
private fun HEConversationDetailScreenPreview() {
    val sampleConversation = generateSampleHESupportConversations()[0]

    AppThemeM3(isDarkTheme = false) {
        HEConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Conversation Detail - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HEConversationDetailScreenPreviewDark() {
    val sampleConversation = generateSampleHESupportConversations()[0]

    AppThemeM3(isDarkTheme = true) {
        HEConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Conversation Detail - WordPress")
@Composable
private fun HEConversationDetailScreenWordPressPreview() {
    val sampleConversation = generateSampleHESupportConversations()[0]

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        HEConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Conversation Detail - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HEConversationDetailScreenPreviewWordPressDark() {
    val sampleConversation = generateSampleHESupportConversations()[0]

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        HEConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}
