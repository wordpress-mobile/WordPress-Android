package org.wordpress.android.ui.jpfullplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.jpfullplugininstall.install.UiState

@Composable
fun DoneState(
    uiState: UiState.Done,
    onDoneClick: () -> Unit,
) = Box(
    Modifier
        .fillMaxWidth()
        .fillMaxHeight()
) {
    with(uiState) {
        BaseState(uiState) {
            PrimaryButton(
                text = stringResource(buttonText),
                onClick = onDoneClick,
                modifier = Modifier.padding(top = Margin.Large.value),
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
