package org.wordpress.android.ui.prefs.privacy.banner

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.WPSwitch
import org.wordpress.android.ui.compose.theme.AppThemeM2

@Composable
fun PrivacyBannerScreen(viewModel: PrivacyBannerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    PrivacyBannerScreen(
        uiState,
        viewModel::onAnalyticsEnabledChanged,
        viewModel::onSavePressed,
        viewModel::onSettingsPressed,
    )
}

@Composable
fun PrivacyBannerScreen(
    state: PrivacyBannerViewModel.UiState,
    onSwitchChanged: (Boolean) -> Unit,
    onSavePressed: () -> Unit,
    onSettingsPressed: () -> Unit,
) {
    Box(Modifier.background(MaterialTheme.colors.surface)) {
        Column(
            Modifier
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.privacy_banner_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.h6
            )

            Text(
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.body2,
                text = stringResource(R.string.privacy_banner_description)
            )

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clickable { onSwitchChanged(!state.analyticsSwitchEnabled) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(R.string.privacy_banner_analytics),
                )
                Spacer(modifier = Modifier.weight(1f))
                WPSwitch(
                    modifier = Modifier.padding(end = 16.dp),
                    checked = state.analyticsSwitchEnabled,
                    onCheckedChange = { onSwitchChanged(it) },
                )
            }

            Text(
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 64.dp),
                style = TextStyle(
                    lineHeight = 20.sp,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(
                        alpha = 0.60f
                    ),
                ),
                text = stringResource(R.string.privacy_banner_analytics_description),
            )

            if (state.showError) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        lineHeight = 20.sp,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.error,
                    ),
                    text = stringResource(R.string.privacy_banner_error_save)
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = contentColorFor(MaterialTheme.colors.surface)
                    ),
                    border = ButtonDefaults.outlinedBorder,
                    shape = MaterialTheme.shapes.small.copy(CornerSize(8.dp)),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    ),
                    onClick = onSettingsPressed
                ) {
                    Text(stringResource(R.string.privacy_banner_settings))
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { onSavePressed() },
                    shape = MaterialTheme.shapes.small.copy(CornerSize(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    ),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier
                                .size(24.dp),
                            strokeWidth = 3.dp
                        )
                    } else if (state.showError) {
                        Text(stringResource(R.string.retry))
                    } else {
                        Text(stringResource(R.string.privacy_banner_save))
                    }
                }
            }
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Smaller screen", device = Devices.NEXUS_5)
@Composable
private fun PreviewPrivacyBanner() {
    AppThemeM2 {
        PrivacyBannerScreen(
            state = PrivacyBannerViewModel.UiState(
                analyticsSwitchEnabled = false,
                loading = true,
                showError = false,
            ),
            {}, {}, {}
        )
    }
}

@Preview(name = "With error")
@Composable
private fun PreviewError() {
    AppThemeM2 {
        PrivacyBannerScreen(
            state = PrivacyBannerViewModel.UiState(
                analyticsSwitchEnabled = false,
                loading = false,
                showError = true,
            ),
            {}, {}, {}
        )
    }
}
