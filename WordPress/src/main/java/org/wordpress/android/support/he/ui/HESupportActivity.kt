package org.wordpress.android.support.he.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.R
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel

@AndroidEntryPoint
class HESupportActivity : AppCompatActivity() {
    private val viewModel by viewModels<HESupportViewModel>()

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
        observeNavigationEvents()
        viewModel.init()
    }


    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is ConversationsSupportViewModel.NavigationEvent.NavigateToConversationDetail -> {
                            navController.navigate(ConversationScreen.Detail.name)
                        }
                        ConversationsSupportViewModel.NavigationEvent.NavigateToNewConversation -> {
                            navController.navigate(ConversationScreen.NewTicket.name)
                        }
                        ConversationsSupportViewModel.NavigationEvent.NavigateBack -> {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }

    private enum class ConversationScreen {
        List,
        Detail,
        NewTicket
    }

    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val errorMessage by viewModel.errorMessage.collectAsState()

        // Show snackbar when error occurs
        errorMessage?.let { errorType ->
            val message = when (errorType) {
                ConversationsSupportViewModel.ErrorType.GENERAL -> getString(R.string.he_support_generic_error)
                ConversationsSupportViewModel.ErrorType.FORBIDDEN -> getString(R.string.he_support_forbidden_error)
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearError()
            }
        }

        AppThemeM3 {
            NavHost(
                navController = navController,
                startDestination = ConversationScreen.List.name,
            ) {
                composable(route = ConversationScreen.List.name) {
                    HEConversationsListScreen(
                        snackbarHostState = snackbarHostState,
                        conversations = viewModel.conversations,
                        isLoadingConversations = viewModel.isLoadingConversations,
                        onConversationClick = { conversation ->
                            viewModel.onConversationClick(conversation)
                        },
                        onBackClick = { finish() },
                        onCreateNewConversationClick = {
                            viewModel.onCreateNewConversationClick()
                        },
                        onRefresh = {
                            viewModel.refreshConversations()
                        }
                    )
                }

                composable(route = ConversationScreen.Detail.name) {
                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    val isLoadingConversation by viewModel.isLoadingConversation.collectAsState()
                    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
                    val messageSendResult by viewModel.messageSendResult.collectAsState()
                    selectedConversation?.let { conversation ->
                        HEConversationDetailScreen(
                            snackbarHostState = snackbarHostState,
                            conversation = conversation,
                            isLoading = isLoadingConversation,
                            isSendingMessage = isSendingMessage,
                            messageSendResult = messageSendResult,
                            onBackClick = { viewModel.onBackClick() },
                            onSendMessage = { message, includeAppLogs ->
                                viewModel.onAddMessageToConversation(
                                    message = message,
                                    attachments = emptyList()
                                )
                            },
                            onClearMessageSendResult = { viewModel.clearMessageSendResult() }
                        )
                    }
                }

                composable(route = ConversationScreen.NewTicket.name) {
                    val userInfo by viewModel.userInfo.collectAsState()
                    val isSendingNewConversation by viewModel.isSendingMessage.collectAsState()
                    HENewTicketScreen(
                        snackbarHostState = snackbarHostState,
                        onBackClick = { viewModel.onBackClick() },
                        onSubmit = { category, subject, messageText, siteAddress ->
                            viewModel.onSendNewConversation(
                                subject = subject,
                                message = messageText,
                                tags = listOf(category.name),
                                attachments = listOf()
                            )
                        },
                        userName = userInfo.userName,
                        userEmail = userInfo.userEmail,
                        userAvatarUrl = userInfo.avatarUrl,
                        isSendingNewConversation = isSendingNewConversation
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, HESupportActivity::class.java)
    }
}
