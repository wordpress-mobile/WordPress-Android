package org.wordpress.android.ui.mysite.personalization

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.utils.UiString

data class ShortcutsState(
    @DrawableRes val icon: Int,
    val label: UiString.UiStringRes,
    val isActive: Boolean = false
)
