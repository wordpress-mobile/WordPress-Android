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
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.R

@AndroidEntryPoint
class LogsActivity : AppCompatActivity() {
    private val viewModel by viewModels<LogsViewModel>()

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
                        onBackClick = { finish() }
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

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, LogsActivity::class.java)
    }
}
