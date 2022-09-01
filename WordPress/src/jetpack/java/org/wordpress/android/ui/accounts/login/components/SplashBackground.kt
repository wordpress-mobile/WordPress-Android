package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline.Rectangle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
private fun SplashBox(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(MaterialTheme.colors.background)) {
        Image(
                painter = painterResource(drawable.bg_jetpack_login_splash),
                contentDescription = stringResource(string.login_prologue_revamped_content_description_bg),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
        )
        LargeTexts(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .then(textModifier)
        )
    }
}

@Composable
fun SplashBackground(
    columnContent: @Composable ColumnScope.() -> Unit
) {
    val blurClipShape = remember {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Rectangle {
                return Rectangle(
                        Rect(
                                bottom = size.height,
                                left = 0.0f,
                                right = size.width,
                                top = size.height / 4 * 3,
                        )
                )
            }
        }
    }

    SplashBox()
    SplashBox(
            modifier = Modifier.clip(blurClipShape),
            textModifier = Modifier.blur(15.dp, BlurredEdgeTreatment.Unbounded)
    )
    Image(
            painter = painterResource(drawable.bg_jetpack_login_splash_top_gradient),
            contentDescription = stringResource(string.login_prologue_revamped_content_description_top_bg),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 292.dp)
    )
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            content = columnContent,
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_4, backgroundColor = 0x00000000)
@Composable
fun PreviewSplashBackgroundBox() {
    AppTheme {
        SplashBackground {}
    }
}
