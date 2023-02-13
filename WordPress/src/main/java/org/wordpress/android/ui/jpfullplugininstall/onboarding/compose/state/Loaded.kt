package org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
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
fun Loaded(
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
                val animationRawRes =
                    if (LocalContext.current.isRtl()) {
                        R.raw.jp_install_full_plugin_rtl
                    } else {
                        R.raw.jp_install_full_plugin_left
                    }
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRawRes))
                LottieAnimation(
                    composition,
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .padding(top = 34.dp)
                )
                Title(text = stringResource(R.string.jetpack_individual_plugin_support_onboarding_title))
                PluginDescription(
                    siteName = siteName,
                    pluginNames = pluginNames
                )
            }
            TermsAndConditions(
                modifier = Modifier.padding(
                    top = Margin.ExtraMediumLarge.value,
                    bottom = Margin.ExtraMediumLarge.value
                ),
                onTermsAndConditionsClick = { onTermsAndConditionsClick() },
            )
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
private fun PreviewJetpackFullPluginInstallOnboardingScreen() {
    AppTheme {
        val uiState = UiState.Loaded(
            siteName = "wordpress.com",
            pluginNames = listOf("Jetpack Search"),
        )
        Loaded(uiState, {}, {}, {}, {})
    }
}
