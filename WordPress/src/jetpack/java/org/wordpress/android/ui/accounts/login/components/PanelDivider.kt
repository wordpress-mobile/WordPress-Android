package org.wordpress.android.ui.accounts.login.components

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.color

@Composable
fun BorderDivider() {
    Divider(
            thickness = 1.dp,
            color = colorResource(color.border_shadow_jetpack_login_splash_bottom_panel),
    )
    Divider(
            thickness = 1.dp,
            color = colorResource(color.border_highlight_jetpack_login_splash_bottom_panel),
    )
}
