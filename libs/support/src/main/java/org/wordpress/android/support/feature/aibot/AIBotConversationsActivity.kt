package org.wordpress.android.support.feature.aibot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AIBotConversationsActivity : AppCompatActivity() {
    private val viewModel by viewModels<AIBotConversationsViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    NavigableContent()
                }
            }
        )
    }

    private enum class ConversationScreen {
        List,
        Detail
    }

    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()
        var currentTitle by remember { mutableStateOf("Conversations") }

        MaterialTheme {
            NavHost(
                navController = navController,
                startDestination = ConversationScreen.List.name
            ) {
                composable(route = ConversationScreen.List.name) {
                    currentTitle = "Conversations"
                    ConversationsListScreen(
                        onConversationClick = { conversation ->
                            viewModel.selectConversation(conversation)
                            navController.navigate(ConversationScreen.Detail.name)
                        },
                        onBackClick = { finish() }
                    )
                }

                composable(route = ConversationScreen.Detail.name) {
                    viewModel.selectedConversation.value?.let { conversation ->
                        currentTitle = conversation.title
                        ConversationDetailScreen(
                            conversation = conversation,
                            onBackClick = { navController.navigateUp() },
                            onSendMessage = { text ->
                                viewModel.sendMessage(text)
                            }
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConversationsListScreen(
        onConversationClick: (BotConversation) -> Unit,
        onBackClick: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Conversations") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.createNewConversation()
                            viewModel.selectedConversation.value?.let { newConversation ->
                                navController.navigate(ConversationScreen.Detail.name)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New conversation"
                            )
                        }
                    }
                )
            },
        ) { contentPadding ->
            ShowConversationsList(
                modifier = Modifier.padding(contentPadding),
                onConversationClick = onConversationClick
            )
        }
    }

    @Composable
    private fun ShowConversationsList(
        modifier: Modifier,
        onConversationClick: (BotConversation) -> Unit
    ) {
        val conversations by viewModel.conversations.collectAsState()

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Add top spacing
                Spacer(modifier = Modifier.padding(top = 4.dp))
            }

            items(conversations) { conversation ->
                ConversationCard(
                    conversation = conversation,
                    onClick = { onConversationClick(conversation) }
                )
            }

            item {
                // Add bottom spacing
                Spacer(modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConversationDetailScreen(
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

    @Composable
    private fun ConversationCard(
        conversation: BotConversation,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatRelativeTime(conversation.mostRecentMessageDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                conversation.messages.lastOrNull()?.let { lastMessage ->
                    Text(
                        text = lastMessage.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    private fun formatRelativeTime(date: Date): String {
        val now = Date()
        val diffMillis = now.time - date.time
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
            diffHours < 24 -> "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
            diffDays < 7 -> "$diffDays day${if (diffDays == 1L) "" else "s"} ago"
            diffDays < 30 -> "${diffDays / 7} week${if (diffDays / 7 == 1L) "" else "s"} ago"
            else -> {
                val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                formatter.format(date)
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(
            context: Context,
        ): Intent = Intent(context, AIBotConversationsActivity::class.java)
    }
}

// Preview functions
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true, name = "Conversations List")
@Composable
private fun ConversationsScreenPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Conversations") },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            },
        ) { contentPadding ->
            ConversationsListPreview(modifier = Modifier.padding(contentPadding))
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

@Composable
private fun ConversationsListPreview(modifier: Modifier) {
    val now = Date()
    val sampleConversations = listOf(
        BotConversation(
            id = 1234,
            title = "App Crashing on Launch",
            mostRecentMessageDate = Date(now.time - 120_000), // 2 minutes ago
            messages = listOf(
                BotMessage(
                    id = 1001,
                    text = "Hi, I'm having trouble with the app.",
                    date = Date(now.time - 3_600_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 1002,
                    text = "Wonderful! I'm so glad that resolved the issue for you.",
                    date = Date(now.time - 120_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1235,
            title = "Site Setup Assistance",
            mostRecentMessageDate = Date(now.time - 7_200_000), // 2 hours ago
            messages = listOf(
                BotMessage(
                    id = 2001,
                    text = "I just created my WordPress site and need help getting started.",
                    date = Date(now.time - 7_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 2002,
                    text = "Congratulations on your new site! I'd be happy to help you get started.",
                    date = Date(now.time - 7_200_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1236,
            title = "Theme Customization",
            mostRecentMessageDate = Date(now.time - 86_400_000), // 1 day ago
            messages = listOf(
                BotMessage(
                    id = 3001,
                    text = "How can I change the colors on my site?",
                    date = Date(now.time - 87_000_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 3002,
                    text = "You can change the colors by going to Appearance → Customize → Colors.",
                    date = Date(now.time - 86_400_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1237,
            title = "SEO Optimization Tips",
            mostRecentMessageDate = Date(now.time - 259_200_000), // 3 days ago
            messages = listOf(
                BotMessage(
                    id = 4001,
                    text = "My site isn't showing up in Google search results.",
                    date = Date(now.time - 259_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 4002,
                    text = "To improve your SEO, consider installing an SEO plugin like Yoast.",
                    date = Date(now.time - 259_200_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        ),
        BotConversation(
            id = 1238,
            title = "Site Performance Questions",
            mostRecentMessageDate = Date(now.time - 604_800_000), // 1 week ago
            messages = listOf(
                BotMessage(
                    id = 5001,
                    text = "My website seems to be loading slowly.",
                    date = Date(now.time - 605_400_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = true
                ),
                BotMessage(
                    id = 5002,
                    text = "Your site is loading well, but here are some tips to optimize further.",
                    date = Date(now.time - 604_800_000),
                    userWantsToTalkToHuman = false,
                    isWrittenByUser = false
                )
            )
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
        }

        items(sampleConversations) { conversation ->
            ConversationCardPreview(conversation = conversation)
        }

        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun ConversationCardPreview(conversation: BotConversation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { }),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatRelativeTimePreview(conversation.mostRecentMessageDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            conversation.messages.lastOrNull()?.let { lastMessage ->
                Text(
                    text = lastMessage.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun formatRelativeTimePreview(date: Date): String {
    val now = Date()
    val diffMillis = now.time - date.time
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes == 1L) "" else "s"} ago"
        diffHours < 24 -> "$diffHours hour${if (diffHours == 1L) "" else "s"} ago"
        diffDays < 7 -> "$diffDays day${if (diffDays == 1L) "" else "s"} ago"
        diffDays < 30 -> "${diffDays / 7} week${if (diffDays / 7 == 1L) "" else "s"} ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}
