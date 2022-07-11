package org.wordpress.android.ui.main

import android.view.ViewGroup
import org.wordpress.android.databinding.BloggingPromptCardCompactBinding
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution.DAY_ONE
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
        uiHelpers.updateVisibility(attributionContainer, action.attribution == DAY_ONE)

        answerButton.setOnClickListener {
            action.onClickAction?.invoke(action.promptId)
        }
        promptHelpButtonContainer.setOnClickListener {
            action.onHelpAction?.invoke()
        }
        uiHelpers.updateVisibility(answeredButton, action.isAnswered)
        uiHelpers.updateVisibility(answerButton, !action.isAnswered)
    }
}
