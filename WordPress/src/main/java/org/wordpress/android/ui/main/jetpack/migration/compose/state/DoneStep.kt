package org.wordpress.android.ui.main.jetpack.migration.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ButtonsColumn
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.compose.components.text.SubtitleM3
import org.wordpress.android.ui.compose.components.text.TitleM3
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.htmlToAnnotatedString
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.DonePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.components.ScreenIcon

@Composable
fun DoneStep(uiState: UiState.Content.Done): Unit = with(uiState) {
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

            if (showDeleteWPApp) {
                Spacer(modifier = Modifier.weight(0.5f))
                Image(
                    painter = painterResource(deleteWpIcon),
                    contentDescription = stringResource(R.string.jp_migration_remove_wp_app_icon_content_description),
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .size(70.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = htmlToAnnotatedString(uiStringText(message)),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = colorResource(R.color.gray_50),
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                )
                Spacer(modifier = Modifier.weight(0.5f))
            } else {
                SubtitleM3(text = uiStringText(noSitesMessage))
            }
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
private fun PreviewDoneStep() {
    AppThemeM3 {
        val uiState = UiState.Content.Done(DonePrimaryButton {})
        DoneStep(uiState)
    }
}
@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
private fun PreviewDoneStepNoSites() {
    AppThemeM3 {
        val uiState = UiState.Content.Done(
            JetpackMigrationViewModel.ActionButton.DoneNoSitesFlowPrimaryButton {}, false)
        DoneStep(uiState)
    }
}
