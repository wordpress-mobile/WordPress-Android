package org.wordpress.android.ui.reader.views.compose.dropdown

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.utils.UiString

sealed interface JetpackMenuElementData {
    data object Divider : JetpackMenuElementData

    sealed class Item(
        open val id: String,
        open val text: UiString,
        @DrawableRes open val leadingIcon: Int,
    ) : JetpackMenuElementData {
        // Item element that closes the menu when clicked
        data class Single(
            override val id: String,
            override val text: UiString,
            @DrawableRes override val leadingIcon: Int = NO_ICON,
        ) : Item(id, text, leadingIcon)

        // Sub-menu element that opens a sub-menu when clicked
        data class SubMenu(
            override val id: String,
            override val text: UiString,
            val children: List<JetpackMenuElementData>,
            @DrawableRes override val leadingIcon: Int = NO_ICON,
        ) : Item(id, text, leadingIcon)
    }
}

const val NO_ICON = -1
