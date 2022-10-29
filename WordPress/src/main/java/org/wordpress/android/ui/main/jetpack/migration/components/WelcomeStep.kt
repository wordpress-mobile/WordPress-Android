package org.wordpress.android.ui.main.jetpack.migration.components

import androidx.compose.runtime.Composable
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.StepUiState

@Composable
fun WelcomeStep(uiState: StepUiState.Welcome) = with(uiState) {
    ScreenIcon(iconRes = screenIconRes)
    Title(text = uiStringText(title))
    Subtitle(text = uiStringText(subtitle))
    Message(text = uiStringText(message))
}
