package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteBloggingPromptCardBinding
import org.wordpress.android.ui.avatars.AVATAR_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.avatars.AvatarItemDecorator
import org.wordpress.android.ui.avatars.TrainOfAvatarsAdapter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution.DAY_ONE
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager

class BloggingPromptCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker,
    private val htmlCompatWrapper: HtmlCompatWrapper,
    private val learnMoreClicked: () -> Unit
) : CardViewHolder<MySiteBloggingPromptCardBinding>(
    parent.viewBinding(MySiteBloggingPromptCardBinding::inflate)
) {
    @SuppressLint("ClickableViewAccessibility")
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

        val layoutManager = FlexboxLayoutManager(
            answeredUsersRecycler.context,
            FlexDirection.ROW,
            FlexWrap.NOWRAP
        ).apply { justifyContent = JustifyContent.CENTER }

        if (card.numberOfAnswers > 0) {
            uiHelpers.updateVisibility(answeredUsersContainer, true)
            answeredUsersRecycler.addItemDecoration(
                AvatarItemDecorator(
                    RtlUtils.isRtl(answeredUsersRecycler.context),
                    answeredUsersRecycler.context,
                    AVATAR_LEFT_OFFSET_DIMEN
                )
            )
            answeredUsersRecycler.layoutManager = layoutManager

            val adapter = TrainOfAvatarsAdapter(
                imageManager,
                uiHelpers
            )
            answeredUsersRecycler.adapter = adapter

            adapter.loadData(card.respondents)

            // intercept touches in recycler view and pass it to its parent instead of using it for scrolling
            answeredUsersRecycler.setOnTouchListener { v, event -> (v.parent as View).onTouchEvent(event) }
            answeredUsersContainer.setOnClickListener { card.onViewAnswersClick(card.promptId) }
        } else {
            uiHelpers.updateVisibility(answeredUsersContainer, false)
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
                    R.id.remove -> bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuRemoveFromDashboardClicked()
                    R.id.learn_more -> {
                        bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuLearnMoreClicked()
                        learnMoreClicked()
                    }
                }
                return@setOnMenuItemClickListener true
            }
            inflate(R.menu.blogging_prompt_card_menu)
            menu.findItem(R.id.view_more)?.isVisible = card.showViewMoreAction
            MenuCompat.setGroupDividerEnabled(menu, true)
        }.also {
            it.show()
        }
    }
}
