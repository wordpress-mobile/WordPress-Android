package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.util.generateSampleSupportConversations
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversation: SupportConversation,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversation.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.ai_bot_back_button_content_description)
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Conversation detail screen - Coming soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "HE Support Conversation Detail")
@Composable
private fun ConversationDetailScreenPreview() {
    val sampleConversation = generateSampleSupportConversations()[0]

    AppThemeM3(isDarkTheme = false) {
        ConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversation Detail - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationDetailScreenPreviewDark() {
    val sampleConversation = generateSampleSupportConversations()[0]

    AppThemeM3(isDarkTheme = true) {
        ConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversation Detail - WordPress")
@Composable
private fun ConversationDetailScreenWordPressPreview() {
    val sampleConversation = generateSampleSupportConversations()[0]

    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        ConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}

@Preview(showBackground = true, name = "HE Support Conversation Detail - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConversationDetailScreenPreviewWordPressDark() {
    val sampleConversation = generateSampleSupportConversations()[0]

    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        ConversationDetailScreen(
            conversation = sampleConversation,
            onBackClick = { }
        )
    }
}
