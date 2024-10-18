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
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.compose.components.buttons.SecondaryButtonM3
import org.wordpress.android.ui.compose.components.text.MessageM3
import org.wordpress.android.ui.compose.components.text.SubtitleM3
import org.wordpress.android.ui.compose.components.text.TitleM3
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon
import org.wordpress.android.ui.main.jetpack.migration.compose.dimmed

@Composable
fun ErrorStep(uiState: UiState.Error): Unit = with(uiState) {
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
            TitleM3(
                text = uiStringText(type.title),
                modifier = Modifier.dimmed(isProcessing)
            )
            SubtitleM3(
                text = uiStringText(type.subtitle),
                modifier = Modifier.dimmed(isProcessing)
            )
            MessageM3(
                text = uiStringText(type.message),
                modifier = Modifier.dimmed(isProcessing)
            )
        }
        ButtonsColumn {
            PrimaryButtonM3(
                text = uiStringText(primaryActionButton.text),
                onClick = primaryActionButton.onClick,
                isInProgress = isProcessing,
            )
            SecondaryButtonM3(
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
    AppThemeM3 {
        val uiState = UiState.Error(
            primaryActionButton = ErrorPrimaryButton {},
            secondaryActionButton = ErrorSecondaryButton {},
            type = UiState.Error.Generic,
        )
        ErrorStep(uiState)
    }
}
