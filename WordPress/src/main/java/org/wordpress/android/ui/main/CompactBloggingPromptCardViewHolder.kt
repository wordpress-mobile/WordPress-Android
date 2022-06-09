package org.wordpress.android.ui.main

import android.view.ViewGroup
import org.wordpress.android.databinding.BloggingPromptCardCompactBinding
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.extensions.viewBinding

class CompactBloggingPromptCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val htmlCompatWrapper: HtmlCompatWrapper
) : AddContentViewHolder<BloggingPromptCardCompactBinding>(
        parent.viewBinding(BloggingPromptCardCompactBinding::inflate)
) {
    fun bind(action: AnswerBloggingPromptAction) = with(binding) {
        val cardPrompt = htmlCompatWrapper.fromHtml(
                uiHelpers.getTextOfUiString(promptContent.context, action.promptTitle).toString()
        )
        uiHelpers.setTextOrHide(promptContent, cardPrompt)

        uiHelpers.updateVisibility(answerButton, !action.isAnswered)

        answerButton.setOnClickListener {
            action.onClickAction?.invoke(action.promptId)
        }
        answeredButton.setOnClickListener {
            action.onClickAction?.invoke(action.promptId)
        }
        promptHelpButton.setOnClickListener {
            action.onHelpAction?.invoke()
        }
        uiHelpers.updateVisibility(answeredButton, action.isAnswered)
    }
}
