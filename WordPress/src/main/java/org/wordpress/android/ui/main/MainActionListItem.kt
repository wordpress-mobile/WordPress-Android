package org.wordpress.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class MainActionListItem {
    abstract val actionType: ActionType

    enum class ActionType {
        NO_ACTION,
        CREATE_NEW_PAGE,
        CREATE_NEW_POST,
        CREATE_NEW_STORY
    }

    data class CreateAction(
        override val actionType: ActionType,
        @DrawableRes val iconRes: Int,
        @StringRes val labelRes: Int,
        val onClickAction: ((actionType: ActionType) -> Unit)?,
        val showQuickStartFocusPoint: Boolean = false
    ) : MainActionListItem()
}
