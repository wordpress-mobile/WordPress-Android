package org.wordpress.android.ui.main.jetpack.migration.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.ButtonsColumn
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.components.buttons.SecondaryButton
import org.wordpress.android.ui.compose.components.text.Message
import org.wordpress.android.ui.compose.components.text.Subtitle
import org.wordpress.android.ui.compose.components.text.Title
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon
import org.wordpress.android.ui.main.jetpack.migration.compose.dimmed

@Composable
fun ErrorStep(uiState: UiState.Error) = with(uiState) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .weight(1f)
        ) {
            ScreenIcon(
                iconRes = screenIconRes,
                modifier = Modifier.dimmed(isProcessing)
            )
            Title(
                text = uiStringText(type.title),
                modifier = Modifier.dimmed(isProcessing)
            )
            Subtitle(
                text = uiStringText(type.subtitle),
                modifier = Modifier.dimmed(isProcessing)
            )
            Message(
                text = uiStringText(type.message),
                modifier = Modifier.dimmed(isProcessing)
            )
        }
        ButtonsColumn {
            PrimaryButton(
                text = uiStringText(primaryActionButton.text),
                onClick = primaryActionButton.onClick,
                isInProgress = isProcessing,
            )
            SecondaryButton(
                text = uiStringText(secondaryActionButton.text),
                onClick = secondaryActionButton.onClick,
                enabled = !isProcessing,
                modifier = Modifier.dimmed(isProcessing)
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewErrorStep() {
    AppTheme {
        val uiState = UiState.Error(
            primaryActionButton = ErrorPrimaryButton {},
            secondaryActionButton = ErrorSecondaryButton {},
            type = UiState.Error.Generic,
        )
        ErrorStep(uiState)
    }
}
