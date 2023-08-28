package org.wordpress.android.ui.mysite.cards.dashboard.posts

import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.viewbinding.ViewBinding
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePostCardWithPostItemsBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager

sealed class PostCardViewHolder<T : ViewBinding>(
    override val binding: T
) : CardViewHolder<T>(binding) {
    abstract fun bind(card: PostCard)

    class PostCardWithPostItemsViewHolder(
        parent: ViewGroup,
        imageManager: ImageManager,
        private val uiHelpers: UiHelpers
    ) : PostCardViewHolder<MySitePostCardWithPostItemsBinding>(
        parent.viewBinding(MySitePostCardWithPostItemsBinding::inflate)
    ) {
        init {
            binding.postItems.adapter = PostItemsAdapter(imageManager, uiHelpers)
        }

        override fun bind(card: PostCard) = with(binding) {
            val postCard = card as PostCardWithPostItems
            mySiteToolbar.update(postCard)
            (postItems.adapter as PostItemsAdapter).update(postCard.postItems)
            uiHelpers.setTextOrHide(mySiteCardFooterLink.linkLabel, postCard.footerLink.label)
            mySiteCardFooterLink.linkLabel.setOnClickListener {
                postCard.footerLink.onClick.invoke(card.postCardType)
            }
        }

        private fun MySiteCardToolbarBinding.update(card: PostCardWithPostItems) {
            uiHelpers.setTextOrHide(mySiteCardToolbarTitle, card.title)
            mySiteCardToolbarMore.visibility = View.VISIBLE
            mySiteCardToolbarMore.setOnClickListener {
                showMoreMenu(card)
            }
        }

        private fun MySiteCardToolbarBinding.showMoreMenu(card: PostCardWithPostItems) {
            card.moreMenuOptions.onMoreMenuClick.invoke(card.postCardType)
            val popupMenu = PopupMenu(itemView.context, mySiteCardToolbarMore)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.post_card_menu_item_view_all -> {
                        card.moreMenuOptions.onViewPostsMenuItemClick.invoke(card.postCardType)
                        return@setOnMenuItemClickListener true
                    }

                    R.id.post_card_menu_item_hide_this -> {
                        card.moreMenuOptions.onHideThisMenuItemClick.invoke(card.postCardType)
                        return@setOnMenuItemClickListener true
                    }

                    else -> return@setOnMenuItemClickListener true
                }
            }

            popupMenu.inflate(card.moreMenuResId)
            popupMenu.show()
        }
    }
}
