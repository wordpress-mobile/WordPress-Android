package org.wordpress.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.ui.main.MainActionListItem.ItemType.CREATE_ACTION
import org.wordpress.android.ui.main.MainActionListItem.ItemType.TITLE

sealed class MainActionListItem(val itemType: ItemType) {
    enum class ItemType {
        TITLE,
        CREATE_ACTION,
    }

    enum class ActionType {
        CREATE_NEW_PAGE,
        CREATE_NEW_POST
    }

    data class Title(
        @StringRes val labelRes: Int
    ) : MainActionListItem(TITLE)

    data class CreateAction(
        val actionType: ActionType,
        @DrawableRes val iconRes: Int,
        @StringRes val labelRes: Int,
        val onClickAction: (actionType: ActionType) -> Unit
    ) : MainActionListItem(CREATE_ACTION)
}
