package org.wordpress.android.ui.compose.unit

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class Margin(val value: Dp) {
    object ExtraSmall : Margin(2.dp)
    object Small : Margin(4.dp)
    object Medium : Margin(8.dp)
    object MediumLarge : Margin(10.dp)
    object Large : Margin(12.dp)
    object ExtraLarge : Margin(16.dp)
    object ExtraMediumLarge : Margin(24.dp)
    object ExtraExtraMediumLarge : Margin(32.dp)
}
