package org.wordpress.android.ui.prefs.accountsettings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.AccountClosureUiState.Opened

@Composable
fun AccountClosureUi(viewModel: AccountSettingsViewModel, onHelpRequested: () -> Unit) {
    val uiState = viewModel.accountClosureUiState.collectAsState()

    CloseAccountButton(onClick = { viewModel.openAccountClosureDialog() })

    (uiState.value as? Opened)?.let {
        when(it) {
           is Opened.Default -> it.username?.let { currentUsername ->
               AccountClosureDialog(
                   onDismissRequest = { viewModel.dismissAccountClosureDialog() },
                   currentUsername,
                   onConfirm = { viewModel.closeAccount() },
                   isPending = it.isPending,
               )
           }

            Opened.Atomic -> IneligibleClosureDialog(
                onDismissRequest = { viewModel.dismissAccountClosureDialog() },
                onHelpRequested = onHelpRequested,
            )
        }
    }
}
