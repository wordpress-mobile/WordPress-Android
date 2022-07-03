package org.wordpress.android.ui.compose.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class Margin(val value: Dp) {
    object ExtraLarge : Margin(16.dp)
    object Medium : Margin(8.dp)
    object ExtraExtraMediumLarge : Margin(32.dp)
}
