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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chuckerteam.chucker.api.Chucker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.NetworkRequestsRetentionPeriod
import org.wordpress.android.support.aibot.ui.AIBotSupportActivity
import org.wordpress.android.support.he.ui.HESupportActivity
import org.wordpress.android.support.logs.ui.LogsActivity
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.compose.theme.AppThemeM3
import javax.inject.Inject

@AndroidEntryPoint
class SupportActivity : AppCompatActivity() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

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
                    val networkTrackingState by viewModel.networkTrackingState.collectAsState()
                    val dialogState by viewModel.dialogState.collectAsState()
                    AppThemeM3 {
                        SupportScreen(
                            userName = userInfo.userName,
                            userEmail = userInfo.userEmail,
                            userAvatarUrl = userInfo.avatarUrl,
                            isLoggedIn = isLoggedIn,
                            showAskTheBots = optionsVisibility.showAskTheBots,
                            showAskHappinessEngineers = optionsVisibility.showAskHappinessEngineers,
                            showNetworkDebugging = networkTrackingState.showNetworkDebugging,
                            isNetworkTrackingEnabled = networkTrackingState.isTrackingEnabled,
                            networkTrackingRetentionInfo = getRetentionInfoText(
                                networkTrackingState.retentionPeriod
                            ),
                            versionName = WordPress.versionName,
                            dialogState = dialogState,
                            onBackClick = { finish() },
                            onLoginClick = { viewModel.onLoginClick() },
                            onHelpCenterClick = { viewModel.onHelpCenterClick() },
                            onAskTheBotsClick = { viewModel.onAskTheBotsClick() },
                            onAskHappinessEngineersClick = { viewModel.onAskHappinessEngineersClick() },
                            onApplicationLogsClick = { viewModel.onApplicationLogsClick() },
                            onNetworkTrackingToggle = { viewModel.onNetworkTrackingToggle(it) },
                            onViewNetworkRequestsClick = { viewModel.onViewNetworkRequestsClick() },
                            onRetentionPeriodSelected = { viewModel.onRetentionPeriodSelected(it) },
                            onEnableTrackingConfirmed = { viewModel.onEnableTrackingConfirmed(it) },
                            onDisableTrackingConfirmed = { viewModel.onDisableTrackingConfirmed() },
                            onDialogDismissed = { viewModel.onDialogDismissed() },
                        )
                    }
                }
            }
        )
    }

    private fun getRetentionInfoText(period: NetworkRequestsRetentionPeriod): String {
        val periodString = getRetentionPeriodDisplayString(period)
        return getString(R.string.network_requests_retention_info, periodString)
    }

private fun getRetentionPeriodDisplayString(period: NetworkRequestsRetentionPeriod): String {
    return getString(getRetentionPeriodStringRes(period))
}

private fun getRetentionPeriodStringRes(period: NetworkRequestsRetentionPeriod): Int {
    return when (period) {
        NetworkRequestsRetentionPeriod.ONE_HOUR -> R.string.network_requests_retention_one_hour
        NetworkRequestsRetentionPeriod.ONE_DAY -> R.string.network_requests_retention_one_day
        NetworkRequestsRetentionPeriod.ONE_WEEK -> R.string.network_requests_retention_one_week
        NetworkRequestsRetentionPeriod.FOREVER -> R.string.network_requests_retention_until_cleared
    }
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
                        is SupportViewModel.NavigationEvent.NavigateToNetworkRequests -> {
                            navigateToNetworkRequests()
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
        activityNavigator.openInCustomTab(this, HELP_CENTER_URL)
        AnalyticsTracker.track(Stat.SUPPORT_HELP_CENTER_VIEWED)
    }

    private fun navigateToApplicationLogs() {
        startActivity(LogsActivity.createIntent(this))
    }

    private fun navigateToNetworkRequests() {
        startActivity(Chucker.getLaunchIntent(this))
    }

    companion object {
        private const val HELP_CENTER_URL = "https://apps.wordpress.com/support/mobile"

        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, SupportActivity::class.java)
    }
}
