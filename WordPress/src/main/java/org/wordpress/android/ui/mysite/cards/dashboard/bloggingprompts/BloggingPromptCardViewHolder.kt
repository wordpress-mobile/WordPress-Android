package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
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
        uiHelpers.updateVisibility(answerButton, !card.isAnswered)

        bloggingPromptCardMenu.setOnClickListener {
            showCardMenu()
        }

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

    private fun MySiteBloggingPrompCardBinding.showCardMenu() {
        val quickStartPopupMenu = PopupMenu(itemView.context, bloggingPromptCardMenu)
        quickStartPopupMenu.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener true
        }
        quickStartPopupMenu.inflate(R.menu.blogging_prompt_card_menu)
        MenuCompat.setGroupDividerEnabled(quickStartPopupMenu.menu, true)
        quickStartPopupMenu.show()
    }
}
