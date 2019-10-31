package org.wordpress.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class MainActionListItem {
    abstract val actionType: ActionType

    enum class ActionType {
        CREATE_NEW_PAGE,
        CREATE_NEW_POST
    }

    data class CreateAction(
        override val actionType: ActionType,
        @DrawableRes val iconRes: Int,
        @StringRes val labelRes: Int,
        val onClickAction: (actionType: ActionType) -> Unit
    ) : MainActionListItem()
}
