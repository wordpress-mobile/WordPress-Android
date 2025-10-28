package org.wordpress.android.support.common.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wordpress.android.support.common.model.Conversation
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Conversation> ConversationsListScreen(
    modifier: Modifier = Modifier,
    title: String,
    addConversationContentDescription: String,
    snackbarHostState: SnackbarHostState,
    conversations: List<T>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onCreateNewConversationClick: () -> Unit,
    onRefresh: () -> Unit,
    conversationListItem: @Composable (T) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainTopAppBar(
                title = title,
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackClick,
                actions = {
                    IconButton(onClick = { onCreateNewConversationClick() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = addConversationContentDescription
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize()
        ) {
            ConversationsList(
                modifier = Modifier.padding(contentPadding),
                conversations = conversations,
                isLoading = isLoading,
                onCreateNewConversationClick = onCreateNewConversationClick,
                conversationListItem = conversationListItem,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Conversation> ConversationsList(
    modifier: Modifier,
    conversations: List<T>,
    isLoading: Boolean,
    onCreateNewConversationClick: () -> Unit,
    conversationListItem: @Composable (T) -> Unit
) {
    if (conversations.isEmpty() && !isLoading) {
        EmptyConversationsView(
            modifier = modifier,
            onCreateNewConversationClick = onCreateNewConversationClick
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(
                items = conversations,
                key = { it.getConversationId() }
            ) { conversation ->
                conversationListItem(conversation)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
