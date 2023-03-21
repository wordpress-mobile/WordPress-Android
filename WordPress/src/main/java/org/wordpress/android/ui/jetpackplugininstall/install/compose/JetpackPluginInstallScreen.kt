package org.wordpress.android.ui.jetpackplugininstall.install.compose

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.jetpackplugininstall.install.UiState
import org.wordpress.android.ui.jetpackplugininstall.install.compose.state.DoneState
import org.wordpress.android.ui.jetpackplugininstall.install.compose.state.ErrorState
import org.wordpress.android.ui.jetpackplugininstall.install.compose.state.InitialState
import org.wordpress.android.ui.jetpackplugininstall.install.compose.state.InstallingState
@Composable
fun JetpackPluginInstallScreen(
    uiState: UiState,
    onDismissScreenClick: () -> Unit,
    onInitialButtonClick: () -> Unit,
    onDoneButtonClick: () -> Unit,
    onRetryButtonClick: () -> Unit,
    onContactSupportButtonClick: () -> Unit,
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
                        onContinueClick = onInitialButtonClick,
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
                        onDoneClick = onDoneButtonClick,
                    )
                }

                is UiState.Error -> {
                    ErrorState(
                        uiState = this,
                        onRetryClick = onRetryButtonClick,
                        onContactSupportClick = onContactSupportButtonClick,
                    )
                    onErrorShown()
                }
            }
        }
    }
}
