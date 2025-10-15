package org.wordpress.android.support.aibot.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class AIBotSupportActivity : AppCompatActivity() {
    private val viewModel by viewModels<AIBotSupportViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userName = intent.getStringExtra(USERNAME).orEmpty()
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
        viewModel.init(
            accessToken = intent.getStringExtra(ACCESS_TOKEN_ID)!!,
            userId = intent.getLongExtra(USER_ID, 0)
        )

        // Observe error messages and show them as Toast
        lifecycleScope.launch {
            viewModel.errorMessage.collect { errorType ->
                val errorMessage = when (errorType) {
                    AIBotSupportViewModel.ErrorType.GENERAL -> getString(R.string.ai_bot_generic_error)
                    null -> null
                }
                errorMessage?.let {
                    ToastUtils.showToast(this@AIBotSupportActivity, it, ToastUtils.Duration.LONG, Gravity.CENTER)
                    viewModel.clearError()
                }
            }
        }
    }

    private enum class ConversationScreen {
        List,
        Detail
    }

    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()

        AppThemeM3 {
            NavHost(
                navController = navController,
                startDestination = ConversationScreen.List.name
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
                    selectedConversation?.let { conversation ->
                        ConversationDetailScreen(
                            userName = userName,
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

    companion object {
        private const val ACCESS_TOKEN_ID = "arg_access_token_id"
        private const val USER_ID = "arg_user_id"
        private const val USERNAME = "arg_username"
        @JvmStatic
        fun createIntent(
            context: Context,
            accessToken: String,
            userId: Long,
            userName: String,
        ): Intent = Intent(context, AIBotSupportActivity::class.java).apply {
            putExtra(ACCESS_TOKEN_ID, accessToken)
            putExtra(USER_ID, userId)
            putExtra(USERNAME, userName)
        }
    }
}
