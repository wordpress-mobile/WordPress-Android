package org.wordpress.android.ui.accounts.login.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun LoopingTextWithBackground(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
) {
    Box(modifier.background(colorResource(R.color.bg_jetpack_login_splash))) {
        Image(
                painter = painterResource(R.drawable.bg_jetpack_login_splash),
                contentDescription = stringResource(R.string.login_prologue_revamped_content_description_bg),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
                alpha = 0.8f
        )
        LoopingText(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .alpha(0.8f)
                        .then(textModifier)
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoopingTextWithBackground() {
    AppTheme {
        LoopingTextWithBackground()
    }
}
