package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun LoopingTextWithBackground(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
) {
    Box(modifier.background(MaterialTheme.colors.background)) {
        Image(
                painter = painterResource(drawable.bg_jetpack_login_splash),
                contentDescription = stringResource(string.login_prologue_revamped_content_description_bg),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
        )
        LoopingText(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .then(textModifier)
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
fun PreviewLoopingTextWithBackground() {
    AppTheme {
        LoopingTextWithBackground()
    }
}
