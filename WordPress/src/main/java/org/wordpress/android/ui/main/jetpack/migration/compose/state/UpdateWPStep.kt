package org.wordpress.android.ui.main.jetpack.migration.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.UpdateWPPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.UpdateWPSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content.UpdateWP
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ButtonsColumn
import org.wordpress.android.ui.main.jetpack.migration.compose.components.PrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon
import org.wordpress.android.ui.main.jetpack.migration.compose.components.SecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Subtitle
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Title

@Composable
fun UpdateWPStep(uiState: UpdateWP) = with(uiState) {
    Column(
            modifier = Modifier.fillMaxSize()
    ) {
        val scrollState = rememberScrollState()
        Column(
                modifier = Modifier
                        .verticalScroll(scrollState)
                        .weight(1f)
        ) {
            ScreenIcon(iconRes = screenIconRes)
            Title(text = uiStringText(title))
            Subtitle(text = uiStringText(subtitle))
            Spacer(modifier = Modifier.weight(0.5f))
        }
        ButtonsColumn {
            PrimaryButton(
                    text = uiStringText(primaryActionButton.text),
                    onClick = primaryActionButton.onClick,
            )
            SecondaryButton(
                    text = uiStringText(secondaryActionButton.text),
                    onClick = secondaryActionButton.onClick,
                    enabled = true
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewUpdateWPStep() {
    AppTheme {
        val uiState = UpdateWP(UpdateWPPrimaryButton {}, UpdateWPSecondaryButton {})
        UpdateWPStep(uiState)
    }
}

