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
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.compose.components.text.MessageM3
import org.wordpress.android.ui.compose.components.text.SubtitleM3
import org.wordpress.android.ui.compose.components.text.TitleM3
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.NotificationsPrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon

@Composable
fun NotificationsStep(uiState: UiState.Content.Notifications): Unit = with(uiState) {
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
            TitleM3(text = uiStringText(title))
            SubtitleM3(text = uiStringText(subtitle))
            MessageM3(text = uiStringText(message))
        }
        ButtonsColumn {
            PrimaryButtonM3(
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
    AppThemeM3 {
        val uiState = UiState.Content.Notifications(NotificationsPrimaryButton {})
        NotificationsStep(uiState)
    }
}
