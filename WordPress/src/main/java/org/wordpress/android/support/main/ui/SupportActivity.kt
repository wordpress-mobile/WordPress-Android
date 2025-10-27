package org.wordpress.android.support.main.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.WordPress
import org.wordpress.android.support.aibot.ui.AIBotSupportActivity
import org.wordpress.android.support.logs.ui.LogsActivity
import org.wordpress.android.support.he.ui.HESupportActivity
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3

@AndroidEntryPoint
class SupportActivity : AppCompatActivity() {
    private val viewModel by viewModels<SupportViewModel>()

    private lateinit var composeView: ComposeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
        observeNavigationEvents()
        composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val userInfo by viewModel.userInfo.collectAsState()
                    val optionsVisibility by viewModel.optionsVisibility.collectAsState()
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                    AppThemeM3 {
                        SupportScreen(
                            userName = userInfo.userName,
                            userEmail = userInfo.userEmail,
                            userAvatarUrl = userInfo.avatarUrl,
                            isLoggedIn = isLoggedIn,
                            showAskTheBots = optionsVisibility.showAskTheBots,
                            showAskHappinessEngineers = optionsVisibility.showAskHappinessEngineers,
                            versionName = WordPress.versionName,
                            onBackClick = { finish() },
                            onLoginClick = { viewModel.onLoginClick() },
                            onHelpCenterClick = { viewModel.onHelpCenterClick() },
                            onAskTheBotsClick = { viewModel.onAskTheBotsClick() },
                            onAskHappinessEngineersClick = { viewModel.onAskHappinessEngineersClick() },
                            onApplicationLogsClick = { viewModel.onApplicationLogsClick() }
                        )
                    }
                }
            }
        )
    }

    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is SupportViewModel.NavigationEvent.NavigateToAskTheBots -> navigateToAskTheBots()
                        is SupportViewModel.NavigationEvent.NavigateToLogin -> navigateToLogin()
                        is SupportViewModel.NavigationEvent.NavigateToHelpCenter -> navigateToHelpCenter()
                        is SupportViewModel.NavigationEvent.NavigateToApplicationLogs -> navigateToApplicationLogs()
                        is SupportViewModel.NavigationEvent.NavigateToAskHappinessEngineers -> {
                            navigateToAskTheHappinessEngineers()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToAskTheBots() {
        startActivity(
            AIBotSupportActivity.Companion.createIntent(this)
        )
    }

    private fun navigateToAskTheHappinessEngineers() {
        startActivity(
            HESupportActivity.Companion.createIntent(this)
        )
    }

    private fun navigateToLogin() {
        if (BuildConfig.IS_JETPACK_APP) {
            ActivityLauncher.showSignInForResultJetpackOnly(this)
        } else {
            ActivityLauncher.showSignInForResultWpComOnly(this)
        }
    }

    private fun navigateToHelpCenter() {
        val intent = Intent(Intent.ACTION_VIEW, "https://wordpress.com/support/".toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(null) // Ensure it doesn't match internal activities
        }
        startActivity(intent)
        AnalyticsTracker.track(Stat.SUPPORT_HELP_CENTER_VIEWED)
    }

    private fun navigateToApplicationLogs() {
        startActivity(LogsActivity.createIntent(this))
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, SupportActivity::class.java)
    }
}
