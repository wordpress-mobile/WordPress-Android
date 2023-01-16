package org.wordpress.android.ui.mysite.cards.dashboard.posts

import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.MySiteCardToolbarBinding
import org.wordpress.android.databinding.MySitePostCardWithPostItemsBinding
import org.wordpress.android.databinding.MySitePostCardWithoutPostItemsBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager

sealed class PostCardViewHolder<T : ViewBinding>(
    override val binding: T
) : CardViewHolder<T>(binding) {
    abstract fun bind(card: PostCard)

    class PostCardWithoutPostItemsViewHolder(
        parent: ViewGroup,
        private val imageManager: ImageManager,
        private val uiHelpers: UiHelpers
    ) : PostCardViewHolder<MySitePostCardWithoutPostItemsBinding>(
        parent.viewBinding(MySitePostCardWithoutPostItemsBinding::inflate)
    ) {
        override fun bind(card: PostCard) = with(binding) {
            val postCard = card as PostCardWithoutPostItems
            uiHelpers.setTextOrHide(title, postCard.title)
            uiHelpers.setTextOrHide(excerpt, postCard.excerpt)
            imageManager.load(image, postCard.imageRes)
            uiHelpers.setTextOrHide(mySiteCardFooterLink.linkLabel, postCard.footerLink.label)
            mySiteCardFooterLink.linkLabel.setOnClickListener {
                postCard.footerLink.onClick.invoke(card.postCardType)
            }
            itemView.setOnClickListener { postCard.onClick.click() }
        }
    }

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
            mySiteToolbar.update(postCard.title)
            (postItems.adapter as PostItemsAdapter).update(postCard.postItems)
            uiHelpers.setTextOrHide(mySiteCardFooterLink.linkLabel, postCard.footerLink.label)
            mySiteCardFooterLink.linkLabel.setOnClickListener {
                postCard.footerLink.onClick.invoke(card.postCardType)
            }
        }

        private fun MySiteCardToolbarBinding.update(title: UiString?) {
            uiHelpers.setTextOrHide(mySiteCardToolbarTitle, title)
        }
    }
}
