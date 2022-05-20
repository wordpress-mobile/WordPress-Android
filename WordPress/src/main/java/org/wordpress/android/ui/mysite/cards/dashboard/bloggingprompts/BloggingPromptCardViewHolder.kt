package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteBloggingPrompCardBinding
import org.wordpress.android.ui.avatars.AVATAR_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.avatars.AvatarItemDecorator
import org.wordpress.android.ui.avatars.TrainOfAvatarsAdapter
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager

class BloggingPromptCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker
) : CardViewHolder<MySiteBloggingPrompCardBinding>(
        parent.viewBinding(MySiteBloggingPrompCardBinding::inflate)
) {
    fun bind(card: BloggingPromptCardWithData) = with(binding) {
        uiHelpers.setTextOrHide(promptContent, card.prompt)

        uiHelpers.updateVisibility(answerButton, !card.isAnswered)

        bloggingPromptCardMenu.setOnClickListener {
            bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuClicked()
            showCardMenu(this.root)
        }

        answerButton.setOnClickListener {
            card.onAnswerClick.invoke(card.promptId)
            uiHelpers.updateVisibility(answerButton, false)
            uiHelpers.updateVisibility(answeredPromptControls, true)
        }
        answeredButton.setOnClickListener {
            uiHelpers.updateVisibility(answerButton, true)
            uiHelpers.updateVisibility(answeredPromptControls, false)
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
            uiHelpers.updateVisibility(answeredUsersRecycler, true)
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
        } else {
            uiHelpers.updateVisibility(answeredUsersRecycler, false)
        }
    }

    private fun showCardMenu(bloggingPromptCardView: MaterialCardView) {
        val quickStartPopupMenu = PopupMenu(bloggingPromptCardView.context, bloggingPromptCardView)
        quickStartPopupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.view_more -> bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuViewMorePromptsClicked()
                R.id.skip -> bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuSkipThisPromptClicked()
                R.id.remove -> bloggingPromptsCardAnalyticsTracker.trackMySiteCardMenuRemoveFromDashboardClicked()
            }
            return@setOnMenuItemClickListener true
        }
        quickStartPopupMenu.inflate(R.menu.blogging_prompt_card_menu)
        MenuCompat.setGroupDividerEnabled(quickStartPopupMenu.menu, true)
        quickStartPopupMenu.show()
    }
}
