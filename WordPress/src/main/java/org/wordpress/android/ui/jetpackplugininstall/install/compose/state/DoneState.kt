package org.wordpress.android.ui.jetpackplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.ButtonSize
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.jetpackplugininstall.install.UiState

@Composable
fun DoneState(
    uiState: UiState.Done,
    onDoneClick: () -> Unit,
) {
    with(uiState) {
        BaseState(this) {
            PrimaryButton(
                text = stringResource(buttonText),
                onClick = onDoneClick,
                padding = PaddingValues(0.dp),
                buttonSize = ButtonSize.LARGE,
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewDoneState() {
    AppThemeM2 {
        val uiState = UiState.Done(
            descriptionText = R.string.jetpack_plugin_install_full_plugin_done_description,
            buttonText = R.string.jetpack_plugin_install_full_plugin_done_button,
        )
        DoneState(uiState, {})
    }
}
