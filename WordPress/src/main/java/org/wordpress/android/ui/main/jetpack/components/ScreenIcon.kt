package org.wordpress.android.ui.main.jetpack.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.ScreenIcon

@Composable
fun ScreenIcon(
    model: ScreenIcon,
    modifier: Modifier = Modifier,
) {
    Image(
            painter = painterResource(model.iconRes),
            contentDescription = uiStringText(model.contentDescription),
            modifier = modifier
                    .width(123.dp)
                    .height(65.dp)
                    .padding(top = 4.dp)
    )
}
