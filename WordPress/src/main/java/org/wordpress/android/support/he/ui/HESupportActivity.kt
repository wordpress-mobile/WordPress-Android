package org.wordpress.android.support.he.ui

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
        viewModel.init()
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
                    HEConversationsListScreen(
                        conversations = viewModel.conversations,
                        onConversationClick = { conversation ->
                            viewModel.selectConversation(conversation)
                            navController.navigate(ConversationScreen.Detail.name)
                        },
                        onBackClick = { finish() },
                        onCreateNewConversationClick = {
                            viewModel.createNewConversation()
                        }
                    )
                }

                composable(route = ConversationScreen.Detail.name) {
                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    selectedConversation?.let { conversation ->
                        HEConversationDetailScreen(
                            conversation = conversation,
                            onBackClick = { navController.navigateUp() }
                        )
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, HESupportActivity::class.java)
    }
}
