package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteBloggingPrompCardBinding
import org.wordpress.android.ui.avatars.FACE_ITEM_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.avatars.PostLikerItemDecorator
import org.wordpress.android.ui.avatars.ReaderPostLikersAdapter
import org.wordpress.android.ui.avatars.TrainOfFacesItem.BloggersLikingTextItem
import org.wordpress.android.ui.avatars.TrainOfFacesItem.FaceItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager

class BloggingPromptCardViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager
) : CardViewHolder<MySiteBloggingPrompCardBinding>(
        parent.viewBinding(MySiteBloggingPrompCardBinding::inflate)
) {
    fun bind(card: BloggingPromptCardWithData) = with(binding) {
        uiHelpers.setTextOrHide(promptContent, card.prompt)

        val numberOfAnswersLabel = promptContent.context.getString(
                R.string.my_site_blogging_prompt_card_number_of_answers,
                card.numberOfAnswers
        )
//        uiHelpers.setTextOrHide(numberOfAnswers, numberOfAnswersLabel)
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

        val layoutManager = LinearLayoutManager(answeredUsersRecycler.context, LinearLayoutManager.HORIZONTAL, false)
        answeredUsersRecycler.addItemDecoration(
                PostLikerItemDecorator(
                        RtlUtils.isRtl(answeredUsersRecycler.context),
                        answeredUsersRecycler.context,
                        FACE_ITEM_LEFT_OFFSET_DIMEN
                )
        )

        answeredUsersRecycler.layoutManager = layoutManager

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(answeredUsersRecycler)

        val adapter = ReaderPostLikersAdapter(
                imageManager,
                uiHelpers
        )

        adapter.loadData(
                listOf(
                        FaceItem(
                                54279365,
                                "https://0.gravatar.com/avatar/cec64efa352617c35743d8ed233ab410?s=96&d=identicon&r=G"
                        ), BloggersLikingTextItem(UiStringText(numberOfAnswersLabel))
                )
        )

        answeredUsersRecycler.adapter = adapter
    }
}

private fun MySiteBloggingPrompCardBinding.showCardMenu() {
    val quickStartPopupMenu = PopupMenu(bloggingPromptCardMenu.context, bloggingPromptCardMenu)
    quickStartPopupMenu.setOnMenuItemClickListener {
        return@setOnMenuItemClickListener true
    }
    quickStartPopupMenu.inflate(R.menu.blogging_prompt_card_menu)
    MenuCompat.setGroupDividerEnabled(quickStartPopupMenu.menu, true)
    quickStartPopupMenu.show()
}

