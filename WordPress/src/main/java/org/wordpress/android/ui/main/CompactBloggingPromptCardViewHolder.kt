package org.wordpress.android.ui.main

import android.view.ViewGroup
import org.wordpress.android.databinding.BloggingPrompCardCompactBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class CompactBloggingPromptCardViewHolder(   parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : AddContentViewHolder<BloggingPrompCardCompactBinding>(
        parent.viewBinding(BloggingPrompCardCompactBinding::inflate)
) {
    fun bind(card: BloggingPromptCardWithData) = with(binding) {
        uiHelpers.setTextOrHide(promptContent, card.prompt)

        uiHelpers.updateVisibility(answerButton, !card.isAnswered)


        answerButton.setOnClickListener {
            uiHelpers.updateVisibility(answerButton, false)
            uiHelpers.updateVisibility(answeredPromptControls, true)
        }
        answeredButton.setOnClickListener {
            uiHelpers.updateVisibility(answerButton, true)
            uiHelpers.updateVisibility(answeredPromptControls, false)
        }
        shareButton.setOnClickListener {
            card.onShareClick.invoke(
                    uiHelpers.getTextOfUiString(
                            shareButton.context,
                            card.prompt
                    ).toString()
            )
        }
        uiHelpers.updateVisibility(answeredPromptControls, card.isAnswered)
    }
}
