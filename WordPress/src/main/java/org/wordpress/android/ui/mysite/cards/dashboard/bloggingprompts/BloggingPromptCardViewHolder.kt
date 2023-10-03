package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import androidx.core.view.isVisible
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteBloggingPromptCardBinding
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution.DAY_ONE
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.extensions.getColorStateListFromAttributeOrRes
import org.wordpress.android.util.extensions.viewBinding

class BloggingPromptCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker,
    private val htmlCompatWrapper: HtmlCompatWrapper,
    private val learnMoreClicked: () -> Unit
) : MySiteCardAndItemViewHolder<MySiteBloggingPromptCardBinding>(
    parent.viewBinding(MySiteBloggingPromptCardBinding::inflate)
) {
    fun bind(card: BloggingPromptCardWithData) = with(binding) {
        val cardPrompt = htmlCompatWrapper.fromHtml(
            uiHelpers.getTextOfUiString(promptContent.context, card.prompt).toString()
        )
        uiHelpers.setTextOrHide(promptContent, cardPrompt)
        uiHelpers.updateVisibility(answerButton, !card.isAnswered)

        uiHelpers.updateVisibility(attributionContainer, card.attribution == DAY_ONE)

        attributionContent.text = htmlCompatWrapper.fromHtml(
            attributionContent.context.getString(R.string.my_site_blogging_prompt_card_attribution_dayone)
        )

        bloggingPromptCardMenu.setOnClickListener {
            bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuClicked()
            showCardMenu(card)
        }

        answerButton.setOnClickListener {
            card.onAnswerClick.invoke(card.promptId)
        }
        shareButton.setOnClickListener {
            bloggingPromptsCardAnalyticsTracker.trackMySiteCardShareClicked()
            card.onShareClick.invoke(
                uiHelpers.getTextOfUiString(
                    shareButton.context,
                    card.prompt
                ).toString()
            )
        }
        uiHelpers.updateVisibility(answeredPromptControls, card.isAnswered)

        setupAnsweredUsersContainer(card)
    }

    @Suppress("NestedBlockDepth")
    private fun setupAnsweredUsersContainer(
        card: BloggingPromptCardWithData,
    ) = with(binding) {
        if (card.numberOfAnswers <= 0) {
            answeredUsersAvatars.isVisible = false
            answeredUsersLabel.isVisible = false
            return@with
        }

        answeredUsersAvatars.apply {
            isVisible = true
            avatars = card.respondents
                .filterIsInstance(TrainOfAvatarsItem.AvatarItem::class.java)
        }

        answeredUsersLabel.apply {
            card.respondents
                .filterIsInstance(TrainOfAvatarsItem.TrailingLabelTextItem::class.java)
                .firstOrNull()
                ?.let {
                    isVisible = true
                    text = uiHelpers.getTextOfUiString(
                        context,
                        it.text
                    )
                    setTextColor(
                        context.getColorStateListFromAttributeOrRes(it.labelColor)
                    )
                    card.onViewAnswersClick?.let { onClick ->
                        setOnClickListener { onClick(card.promptId) }
                    }
                }
                ?: run {
                    isVisible = false
                }
        }
    }

    private fun MySiteBloggingPromptCardBinding.showCardMenu(card: BloggingPromptCardWithData) {
        PopupMenu(bloggingPromptCardMenu.context, bloggingPromptCardMenu).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.view_more -> {
                        bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuViewMorePromptsClicked()
                        card.onViewMoreClick.invoke()
                    }

                    R.id.skip -> {
                        bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuSkipThisPromptClicked()
                        card.onSkipClick.invoke()
                    }

                    R.id.remove -> {
                        bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuRemoveFromDashboardClicked()
                        card.onRemoveClick.invoke()
                    }

                    R.id.learn_more -> {
                        bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuLearnMoreClicked()
                        learnMoreClicked()
                    }
                }
                return@setOnMenuItemClickListener true
            }
            inflate(R.menu.blogging_prompt_card_menu)
            MenuCompat.setGroupDividerEnabled(menu, true)
        }.also {
            it.show()
        }
    }
}
