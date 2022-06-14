package org.wordpress.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.utils.UiString

sealed class MainActionListItem {
    abstract val actionType: ActionType

    enum class ActionType {
        NO_ACTION,
        CREATE_NEW_PAGE,
        CREATE_NEW_POST,
        CREATE_NEW_STORY,
        ANSWER_BLOGGING_PROMPT
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
        val promptId: Int,
        val attribution: BloggingPromptAttribution,
        val onClickAction: ((promptId: Int) -> Unit)?,
        val onHelpAction: (() -> Unit)?
    ) : MainActionListItem()
}
