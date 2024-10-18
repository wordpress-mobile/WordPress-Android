package org.wordpress.android.ui.accounts.login.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun TopLinearGradient(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.bg_jetpack_login_splash_top_gradient),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.82f)
    )
}

@Preview(device = Devices.PIXEL_4_XL)
@Preview(device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTopLinearGradient() {
    AppThemeM3 {
        TopLinearGradient()
    }
}
