package org.wordpress.android.ui.main.jetpack.migration.compose.state

import android.content.res.Configuration
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState

@Composable
fun ErrorStep(uiState: UiState.Error) = with(uiState) {
    Text(text = "TODO")
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewGenericErrorStep() {
    AppTheme {
        val uiState = UiState.Error.Generic({}, {})
        ErrorStep(uiState)
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNetworkingErrorStep() {
    AppTheme {
        val uiState = UiState.Error.Networking({}, {})
        ErrorStep(uiState)
    }
}
