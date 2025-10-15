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
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppThemeM3

@AndroidEntryPoint
class SupportActivity : AppCompatActivity() {
    private val viewModel by viewModels<SupportViewModel>()

    private lateinit var composeView: ComposeView

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
                    val userInfo by viewModel.userInfo.collectAsState()
                    AppThemeM3 {
                        SupportScreen(
                            userName = userInfo.userName,
                            userEmail = userInfo.userEmail,
                            userAvatarUrl = userInfo.avatarUrl,
                            onBackClick = { finish() },
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

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent = Intent(context, SupportActivity::class.java)
    }
}
