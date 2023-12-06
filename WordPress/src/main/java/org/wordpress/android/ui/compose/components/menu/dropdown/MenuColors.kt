package org.wordpress.android.ui.compose.components.menu.dropdown

import androidx.compose.runtime.Composable
import org.wordpress.android.ui.compose.theme.AppColor

@Composable
fun itemContentColor() = if (androidx.compose.material.MaterialTheme.colors.isLight) {
    AppColor.Black
} else {
    AppColor.White
}

@Composable
fun itemBackgroundColor() = if (androidx.compose.material.MaterialTheme.colors.isLight) {
    AppColor.White
} else {
    AppColor.Black
}
