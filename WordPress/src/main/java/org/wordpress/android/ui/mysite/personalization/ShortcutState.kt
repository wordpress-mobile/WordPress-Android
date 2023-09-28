package org.wordpress.android.ui.mysite.personalization

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.utils.UiString

data class ShortcutsState(
    val activeShortCuts: List<ShortcutState>,
    val inactiveShortCuts: List<ShortcutState>,
)

data class ShortcutState(
    @DrawableRes val icon: Int,
    val label: UiString.UiStringRes,
    val isActive: Boolean = false,
    val disableTint : Boolean = false,
    val listItemAction: ListItemAction
)
