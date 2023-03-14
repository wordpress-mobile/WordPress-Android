package org.wordpress.android.ui.jpfullplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.ButtonSize
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.components.buttons.SecondaryButton
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jpfullplugininstall.install.UiState

@Composable
fun ErrorState(
    uiState: UiState.Error,
    onRetryClick: () -> Unit,
    onContactSupportClick: () -> Unit,
) {
    with(uiState) {
        BaseState(this) {
            PrimaryButton(
                text = stringResource(retryButtonText),
                onClick = onRetryClick,
                useDefaultMargins = false,
                buttonSize = ButtonSize.LARGE,
            )
            Spacer(modifier = Modifier.height(10.dp))
            SecondaryButton(
                text = stringResource(contactSupportButtonText),
                onClick = onContactSupportClick,
                useDefaultMargins = false,
                buttonSize = ButtonSize.LARGE,
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewErrorState() {
    AppTheme {
        val uiState = UiState.Error(
            retryButtonText = R.string.jetpack_full_plugin_install_error_button_retry,
            contactSupportButtonText = R.string.jetpack_full_plugin_install_error_button_contact_support,
        )
        ErrorState(uiState, {}, {})
    }
}
