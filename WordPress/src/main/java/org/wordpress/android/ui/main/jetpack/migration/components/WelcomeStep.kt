package org.wordpress.android.ui.main.jetpack.migration.components

import androidx.compose.runtime.Composable
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.StepUiState

@Composable
fun WelcomeStep(uiState: StepUiState.Welcome) = with(uiState) {
    ScreenIcon(iconRes = screenIconRes)
}
