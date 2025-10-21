package org.wordpress.android.support.he.ui

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
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.R

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
        observeErrorEvents()
        viewModel.init()
    }

    private fun observeErrorEvents() {
        // Observe error messages and show them as Toast
        lifecycleScope.launch {
            viewModel.errorMessage.collect { errorType ->
                val errorMessage = when (errorType) {
                    HESupportViewModel.ErrorType.GENERAL -> getString(R.string.he_support_generic_error)
                    null -> null
                }
                errorMessage?.let {
                    ToastUtils.showToast(this@HESupportActivity, it, ToastUtils.Duration.LONG, Gravity.CENTER)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is HESupportViewModel.NavigationEvent.NavigateToConversationDetail -> {
                            navController.navigate(ConversationScreen.Detail.name)
                        }
                        HESupportViewModel.NavigationEvent.NavigateToNewTicket -> {
                            navController.navigate(ConversationScreen.NewTicket.name)
                        }
                        HESupportViewModel.NavigationEvent.NavigateBack -> {
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

        AppThemeM3 {
            NavHost(
                navController = navController,
                startDestination = ConversationScreen.List.name
            ) {
                composable(route = ConversationScreen.List.name) {
                    HEConversationsListScreen(
                        conversations = viewModel.conversations,
                        isLoadingConversations = viewModel.isLoadingConversations,
                        onConversationClick = { conversation ->
                            viewModel.onConversationClick(conversation)
                        },
                        onBackClick = { finish() },
                        onCreateNewConversationClick = {
                            viewModel.onCreateNewConversation()
                        }
                    )
                }

                composable(route = ConversationScreen.Detail.name) {
                    val selectedConversation by viewModel.selectedConversation.collectAsState()
                    selectedConversation?.let { conversation ->
                        HEConversationDetailScreen(
                            conversation = conversation,
                            onBackClick = { viewModel.onBackFromDetailClick() }
                        )
                    }
                }

                composable(route = ConversationScreen.NewTicket.name) {
                    val userInfo by viewModel.userInfo.collectAsState()
                    HENewTicketScreen(
                        onBackClick = { viewModel.onBackFromDetailClick() },
                        onSubmit = { category, subject, siteAddress ->
                            viewModel.onSendNewConversation()
                        },
                        userName = userInfo.userName,
                        userEmail = userInfo.userEmail,
                        userAvatarUrl = userInfo.avatarUrl
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
