package org.wordpress.android.ui.jpfullplugininstall.install

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jpfullplugininstall.install.JetpackFullPluginInstallViewModel.ActionEvent
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.DoneState
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.ErrorState
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.InitialState
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.InstallingState
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class JetpackFullPluginInstallActivity : AppCompatActivity() {
    private val viewModel: JetpackFullPluginInstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                JetpackFullPluginInstallScreen()
            }
        }
        observeActionEvents()
    }

    @Composable
    private fun JetpackFullPluginInstallScreen() {
        val uiState by viewModel.uiState.collectAsState()
        uiState.apply {
            when (this) {
                is UiState.Initial -> InitialState(
                    uiState = this,
                    onContinueClick = viewModel::onContinueClick,
                    onDismissScreenClick = viewModel::onDismissScreenClick,
                )
                is UiState.Installing -> InstallingState(
                    uiState = this,
                    onDismissScreenClick = viewModel::onDismissScreenClick,
                )
                is UiState.Done -> DoneState(
                    uiState = this,
                    onDoneClick = viewModel::onDoneClick,
                    onDismissScreenClick = viewModel::onDismissScreenClick,
                )
                is UiState.Error -> ErrorState(
                    uiState = this,
                    onRetryClick = viewModel::onRetryClick,
                    onContactSupportClick = viewModel::onContactSupportClick,
                    onDismissScreenClick = viewModel::onDismissScreenClick,
                )
            }
        }
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: ActionEvent) {
        when (actionEvent) {
            is ActionEvent.ContactSupport -> {
                ActivityLauncher.viewHelpAndSupport(
                    this,
                    actionEvent.origin,
                    actionEvent.selectedSite,
                    null
                )
            }
            is ActionEvent.Dismiss -> {
                finish()
            }
        }.exhaustive
    }
}
