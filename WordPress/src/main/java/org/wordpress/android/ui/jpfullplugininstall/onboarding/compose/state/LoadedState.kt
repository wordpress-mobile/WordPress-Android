package org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ButtonsColumn
import org.wordpress.android.ui.compose.components.PrimaryButton
import org.wordpress.android.ui.compose.components.SecondaryButton
import org.wordpress.android.ui.compose.components.text.Title
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.component.JPInstallFullPluginAnimation
import org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.component.PluginDescription
import org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.component.TermsAndConditions

@Composable
fun LoadedState(
    content: UiState.Loaded,
    onTermsAndConditionsClick: () -> Unit,
    onInstallFullPluginClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onDismissScreenClick: () -> Unit,
) = Box(
    Modifier
        .fillMaxWidth()
        .fillMaxWidth()
) {
    with(content) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .padding(top = 34.dp),
                ) {
                    JPInstallFullPluginAnimation()
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismissScreenClick) {
                        Icon(
                            modifier = Modifier.align(Alignment.Top),
                            painter = painterResource(R.drawable.ic_close_white_24dp),
                            contentDescription = stringResource(
                                R.string.jetpack_full_plugin_install_onboarding_dismiss_button_content_description
                            ),
                        )
                    }
                }
                Title(text = stringResource(R.string.jetpack_individual_plugin_support_onboarding_title))
                PluginDescription(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .padding(top = 20.dp),
                    siteName = siteName,
                    pluginNames = pluginNames,
                )
                Spacer(Modifier.weight(1f))
                TermsAndConditions(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = Margin.ExtraMediumLarge.value,
                            bottom = Margin.ExtraMediumLarge.value,
                        ),
                    onTermsAndConditionsClick = { onTermsAndConditionsClick() },
                )
            }
            ButtonsColumn {
                PrimaryButton(
                    text = stringResource(R.string.jetpack_full_plugin_install_onboarding_install_button),
                    onClick = { onInstallFullPluginClick() },
                )
                SecondaryButton(
                    text = stringResource(R.string.jetpack_full_plugin_install_onboarding_contact_support_button),
                    onClick = { onContactSupportClick() }
                )
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewLoadedState() {
    AppTheme {
        val uiState = UiState.Loaded(
            siteName = "wordpress.com",
            pluginNames = listOf("Jetpack Search"),
        )
        LoadedState(uiState, {}, {}, {}, {})
    }
}
