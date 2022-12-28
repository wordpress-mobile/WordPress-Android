package org.wordpress.android.ui.accounts.login.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun LoopingTextWithBackground(
    modifier: Modifier = Modifier,
    blurModifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(colorResource(R.color.bg_jetpack_login_splash))
            .paint(
                painter = painterResource(R.drawable.bg_jetpack_login_splash),
                sizeToIntrinsics = true,
                contentScale = ContentScale.FillBounds,
            )
            .then(blurModifier)
    ) {
        LoopingText(
            modifier = Modifier
                .clearAndSetSemantics {}
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.login_prologue_revamped_prompts_padding))
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
