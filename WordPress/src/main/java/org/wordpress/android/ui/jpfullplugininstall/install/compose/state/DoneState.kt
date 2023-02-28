package org.wordpress.android.ui.jpfullplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jpfullplugininstall.install.UiState

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
                useDefaultMargins = false,
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewDoneState() {
    AppTheme {
        val uiState = UiState.Done(
            buttonText = R.string.jetpack_full_plugin_install_done_button,
        )
        DoneState(uiState, {})
    }
}
