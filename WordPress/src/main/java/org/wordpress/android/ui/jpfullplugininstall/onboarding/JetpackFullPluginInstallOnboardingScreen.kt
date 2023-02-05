package org.wordpress.android.ui.jpfullplugininstall.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ButtonsColumn
import org.wordpress.android.ui.compose.components.Message
import org.wordpress.android.ui.compose.components.PrimaryButton
import org.wordpress.android.ui.compose.components.SecondaryButton
import org.wordpress.android.ui.compose.components.Subtitle
import org.wordpress.android.ui.compose.components.Title
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.isRtl

@Composable
fun JetpackFullPluginInstallOnboardingScreen(content: UiState.Content) = Box {
    with(content) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                    composition, modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .padding(top = 34.dp)
                )
                Title(text = uiStringText(title))
                Subtitle(text = uiStringText(subtitle))
            }
            ButtonsColumn {
                Message(text = uiStringText(message))
                PrimaryButton(
                    text = uiStringText(primaryActionButtonText),
                    onClick = primaryActionButtonClick,
                )
                SecondaryButton(
                    text = uiStringText(secondaryActionButtonText),
                    onClick = secondaryActionButtonClick
                )
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewPreviewJetpackStandalonePluginOnboardingScreen() {
    AppTheme {
        val uiState = UiState.Content(
            title = UiString.UiStringRes(R.string.jetpack_individual_plugin_support_onboarding_title),
            subtitle = UiString.UiStringRes(R.string.jetpack_full_plugin_install_onboarding_description),
            message = UiString.UiStringRes(R.string.jetpack_full_plugin_install_onboarding_terms_and_conditions_text),
            primaryActionButtonClick = {},
            primaryActionButtonText = UiString.UiStringRes(R.string.jetpack_full_plugin_install_onboarding_install_button),
            secondaryActionButtonClick = {},
            secondaryActionButtonText = UiString.UiStringRes(R.string.jetpack_full_plugin_install_onboarding_contact_support_button),
        )
        JetpackFullPluginInstallOnboardingScreen(uiState)
    }
}
