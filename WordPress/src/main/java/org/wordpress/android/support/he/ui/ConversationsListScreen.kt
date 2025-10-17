package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.support.aibot.util.formatRelativeTime
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.util.generateSampleSupportConversations
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    conversations: StateFlow<List<SupportConversation>>,
    onConversationClick: (SupportConversation) -> Unit,
    onBackClick: () -> Unit,
    onCreateNewConversationClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.he_support_conversations_title)) },
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
                            contentDescription = stringResource(
                                R.string.he_support_new_conversation_content_description
                            )
                        )
                    }
                }
            )
        }
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
    conversations: StateFlow<List<SupportConversation>>,
    onConversationClick: (SupportConversation) -> Unit
) {
    val conversationsList by conversations.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.padding(top = 4.dp))
        }

        items(conversationsList) { conversation ->
            ConversationCard(
                conversation = conversation,
                onClick = { onConversationClick(conversation) }
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: SupportConversation,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = conversation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = formatRelativeTime(conversation.lastMessageSentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List")
@Composable
private fun ConversationsScreenPreview() {
    val sampleConversations = MutableStateFlow(generateSampleSupportConversations())

    AppThemeM3(isDarkTheme = false) {
        ConversationsListScreen(
            conversations = sampleConversations.asStateFlow(),
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationsScreenPreviewDark() {
    val sampleConversations = MutableStateFlow(generateSampleSupportConversations())

    AppThemeM3(isDarkTheme = true) {
        ConversationsListScreen(
            conversations = sampleConversations.asStateFlow(),
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List - WordPress")
@Composable
private fun ConversationsScreenWordPressPreview() {
    val sampleConversations = MutableStateFlow(generateSampleSupportConversations())

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        ConversationsListScreen(
            conversations = sampleConversations.asStateFlow(),
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversations List - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationsScreenPreviewWordPressDark() {
    val sampleConversations = MutableStateFlow(generateSampleSupportConversations())

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        ConversationsListScreen(
            conversations = sampleConversations.asStateFlow(),
            onConversationClick = { },
            onBackClick = { },
            onCreateNewConversationClick = { }
        )
    }
}
