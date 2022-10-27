package org.wordpress.android.ui.main.jetpack.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun LogoIcon(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = modifier
                    .width(123.dp)
                    .height(65.dp)
                    .padding(top = 4.dp)
    )
}
