package org.wordpress.android.support.aibot.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppThemeM3

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
        viewModel.init(intent.getStringExtra(ACCESS_TOKEN_ID)!!)
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
                    ConversationsListScreen(
                        conversations = viewModel.conversations,
                        onConversationClick = { conversation ->
                            viewModel.selectConversation(conversation)
                            navController.navigate(ConversationScreen.Detail.name)
                        },
                        onBackClick = { finish() },
                        onCreateNewConversationClick = {
                            viewModel.createNewConversation()
                            viewModel.selectedConversation.value?.let { newConversation ->
                                navController.navigate(ConversationScreen.Detail.name)
                            }
                        }
                    )
                }

                composable(route = ConversationScreen.Detail.name) {
                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    selectedConversation?.let { conversation ->
                        ConversationDetailScreen(
                            userName = userName,
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

    companion object {
        private const val ACCESS_TOKEN_ID = "arg_access_token_id"
        private const val USERNAME = "arg_username"
        @JvmStatic
        fun createIntent(
            context: Context,
            accessToken: String,
            userName: String,
        ): Intent = Intent(context, AIBotSupportActivity::class.java).apply {
            putExtra(ACCESS_TOKEN_ID, accessToken)
            putExtra(USERNAME, userName)
        }
    }
}
