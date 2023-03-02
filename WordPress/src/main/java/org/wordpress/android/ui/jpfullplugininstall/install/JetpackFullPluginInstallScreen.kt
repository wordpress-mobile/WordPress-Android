package org.wordpress.android.ui.jpfullplugininstall.install

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.DoneState
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.ErrorState
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.InitialState
import org.wordpress.android.ui.jpfullplugininstall.install.compose.state.InstallingState

@Composable
fun JetpackFullPluginInstallScreen(
    uiState: UiState,
    onDismissScreenClick: () -> Unit,
    onContinueClick: () -> Unit,
    onDoneClick: () -> Unit,
    onRetryClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onInitialShown: () -> Unit = {},
    onInstallingShown: () -> Unit = {},
    onErrorShown: () -> Unit = {},
) {
    uiState.apply {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(toolbarTitle),
                    navigationIcon = NavigationIcons.CloseIcon.takeIf { uiState.showCloseButton },
                    onNavigationIconClick = onDismissScreenClick
                )
            },
        ) {
            when (this) {
                is UiState.Initial -> {
                    InitialState(
                        uiState = this,
                        onContinueClick = onContinueClick,
                    )
                    onInitialShown()
                }

                is UiState.Installing -> {
                    InstallingState(
                        uiState = this,
                    )
                    onInstallingShown()
                }

                is UiState.Done -> {
                    DoneState(
                        uiState = this,
                        onDoneClick = onDoneClick,
                    )
                }

                is UiState.Error -> {
                    ErrorState(
                        uiState = this,
                        onRetryClick = onRetryClick,
                        onContactSupportClick = onContactSupportClick,
                    )
                    onErrorShown()
                }
            }
        }
    }
}
