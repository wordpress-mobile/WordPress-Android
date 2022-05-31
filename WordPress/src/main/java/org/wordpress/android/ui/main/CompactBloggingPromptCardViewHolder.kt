package org.wordpress.android.ui.main

import android.view.ViewGroup
import org.wordpress.android.databinding.BloggingPrompCardCompactBinding
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class CompactBloggingPromptCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : AddContentViewHolder<BloggingPrompCardCompactBinding>(
        parent.viewBinding(BloggingPrompCardCompactBinding::inflate)
) {
    fun bind(action: AnswerBloggingPromptAction) = with(binding) {
        uiHelpers.setTextOrHide(promptContent, action.promptTitle)

        uiHelpers.updateVisibility(answerButton, !action.isAnswered)

        answerButton.setOnClickListener {
            action.onClickAction?.invoke(action.promptId)
            uiHelpers.updateVisibility(answerButton, false)
            uiHelpers.updateVisibility(answeredButton, true)
        }
        answeredButton.setOnClickListener {
            uiHelpers.updateVisibility(answerButton, true)
            uiHelpers.updateVisibility(answeredButton, false)
        }

        uiHelpers.updateVisibility(answeredButton, action.isAnswered)
    }
}
