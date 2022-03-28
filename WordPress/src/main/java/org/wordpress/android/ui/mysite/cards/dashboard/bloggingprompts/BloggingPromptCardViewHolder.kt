package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteBloggingPrompCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class BloggingPromptCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<MySiteBloggingPrompCardBinding>(
        parent.viewBinding(MySiteBloggingPrompCardBinding::inflate)
) {
    fun bind(card: BloggingPromptCardWithData) = with(binding) {
        uiHelpers.setTextOrHide(promptContent, card.prompt)

        val numberOfAnswersLabel = numberOfAnswers.context.getString(
                R.string.my_site_blogging_prompt_card_number_of_answers,
                card.numberOfAnswers
        )
        uiHelpers.setTextOrHide(numberOfAnswers, numberOfAnswersLabel)
    }
}
