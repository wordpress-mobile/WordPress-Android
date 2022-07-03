package org.wordpress.android.ui.compose.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

sealed class FontSize(val value: TextUnit) {
    object DoubleExtraLarge : FontSize(24.sp)
}
