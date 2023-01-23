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
import org.wordpress.android.R.drawable
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun TopLinearGradient(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(drawable.bg_jetpack_login_splash_top_gradient),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.62f)
    )
}

@Preview(device = Devices.PIXEL_4_XL)
@Preview(device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTopLinearGradient() {
    AppTheme {
        TopLinearGradient()
    }
}
