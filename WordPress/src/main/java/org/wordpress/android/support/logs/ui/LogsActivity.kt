package org.wordpress.android.support.logs.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtils
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class LogsActivity : AppCompatActivity() {
    private val viewModel by viewModels<LogsViewModel>()

    private lateinit var composeView: ComposeView
    private var navController: NavHostController? = null

    @Inject
    lateinit var appLogWrapper: AppLogWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()
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
        observeErrorMessages()
        observeNavigationEvents()
        observeActionEvents()
    }

    private fun observeErrorMessages() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }
    }

    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is LogsViewModel.NavigationEvent.NavigateToDetail -> {
                            navController?.navigate(LogsScreen.Detail.name)
                        }
                    }
                }
            }
        }
    }

    private fun observeActionEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionEvents.collect { event ->
                    when (event) {
                        is LogsViewModel.ActionEvent.ShareLogFile -> shareLogFile(event.file)
                    }
                }
            }
        }
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
                navController = navController!!,
                startDestination = LogsScreen.List.name
            ) {
                composable(route = LogsScreen.List.name) {
                    val logFiles by viewModel.logFiles.collectAsState()
                    LogsListScreen(
                        logFiles = logFiles,
                        onLogFileClick = { logFile -> viewModel.onLogFileClick(logFile) },
                        onBackClick = { finish() }
                    )
                }

                composable(route = LogsScreen.Detail.name) {
                    val selectedLogFile by viewModel.selectedLogFile.collectAsState()
                    selectedLogFile?.let { logFile ->
                        LogDetailScreen(
                            logFile = logFile,
                            onBackClick = { navController?.navigateUp() },
                            onShareClick = { viewModel.onShareClick(logFile) }
                        )
                    } ?: run {
                        LaunchedEffect(Unit) {
                            navController?.navigateUp()
                        }
                    }
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun shareLogFile(cachedFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                cachedFile
            )
            val subject = "${getString(R.string.app_name)} " +
                "${getString(R.string.support_screen_application_logs_title)} - ${cachedFile.name}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.reader_btn_share)))
        } catch (ex: Exception) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent)
            appLogWrapper.e(AppLog.T.SUPPORT, "Error sharing log file: ${ex.stackTraceToString()}")
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, LogsActivity::class.java)
    }
}
