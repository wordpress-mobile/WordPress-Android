package org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.component

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.isRtl

@Composable
fun JPInstallFullPluginAnimation(
    modifier: Modifier = Modifier,
) {
    val animationRawRes =
        if (LocalContext.current.isRtl()) {
            R.raw.jp_install_full_plugin_rtl
        } else {
            R.raw.jp_install_full_plugin_left
        }
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRawRes))
    LottieAnimation(
        modifier = modifier,
        composition = lottieComposition
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewPluginDescriptionMultiplePlugins() {
    AppTheme {
        JPInstallFullPluginAnimation()
    }
}
