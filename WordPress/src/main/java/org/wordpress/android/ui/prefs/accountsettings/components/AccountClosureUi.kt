package org.wordpress.android.ui.prefs.accountsettings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.AccountClosureUiState.Opened
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.Companion.AccountClosureAction.ACCOUNT_CLOSED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.Companion.AccountClosureAction.SUPPORT_CONTACTED

@Composable
fun AccountClosureUi(viewModel: AccountSettingsViewModel) {
    val uiState = viewModel.accountClosureUiState.collectAsState()

    CloseAccountButton(onClick = { viewModel.openAccountClosureDialog() })

    (uiState.value as? Opened)?.let {
        AccountClosureDialog(
            onDismissRequest = { viewModel.dismissAccountClosureDialog() },
        ) {
            when(it) {
                is Opened.Default -> it.username?.let { currentUsername ->
                    DialogUi(
                        currentUsername = currentUsername,
                        isPending = it.isPending,
                        onCancel = { viewModel.dismissAccountClosureDialog() },
                        onConfirm = { viewModel.closeAccount() },
                    )
                }

                is Opened.Error -> DialogErrorUi(
                    onDismissRequest = { viewModel.dismissAccountClosureDialog() },
                    onHelpRequested = { viewModel.userAction(SUPPORT_CONTACTED) },
                    it.errorType,
                )
                is Opened.Success -> DialogSuccessUi(
                    onDismissRequest = { viewModel.userAction(ACCOUNT_CLOSED) }
                )
            }
        }
    }
}
