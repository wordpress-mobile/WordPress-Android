package org.wordpress.android.support.main.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chuckerteam.chucker.api.Chucker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.NetworkRequestsRetentionPeriod
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
        observeDialogEvents()
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
                            onBackClick = { finish() },
                            onLoginClick = { viewModel.onLoginClick() },
                            onHelpCenterClick = { viewModel.onHelpCenterClick() },
                            onAskTheBotsClick = { viewModel.onAskTheBotsClick() },
                            onAskHappinessEngineersClick = { viewModel.onAskHappinessEngineersClick() },
                            onApplicationLogsClick = { viewModel.onApplicationLogsClick() },
                            onNetworkTrackingToggle = { viewModel.onNetworkTrackingToggle(it) },
                            onViewNetworkRequestsClick = { viewModel.onViewNetworkRequestsClick() },
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
        return when (period) {
            NetworkRequestsRetentionPeriod.ONE_HOUR ->
                getString(R.string.network_requests_retention_one_hour)
            NetworkRequestsRetentionPeriod.ONE_DAY ->
                getString(R.string.network_requests_retention_one_day)
            NetworkRequestsRetentionPeriod.ONE_WEEK ->
                getString(R.string.network_requests_retention_one_week)
            NetworkRequestsRetentionPeriod.FOREVER ->
                getString(R.string.network_requests_retention_until_cleared)
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

    private fun observeDialogEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dialogEvents.collect { event ->
                    when (event) {
                        is SupportViewModel.DialogEvent.ShowEnableTrackingDialog -> {
                            showEnableTrackingDialog(event.currentPeriod)
                        }
                        is SupportViewModel.DialogEvent.ShowDisableTrackingDialog -> {
                            showDisableTrackingDialog()
                        }
                    }
                }
            }
        }
    }

    private fun showEnableTrackingDialog(currentPeriod: NetworkRequestsRetentionPeriod) {
        val periods = NetworkRequestsRetentionPeriod.entries.toTypedArray()
        val displayNames = periods.map { getRetentionPeriodDisplayString(it) }.toTypedArray()
        var selectedIndex = periods.indexOf(currentPeriod)

        @SuppressLint("InflateParams") // Parent is null because AlertDialog attaches it internally
        val titleView = layoutInflater.inflate(R.layout.dialog_title_with_message, null).apply {
            findViewById<TextView>(R.id.dialog_title).setText(R.string.track_network_requests)
            findViewById<TextView>(R.id.dialog_message)
                .setText(R.string.network_requests_enable_dialog_description)
        }

        AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setSingleChoiceItems(displayNames, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(R.string.network_requests_enable) { _, _ ->
                val selectedPeriod = periods[selectedIndex]
                viewModel.onEnableTrackingConfirmed(selectedPeriod)
            }
            // No action needed on cancel - UI state is driven by ViewModel
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDisableTrackingDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.network_requests_disable_tracking_title)
            .setMessage(R.string.network_requests_disable_tracking_description)
            .setPositiveButton(R.string.network_requests_disable) { _, _ ->
                viewModel.onDisableTrackingConfirmed()
            }
            // No action needed on cancel - UI state is driven by ViewModel
            .setNegativeButton(R.string.cancel, null)
            .show()
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

    private fun navigateToNetworkRequests() {
        startActivity(Chucker.getLaunchIntent(this))
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, SupportActivity::class.java)
    }
}
