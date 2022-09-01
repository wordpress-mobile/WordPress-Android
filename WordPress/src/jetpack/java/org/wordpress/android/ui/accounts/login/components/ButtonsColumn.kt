package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.color

@Composable
fun ButtonsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Divider(
            thickness = 1.dp,
            color = colorResource(color.border_shadow_jetpack_login_splash_bottom_panel),
    )
    Divider(
            thickness = 1.dp,
            color = colorResource(color.border_highlight_jetpack_login_splash_bottom_panel),
    )
    Column(
            modifier = modifier
                    .background(
                            brush = SolidColor(colorResource(color.bg_jetpack_login_splash_bottom_panel)),
                            alpha = 0.6f
                    )
    ) {
        content()
    }
}
