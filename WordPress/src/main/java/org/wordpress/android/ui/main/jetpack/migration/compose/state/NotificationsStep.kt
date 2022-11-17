package org.wordpress.android.ui.main.jetpack.migration.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.NotificationsPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Message
import org.wordpress.android.ui.main.jetpack.migration.compose.components.PrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Subtitle
import org.wordpress.android.ui.main.jetpack.migration.compose.components.Title

@Composable
fun NotificationsStep(uiState: UiState.Content.Notifications) = with(uiState) {
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
            Message(text = uiStringText(message))
        }
        Column {
            Divider(
                    color = colorResource(R.color.gray_10),
                    thickness = 0.5.dp,
            )
            PrimaryButton(
                    text = uiStringText(primaryActionButton.text),
                    onClick = primaryActionButton.onClick,
                    modifier = Modifier.padding(bottom = 50.dp),
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewNotificationsStep() {
    AppTheme {
        val uiState = UiState.Content.Notifications(NotificationsPrimaryButton {})
        NotificationsStep(uiState)
    }
}
