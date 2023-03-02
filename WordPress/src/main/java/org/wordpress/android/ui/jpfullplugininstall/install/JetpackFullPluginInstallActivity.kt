package org.wordpress.android.ui.jpfullplugininstall.install

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class JetpackFullPluginInstallActivity : AppCompatActivity() {
    private val viewModel: JetpackFullPluginInstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val uiState by viewModel.uiState.collectAsState()
                JetpackFullPluginInstallScreen(
                    uiState = uiState,
                    onDismissScreenClick = viewModel::onDismissScreenClick,
                    onContinueClick = viewModel::onContinueClick,
                    onDoneClick = viewModel::onDoneClick,
                    onRetryClick = viewModel::onRetryClick,
                    onContactSupportClick = viewModel::onContactSupportClick,
                    onInitialShown = viewModel::onInitialShown,
                    onInstallingShown = viewModel::onInstallingShown,
                    onErrorShown = viewModel::onErrorShown,

                    )
            }
        }
        observeActionEvents()
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: ActionEvent) {
        when (actionEvent) {
            is ActionEvent.ContactSupport -> {
                ActivityLauncher.viewHelp(
                    this,
                    actionEvent.origin,
                    actionEvent.selectedSite,
                    null
                )
            }

            is ActionEvent.Dismiss -> {
                ActivityLauncher.showMainActivity(this)
                finish()
            }
        }.exhaustive
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context) = Intent(context, JetpackFullPluginInstallActivity::class.java)
    }
}
