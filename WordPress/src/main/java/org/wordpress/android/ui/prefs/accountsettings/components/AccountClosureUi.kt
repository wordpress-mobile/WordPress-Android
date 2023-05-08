package org.wordpress.android.ui.prefs.accountsettings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel

@Composable
fun AccountClosureUi(viewModel: AccountSettingsViewModel) {
    val uiState = viewModel.accountClosureUiState.collectAsState()

    CloseAccountButton(onClick = { viewModel.openAccountClosureDialog() })
    (uiState.value as? AccountSettingsViewModel.AccountClosureUiState.Opened)?.username?.let { currentUsername ->
        AccountClosureDialog(
            onDismissRequest = { viewModel.dismissAccountClosureDialog() },
            currentUsername,
        )
    }
}
