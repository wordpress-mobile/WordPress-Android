package org.wordpress.android.support.logs.ui

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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class LogsActivity : AppCompatActivity() {
    private val viewModel by viewModels<LogsViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    @Inject
    lateinit var appLogWrapper: AppLogWrapper

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
        // Observe error messages and show them as Toast
        lifecycleScope.launch {
            viewModel.errorMessage.collect { errorType ->
                val errorMessage = when (errorType) {
                    LogsViewModel.ErrorType.GENERAL -> getString(R.string.logs_screen_general_error)
                    null -> null
                }
                errorMessage?.let {
                    ToastUtils.showToast(this@LogsActivity, it, ToastUtils.Duration.LONG, Gravity.CENTER)
                    viewModel.clearError()
                }
            }
        }
        viewModel.init(this)
    }

    private enum class LogsScreen {
        List,
        Detail
    }

    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()

        AppThemeM3 {
            NavHost(
                navController = navController,
                startDestination = LogsScreen.List.name
            ) {
                composable(route = LogsScreen.List.name) {
                    val logDays by viewModel.logDays.collectAsState()
                    LogsListScreen(
                        logDays = logDays,
                        onLogDayClick = { logDay ->
                            viewModel.selectLogDay(logDay)
                            navController.navigate(LogsScreen.Detail.name)
                        },
                        onBackClick = { finish() },
                        onShareClick = { shareAppLog() }
                    )
                }

                composable(route = LogsScreen.Detail.name) {
                    val selectedLogDay by viewModel.selectedLogDay.collectAsState()
                    selectedLogDay?.let { logDay ->
                        LogDetailScreen(
                            logDay = logDay,
                            onBackClick = { navController.navigateUp() }
                        )
                    }
                }
            }
        }
    }

    private fun shareAppLog() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, AppLog.toPlainText(this@LogsActivity))
            putExtra(
                Intent.EXTRA_SUBJECT,
                "${getString(R.string.app_name)} ${getString(R.string.support_screen_application_logs_title)}"
            )
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.reader_btn_share)))
        } catch (ex: android.content.ActivityNotFoundException) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent)
            appLogWrapper.e(AppLog.T.SUPPORT, "Error sharing logs: ${ex.stackTraceToString()}")
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, LogsActivity::class.java)
    }
}
