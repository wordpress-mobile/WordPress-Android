package org.wordpress.android.ui.compose.components.menu.dropdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.wordpress.android.ui.compose.theme.AppColor

@Composable
fun isLightTheme(): Boolean = androidx.compose.material.MaterialTheme.colors.isLight

@Composable
fun itemContentColor(): Color = if (isLightTheme()) {
    AppColor.Black
} else {
    AppColor.White
}

@Composable
fun itemBackgroundColor(): Color = if (isLightTheme()) {
    AppColor.White
} else {
    AppColor.DarkGray90
}
