package org.wordpress.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.ui.utils.UiString

sealed class MainActionListItem {
    abstract val actionType: ActionType

    enum class ActionType {
        NO_ACTION,
        CREATE_NEW_PAGE,
        CREATE_NEW_POST,
        CREATE_NEW_STORY,
        ANSWER_BLOGGING_PROMP
    }

    data class CreateAction(
        override val actionType: ActionType,
        @DrawableRes val iconRes: Int,
        @StringRes val labelRes: Int,
        val onClickAction: ((actionType: ActionType) -> Unit)?,
        val showQuickStartFocusPoint: Boolean = false
    ) : MainActionListItem()

    data class AnswerBloggingPromptAction(
        override val actionType: ActionType,
        val promptTitle: UiString,
        val isAnswered: Boolean,
        val onClickAction: (() -> Unit)?,
    ) : MainActionListItem()
}
