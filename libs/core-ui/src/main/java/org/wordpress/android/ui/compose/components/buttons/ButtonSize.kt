package org.wordpress.android.ui.compose.components.buttons

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ButtonSize(val height: Dp) {
    NORMAL(height = Dp.Unspecified),
    // this height matches the jetpack_bottom_sheet_button_height
    LARGE(height = 52.dp)
}
