package org.wordpress.android.support.aibot.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.support.common.ui.SupportViewModel
import org.wordpress.android.ui.compose.theme.AppThemeM3

@AndroidEntryPoint
class AIBotSupportActivity : AppCompatActivity() {
    private val viewModel by viewModels<AIBotSupportViewModel>()

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
        viewModel.init()
    }

    private enum class ConversationScreen {
        List,
        Detail
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
                SupportViewModel.ErrorType.GENERAL -> getString(R.string.ai_bot_generic_error)
                SupportViewModel.ErrorType.FORBIDDEN -> getString(R.string.he_support_forbidden_error)
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
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = ConversationScreen.List.name,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(route = ConversationScreen.List.name) {
                        val isLoadingConversations by viewModel.isLoadingConversations.collectAsState()
                        ConversationsListScreen(
                            conversations = viewModel.conversations,
                            isLoading = isLoadingConversations,
                            onConversationClick = { conversation ->
                                viewModel.onConversationSelected(conversation)
                                navController.navigate(ConversationScreen.Detail.name)
                            },
                            onBackClick = { finish() },
                            onCreateNewConversationClick = {
                                viewModel.onNewConversationClicked()
                                viewModel.selectedConversation.value?.let { newConversation ->
                                    navController.navigate(ConversationScreen.Detail.name)
                                }
                            },
                            onRefresh = {
                                viewModel.refreshConversations()
                            }
                        )
                    }

                    composable(route = ConversationScreen.Detail.name) {
                        val selectedConversation by viewModel.selectedConversation.collectAsState()
                        val isLoadingConversation by viewModel.isLoadingConversation.collectAsState()
                        val isBotTyping by viewModel.isBotTyping.collectAsState()
                        val canSendMessage by viewModel.canSendMessage.collectAsState()
                        val userInfo by viewModel.userInfo.collectAsState()
                        selectedConversation?.let { conversation ->
                            ConversationDetailScreen(
                                userName = userInfo.userName,
                                conversation = conversation,
                                isLoading = isLoadingConversation,
                                isBotTyping = isBotTyping,
                                canSendMessage = canSendMessage,
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
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, AIBotSupportActivity::class.java)
    }
}
