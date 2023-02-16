package org.wordpress.android.ui.jpfullplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.jpfullplugininstall.install.JetpackFullPluginInstallViewModel.UiState

@Composable
fun InstallingState(
    uiState: UiState.Installing,
    onDismissScreenClick: () -> Unit,
) = Box(
    Modifier
        .fillMaxWidth()
        .fillMaxHeight()
) {
    BaseState(
        uiState = uiState,
        onDismissScreenClick = onDismissScreenClick
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(top = Margin.ExtraLarge.value),
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
        InstallingState(uiState) {}
    }
}
