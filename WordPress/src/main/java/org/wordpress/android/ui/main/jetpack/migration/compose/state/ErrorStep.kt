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
import org.wordpress.android.R.string
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.ErrorSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ButtonsColumn
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Message
import org.wordpress.android.ui.main.jetpack.migration.compose.components.PrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon
import org.wordpress.android.ui.main.jetpack.migration.compose.components.SecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Subtitle
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Title
import org.wordpress.android.ui.main.jetpack.migration.compose.dimmed
import org.wordpress.android.ui.utils.UiString.UiStringRes

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
                    text = uiStringText(title),
                    modifier = Modifier.dimmed(isProcessing)
            )
            Subtitle(
                    text = uiStringText(subtitle),
                    modifier = Modifier.dimmed(isProcessing)
            )
            Message(
                    text = uiStringText(message),
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
                title = UiStringRes(string.jp_migration_generic_error_title),
                subtitle = UiStringRes(string.jp_migration_generic_error_subtitle),
                message = UiStringRes(string.jp_migration_generic_error_message),
        )
        ErrorStep(uiState)
    }
}
