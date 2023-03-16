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
import org.wordpress.android.ui.compose.components.ButtonsColumn
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.components.text.Message
import org.wordpress.android.ui.compose.components.text.Subtitle
import org.wordpress.android.ui.compose.components.text.Title
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.NotificationsPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon

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
        ButtonsColumn {
            PrimaryButton(
                text = uiStringText(primaryActionButton.text),
                onClick = primaryActionButton.onClick,
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
