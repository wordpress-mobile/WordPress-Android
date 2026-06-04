package org.wordpress.android.support.unified.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.wordpress.android.R
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.ui.compose.theme.AppThemeM3

@AndroidEntryPoint
class UnifiedSupportActivity : AppCompatActivity() {
    private val viewModel by viewModels<UnifiedSupportViewModel>()

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
                            navController.navigate(UnifiedScreen.Detail.name)
                        }
                        ConversationsSupportViewModel.NavigationEvent.NavigateBack -> {
                            navController.navigateUp()
                        }
                        ConversationsSupportViewModel.NavigationEvent.NavigateToNewConversation -> {
                            // Not supported in unified flow.
                        }
                    }
                }
            }
        }
    }

    private enum class UnifiedScreen {
        List,
        Detail
    }

    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by viewModel.errorMessage.collectAsState()

        val errorType = errorMessage
        if (errorType != null) {
            val message = when (errorType) {
                ConversationsSupportViewModel.ErrorType.GENERAL ->
                    getString(R.string.unified_support_generic_error)
                ConversationsSupportViewModel.ErrorType.FORBIDDEN ->
                    getString(R.string.he_support_forbidden_error)
                ConversationsSupportViewModel.ErrorType.OFFLINE ->
                    getString(R.string.no_network_title)
            }
            LaunchedEffect(errorType) {
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
                startDestination = UnifiedScreen.List.name,
            ) {
                composable(route = UnifiedScreen.List.name) {
                    val conversationsState by viewModel.conversationsState.collectAsState()
                    val conversations by viewModel.conversations.collectAsState()
                    UnifiedConversationsListScreen(
                        snackbarHostState = snackbarHostState,
                        conversations = conversations,
                        conversationsState = conversationsState,
                        onConversationClick = { conversation ->
                            viewModel.onConversationClick(conversation)
                        },
                        onBackClick = { finish() },
                        onCreateNewConversationClick = { viewModel.onCreateNewBotConversationClick() },
                        onRefresh = { viewModel.refreshConversations() },
                    )
                }

                composable(route = UnifiedScreen.Detail.name) {
                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    val isLoadingConversation by viewModel.isLoadingConversation.collectAsState()
                    val isSendingReply by viewModel.isSendingReply.collectAsState()
                    selectedConversation?.let { conversation ->
                        UnifiedConversationDetailScreen(
                            snackbarHostState = snackbarHostState,
                            conversation = conversation,
                            isLoading = isLoadingConversation,
                            isSendingReply = isSendingReply,
                            onBackClick = { viewModel.onBackClick() },
                            onSendReply = { text -> viewModel.sendReply(text) },
                        )
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, UnifiedSupportActivity::class.java)
    }
}
