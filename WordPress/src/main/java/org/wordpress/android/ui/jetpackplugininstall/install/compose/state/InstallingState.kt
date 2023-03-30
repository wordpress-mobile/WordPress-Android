package org.wordpress.android.ui.jetpackplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.components.buttons.ButtonSize
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jetpackplugininstall.install.UiState

@Composable
fun InstallingState(
    uiState: UiState.Installing,
) {
    BaseState(uiState) {
        PrimaryButton(
            text = "",
            onClick = {},
            isInProgress = true,
            padding = PaddingValues(0.dp),
            buttonSize = ButtonSize.LARGE,
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewInstallingState() {
    AppTheme {
        val uiState = UiState.Installing
        InstallingState(uiState)
    }
}
