package org.wordpress.android.ui.compose.unit

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

sealed class FontSize(val value: TextUnit) {
    object Large : FontSize(16.sp)
    object DoubleExtraLarge : FontSize(24.sp)
    object ExtraExtraExtraLarge : FontSize(34.sp)
}
